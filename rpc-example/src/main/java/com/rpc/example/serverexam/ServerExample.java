package com.rpc.example.serverexam;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.example.service.AsyncUserService;
import com.rpc.example.service.AsyncUserServiceImpl;
import com.rpc.example.service.UserService;
import com.rpc.example.service.UserServiceImpl;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import com.rpc.server.RpcServer;
import com.rpc.server.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ServerExample {
    public static void main(String[] args) {
        // 服务器配置
        String serverHost = "localhost";
        int serverPort = 8080;
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if(args.length >= 1) {
            serverPort = Integer.parseInt(args[0]);
        }
        if(args.length >= 2) {
            serverHost = args[1];
        }
        if(args.length >= 3) {
            registryAddress = args[2];
        }
        log.info("启动RPC服务端实例");
        log.info("服务器地址：{}:{}", serverHost, serverPort);
        log.info("注册中心地址：{}", registryAddress);
        try {
            // 1. 初始化序列化信息
            SerializerFactory.setDefaultSerializerType(Serializer.SerializerType.PROTOSTUFF);
            log.info("序列化器初始化完成：{}", SerializerFactory.getDefaultSerializer().getName());
            // 2. 创建服务注册中心
            ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry(registryAddress);
            log.info("服务注册中心创建完成");
            // 3. 创建RPC服务器
            RpcServer rpcServer = new RpcServer(serverHost, serverPort, serviceRegistry);
            log.info("RPC服务器创建完成");
            // 4. 创建并注册服务实例
            registerService(rpcServer);
            // 5. 启动服务器
            log.info("正在启动RPC服务器...");
            rpcServer.start();
            log.info("RPC服务器启动完成，监听端口号：{}", serverPort);
            log.info("服务器状态：{}", rpcServer.isRunning()?"运行中":"已停止");
            // 6. 添加关闭钩子
            addShutdownHook(rpcServer, serviceRegistry);
            // 7. 保持服务器运行
            keepServerRunning(rpcServer);
        } catch (Exception e) {
            log.error("启动RPC服务器时发生异常", e);
            System.exit(1);
        }
    }

    /**
     * 注册服务
     *
     * @param rpcServer RPC服务器
     */
    private static void registerService(RpcServer rpcServer) {
        try {
            // 创建用户服务实例
            UserService userService = new UserServiceImpl();
//
//            // 注册用户服务 - 默认服务和分组
//            rpcServer.registerService(UserService.class, userService, "1.0.0", "default");
//            log.info("用户服务注册成功");
//
//            // 注册用户服务 - 版本2.0.0
//            rpcServer.registerService(UserService.class, userService, "2.0.0", "default");
//            log.info("用户服务V2注册成功");
//
//            // 注册用户服务 - 测试分组
//            rpcServer.registerService(UserService.class, userService, "1.0.0", "test");
//            log.info("用户服务测试分组注册成功");
            
            // 创建并注册异步用户服务
            AsyncUserService asyncUserService = new AsyncUserServiceImpl(userService);
            rpcServer.registerService(AsyncUserService.class, asyncUserService, "1.0.0", "default");
            log.info("异步用户服务注册成功");

            // 注册异步用户服务 - 版本2.0.0
            rpcServer.registerService(AsyncUserService.class, asyncUserService, "2.0.0", "default");
            log.info("异步用户服务V2注册成功");

            // 注册异步用户服务 - 测试分组
            rpcServer.registerService(AsyncUserService.class, asyncUserService, "1.0.0", "test");
            log.info("异步用户服务测试分组注册成功");
            
            //打印服务注册统计
            ServiceProvider serviceProvider = rpcServer.getServiceProvider();
            log.info("服务注册统计：总服务数={}，服务列表={}", serviceProvider.getServiceCount(),
                    serviceProvider.getAllServices().stream().map(ServiceInfo::getServiceKey).toArray());
        } catch (Exception e) {
            log.error("注册服务时发生异常", e);
            throw new RuntimeException("服务注册失败", e);
        }
    }

    /**
     * 添加关闭钩子
     *
     * @param rpcServer RPC服务器
     * @param serviceRegistry 服务注册中心
     */
    private static void addShutdownHook(RpcServer rpcServer, ServiceRegistry serviceRegistry) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("接收到关闭信号，正在优雅关闭服务器...");
            try {
                // 停止RPC服务器
                if(rpcServer != null && rpcServer.isRunning()) {
                    rpcServer.stop();
                    log.info("RPC服务器已停止");
                }
                // 关闭服务注册中心
                if(serviceRegistry != null) {
                    serviceRegistry.destroy();
                    log.info("服务注册中心已关闭");
                }
                // 清理序列化器资源
                SerializerFactory.cleanup();
                log.info("序列化器资源已清理");
                log.info("服务器优雅关闭完成");
            } catch (Exception e) {
                log.error("关闭服务器时发生异常", e);
            }
        }, "shutdown-hook"));
        log.info("关闭钩子已添加");
    }

    /**
     * 保持服务器运行
     *
     * @param rpcServer RPC服务器
     */
    private static void keepServerRunning(RpcServer rpcServer) {
        // 定期检查服务器状态
        Thread statusChecker = new Thread(() -> {
            while(rpcServer.isRunning()) {
                try {
                    Thread.sleep(30000);// 30s检查一次
                    if(rpcServer.isRunning()) {
                        String status = rpcServer.getStatus();
                        log.info("服务器状态检查 - 运行中，服务状态：{}，端口：{}",
                                status, rpcServer.getPort());
                    }
                } catch (InterruptedException e) {
                    log.info("状态检查线程被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("状态检查时发生异常", e);
                }
            }
        }, "status-checker");
        statusChecker.setDaemon(true);
        statusChecker.start();
        // 主线程等待
        try {
            log.info("服务器正在运行，按ctrl+c 停止服务器");
            while(rpcServer.isRunning()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("保持服务器运行时发生异常", e);
        }
        log.info("服务器已停止运行");
    }
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("RPC服务端示例使用说明:");
        System.out.println("java -jar rpc-example.jar [port] [host] [registry_address]");
        System.out.println("参数说明:");
        System.out.println("  port            - 服务端口 (默认: 8080)");
        System.out.println("  host            - 服务主机 (默认: localhost)");
        System.out.println("  registry_address - 注册中心地址 (默认: localhost:2181)");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar rpc-example.jar 8080");
        System.out.println("  java -jar rpc-example.jar 8080 192.168.1.100");
        System.out.println("  java -jar rpc-example.jar 8080 192.168.1.100 192.168.1.200:2181");
    }
}
