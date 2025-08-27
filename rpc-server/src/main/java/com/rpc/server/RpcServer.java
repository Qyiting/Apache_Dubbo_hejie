package com.rpc.server;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.core.metric.MetricsCollector;
import com.rpc.netty.decoder.RpcDecoder;
import com.rpc.netty.encoder.RpcEncoder;
import com.rpc.netty.client.RpcProtocol;
import com.rpc.registry.ServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import com.rpc.server.handler.RpcServerHandler;
import com.rpc.server.provider.providerImpl.DefaultServiceProvider;
import com.rpc.server.provider.ServiceProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
@Data
public class RpcServer {
    /** 服务器地址 */
    private final String host;
    /** 服务器端口 */
    private final int port;
    /** 服务提供者 */
    private final ServiceProvider serviceProvider;
    /** 服务注册中心 */
    private final ServiceRegistry serviceRegistry;
    /** 序列化器 */
    private final Serializer serializer;
    /** Netty服务器引导类 */
    private ServerBootstrap serverBootstrap;
    /** Boss线程组 */
    private EventLoopGroup bossGroup;
    /** Worker线程组 */
    private EventLoopGroup workerGroup;
    /** 服务器通道 */
    private Channel serverChannel;
    /** 服务器是否已启动 */
    private final AtomicBoolean started = new AtomicBoolean(false);
    /** 服务器是否正在关闭 */
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    /** 读超时时间（秒） */
    private int readerIdleTime = 60;
    /** 写超时时间（秒） */
    private int writerIdleTime = 0;
    /** 读写超时时间（秒） */
    private int allIdleTime = 0;
    /** 监控指标收集器 */
    private final MetricsCollector metricsCollector = MetricsCollector.getInstance();
    /**
     * 构造函数
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @param serviceRegistry 服务注册中心
     */
    public RpcServer(String host, int port, ServiceRegistry serviceRegistry) {
        this(host, port, new DefaultServiceProvider(), serviceRegistry, SerializerFactory.getDefaultSerializer());
    }

    /**
     * 构造函数
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @param serviceProvider 服务提供者
     * @param serviceRegistry 服务注册中心
     * @param serializer 序列化器
     */
    public RpcServer(String host, int port, ServiceProvider serviceProvider, ServiceRegistry serviceRegistry, Serializer serializer) {
        this.host = host != null?host:getLocalHost();
        this.port = port;
        this.serviceProvider = serviceProvider!= null?serviceProvider:new DefaultServiceProvider();
        this.serviceRegistry = serviceRegistry;
        this.serializer = serializer != null?serializer:SerializerFactory.getDefaultSerializer();
        log.info("创建RPC服务器：{}:{}，序列化器：{}", this.host, this.port, this.serializer.getName());
    }
    /**
     * 启动服务器
     *
     * @throws RuntimeException 如果启动失败
     */
    public void start() {
        if(!started.compareAndSet(false, true)) {
            log.warn("服务器已经启动，忽略重复启动请求");
            return;
        }
        try {
            log.info("正在启动RPC服务器：{}:{}", host, port);
            // 初始化线程组
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            // 配置服务器
            serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加长度字段处理器，协议头20字节，数据长度字段在偏移16位置，长度4字节
                            pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(RpcProtocol.MAX_FRAME_LENGTH, 16, 4, 0, 0));
                            // 添加编解码器
                            pipeline.addLast("encoder", new RpcEncoder(serializer.getType()));
                            pipeline.addLast("decoder", new RpcDecoder());
                            // 添加空闲状态处理器
                            pipeline.addLast("idleStateHandler", new IdleStateHandler(readerIdleTime, writerIdleTime,
                                    allIdleTime, TimeUnit.SECONDS));
                            // 添加业务处理器
                            pipeline.addLast("handler", new RpcServerHandler(serviceProvider));
                        }
                    });
            // 绑定端口并启动服务器
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            if(future.isSuccess()) {
                serverChannel = future.channel();
                log.info("RPC服务器启动成功：{}:{}", host, port);
                // 注册已有服务到注册中心
                registerServicesToRegistry();
                // 添加关闭钩子
                addShutdownHook();
            } else {
                throw new RuntimeException("服务器启动我失败：" + future.cause().getMessage(), future.cause());
            }
        } catch (Exception e) {
            started.set(false);
            cleanup();
            throw new RuntimeException("启动RPC服务器失败", e);
        }
    }
    /**
     * 停止服务器
     */
    public void stop() {
        if(!started.get() || !shutdownInProgress.compareAndSet(false, true)) {
            log.warn("服务器未启动或正在关闭中");
            return;
        }
        try {
            log.info("正在关闭RPC服务器：{}:{}", host, port);
            // 从注册中心注销服务
            unregisterServicesFromRegistry();
            // 关闭服务器通道
            if(serverChannel != null) {
                serverChannel.close().sync();
            }
            // 清理资源
            cleanup();
            started.set(false);
            log.info("RPC服务器已关闭：{}：{}", host, port);
        } catch (Exception e) {
            log.error("关闭RPC服务器时发送异常", e);
        } finally {
            shutdownInProgress.set(false);
        }
    }

    /**
     * 注册服务
     *
     * @param interfaceClass 服务接口类
     * @param serviceInstance 服务实例
     */
    public void registerService(Class<?> interfaceClass, Object serviceInstance) {
        registerService(interfaceClass, serviceInstance, "1.0.0", "deafult");
    }

    /**
     * 注册服务
     *
     * @param interfaceClass 服务接口类
     * @param serviceInstance 服务实例
     * @param version 服务版本
     * @param group 服务分组
     */
    public void registerService(Class<?> interfaceClass, Object serviceInstance, String version, String group) {
        // 注册到本地服务提供者
        serviceProvider.registerService(interfaceClass, serviceInstance, version, group);
        // 如果服务器已启动，立即注册到注册中心
        if(started.get() && serviceRegistry != null) {
            try {
                ServiceInfo serviceInfo = createServiceInfo(interfaceClass.getName(), version, group);
                serviceRegistry.register(serviceInfo);
                // 记录服务注册指标
                metricsCollector.recordServiceRegistration();
                log.info("服务已注册到注册中心：{}", serviceInfo.getServiceKey());
            } catch (Exception e) {
                log.error("注册服务到注册中心失败：{}", interfaceClass.getName(), e);
            }
        }
    }

    /**
     * 注销服务
     *
     * @param interfaceClass 服务接口类
     */
    public void unregisterService(Class<?> interfaceClass) {
        unregisterService(interfaceClass, "1.0.0", "default");
    }

    /**
     * 注销服务
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     */
    public void unregisterService(Class<?> interfaceClass, String version, String group) {
        String serviceName = interfaceClass.getName();
        // 从本地服务提供者注销
        boolean localSuccess = serviceProvider.unregisterService(serviceName, version, group);
        // 从注册中心注销
        if(serviceRegistry != null) {
            try {
                ServiceInfo serviceInfo = createServiceInfo(serviceName, version, group);
                serviceRegistry.unregister(serviceInfo);
                // 记录服务注销指标
                metricsCollector.recordServiceUnregistration();
                log.info("服务已从注册中心注销：{}", serviceInfo.getServiceKey());
            } catch (Exception e) {
                log.error("从注册中心注销服务失败：{}", serviceName, e);
            }
        }
        if(localSuccess) {
            log.info("服务已注销：{}", serviceName);
        }
    }

    /**
     * 获取服务器状态信息
     *
     * @return 状态信息
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("RPC服务器状态:\n");
        status.append(" 地址: ").append(host).append(":").append(port).append("\n");
        status.append(" 启动状态: ").append(started.get()?"已启动":"未启动").append("\n");
        status.append(" 序列化器: ").append(serializer.getName()).append("\n");
        status.append(" 注册中心: ").append(serviceRegistry!= null?"已连接":"未连接").append("\n");
        if(serviceProvider instanceof DefaultServiceProvider) {
            status.append(" ").append(((DefaultServiceProvider) serviceProvider).getStatistics()).append("\n");
        } else {
            status.append(" 服务数量：").append(serviceProvider.getServiceCount()).append("\n");
        }
        return status.toString();
    }

    /**
     * 获取监控指标摘要
     *
     * @return 监控指标摘要
     */
    public MetricsCollector.MetricsSummary getMetricsSummary() {
        return metricsCollector.getMetricsSummary();
    }

    /**
     * 将已注册的服务注册到注册中心
     */
    private void registerServicesToRegistry() {
        if(serviceRegistry == null) {
            log.warn("未配置服务注册中心，跳过服务注册");
            return;
        }
        List<ServiceInfo> services = serviceProvider.getAllServices();
        for(ServiceInfo serviceInfo : services) {
            try {
                // 更新服务地址信息
                serviceInfo.setAddress(host);
                serviceInfo.setPort(port);
                serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
                serviceInfo.setLastUpdateTime(System.currentTimeMillis());
                serviceRegistry.register(serviceInfo);
                // 记录服务注册指标
                metricsCollector.recordServiceRegistration();
                log.info("服务已注册到注册中心：{}", serviceInfo.getServiceKey());
            } catch (Exception e) {
                log.error("注册服务到注册中心失败：{}", serviceInfo.getServiceKey());
            }
        }
    }

    /**
     * 从注册中心注销所有服务
     */
    private void unregisterServicesFromRegistry() {
        if(serviceRegistry == null) {
            return;
        }
        List<ServiceInfo> services = serviceProvider.getAllServices();
        for(ServiceInfo serviceInfo: services) {
            try {
                serviceRegistry.unregister(serviceInfo);
                // 记录服务注销指标
                metricsCollector.recordServiceUnregistration();
                log.info("服务已从注册中心注销：{}", serviceInfo.getServiceKey());
            } catch (Exception e) {
                log.error("从注册中心注销服务失败：{}", serviceInfo.getServiceKey());
            }
        }
    }

    /**
     * 创建服务信息
     *
     * @param serviceName 服务名称
     * @param version 版本
     * @param group 分组
     * @return 服务信息
     */
    private ServiceInfo createServiceInfo(String serviceName, String version, String group) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setVersion(version);
        serviceInfo.setGroup(group);
        serviceInfo.setAddress(host);
        serviceInfo.setPort(port);
        serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
        serviceInfo.setSerializerType(serializer.getType()); // 设置序列化类型
        serviceInfo.setRegisterTime(System.currentTimeMillis());
        serviceInfo.setLastUpdateTime(System.currentTimeMillis());
        return serviceInfo;
    }

    public boolean isRunning() {
        return started.get() && !shutdownInProgress.get();
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        if(workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if(bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if(serviceProvider != null) {
            serviceProvider.destroy();
        }
        if(serviceRegistry != null) {
            try {
                serviceRegistry.destroy();
            } catch (Exception e) {
                log.error("关闭服务注册中心时发送异常", e);
            }
        }
    }

    /**
     * 添加关闭钩子
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("检测到JVM关闭信号，正在关闭RPC服务器");
            stop();
        }, "rpc-server-shutdown-hook"));
    }


    /**
     * 获取本地主机地址
     *
     * @return 本地主机地址
     */
    private String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("获取本地主机地址失败，使用默认地址：localhost", e);
            return "localhost";
        }
    }
}
