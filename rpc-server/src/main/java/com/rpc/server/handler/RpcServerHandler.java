package com.rpc.server.handler;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.core.metric.MetricsCollector;
import com.rpc.serialization.json.JsonSerializer;
import com.rpc.serialization.Serializer;
import com.rpc.server.provider.ServiceProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    /** 服务提供者 */
    private final ServiceProvider serviceProvider;
    /** 业务线程池 */
    private final ExecutorService businessExecutor;
    /** 处理的请求计数器 */
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    /** 监控指标收集器 */
    private final MetricsCollector metricsCollector = MetricsCollector.getInstance();
    /**
     * 构造函数
     *
     * @param serviceProvider 服务提供者
     */
    public RpcServerHandler(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
        // 创建业务线程池
        this.businessExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "rpc-server-business-" + threadNumber.getAndIncrement());
                        thread.setDaemon(false);
                        return thread;
                    }
                }
        );
        log.info("RPC服务器处理器已初始化，业务线程池大小：{}", Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        // 增加请求计数
        int requestId = requestCounter.incrementAndGet();
        log.debug("接收到RPC请求 [{}]: {}", requestId, request.getRequestId());
        
        // 记录请求开始
        String serviceName = request.getInterfaceName();
        metricsCollector.recordRequestStart(serviceName);
        long startTime = System.currentTimeMillis();
        
        // 异步处理请求
        CompletableFuture.supplyAsync(() ->
                        handleRequest(ctx, request), businessExecutor)
                .whenComplete((response, throwable) -> {
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    if (throwable != null) {
                        log.error("处理RPC请求异常 [{}]: {}", requestId, request.getRequestId(), throwable);
                        response = RpcResponse.fail(request.getRequestId(), throwable);
                        // 记录请求失败
                        metricsCollector.recordRequestFailure(serviceName, throwable);
                    } else if (response.getStatusCode() == RpcResponse.StatusCode.SUCCESS) {
                        // 记录请求成功
                        metricsCollector.recordRequestSuccess(serviceName, responseTime);
                    } else {
                        // 记录请求失败
                        Throwable exception = response.getException() != null ? response.getException() : 
                            new RuntimeException("RPC调用失败：" + response.getMessage());
                        metricsCollector.recordRequestFailure(serviceName, exception);
                    }
                    
                    // 发送响应
                    ctx.writeAndFlush(response).addListener(future -> {
                        if (future.isSuccess()) {
                            log.debug("RPC响应发送成功 [{}]: {}", requestId, request.getRequestId());
                        } else {
                            log.error("RPC响应发送失败 [{}]: {}", requestId, request.getRequestId(),
                                    future.cause());
                        }
                    });
                });
    }

    /**
     * 处理RPC请求
     *
     * @param request RPC请求
     * @return RPC响应
     */
    private RpcResponse handleRequest(ChannelHandlerContext ctx, RpcRequest request) {
        try {
            // 验证请求参数
            if(request == null) {
                return RpcResponse.fail(null, new IllegalArgumentException("请求不能为null"));
            }
            if(request.getInterfaceName() == null || request.getInterfaceName().trim().isEmpty()) {
                return RpcResponse.fail(request.getRequestId(),
                        new IllegalArgumentException("接口名称不能为空"));
            }
            if(request.getMethodName() == null || request.getMethodName().trim().isEmpty()) {
                return RpcResponse.fail(request.getRequestId(),
                        new IllegalArgumentException("方法名称不能为空"));
            }
            // 获取服务实例
            Object serviceInstance = serviceProvider.getService(request.getInterfaceName(), request.getVersion(),
                    request.getGroup());
            if(serviceInstance == null) {
                String serviceKey = request.getServiceKey();
                log.warn("未找到服务实例：{}", serviceKey);
                return RpcResponse.notFound(request.getRequestId(),
                        "未找到服务：" + serviceKey);
            }
            // 获取方法
            Class<?> serviceClass = serviceInstance.getClass();
            Method method = findMethod(serviceClass, request.getMethodName(), request.getParameterTypes());
            if(method == null) {
                log.warn("未找到方法：{}.{}({})", request.getInterfaceName(),
                        request.getMethodName(), Arrays.toString(request.getParameterTypes()));
                return RpcResponse.notFound(request.getRequestId(),
                        "未找到方法：" + request.getMethodName());
            }
            // 设置方法可访问
            method.setAccessible(true);
            // 执行方法（带 JSON 参数类型适配）
            Object[] params = request.getParameters();
            Class<?>[] paramTypes = method.getParameterTypes();
            // 从 Channel 属性里获取序列化方式
            Byte serializationType = (Byte) ctx.channel()
                    .attr(AttributeKey.valueOf("serializationType"))
                    .get();
            // 如果是 JSON 序列化，则进行参数类型适配
            if (serializationType != null
                    && serializationType == Serializer.SerializerType.JSON
                    && params != null && paramTypes != null) {
                for (int i = 0; i < params.length; i++) {
                    Object arg = params[i];
                    Class<?> expectedType = paramTypes[i];
                    if (arg != null && !expectedType.isAssignableFrom(arg.getClass())) {
                        try {
                            params[i] = JsonSerializer.OBJECT_MAPPER.convertValue(arg, expectedType);
                            log.debug("参数[{}]已从 {} 转换为 {}",
                                    i, arg.getClass().getName(), expectedType.getName());
                        } catch (Exception e) {
                            log.warn("参数[{}]类型转换失败：{} -> {}，使用原始值",
                                    i, arg.getClass().getName(), expectedType.getName(), e);
                        }
                    }
                }
            }

            long startTime = System.currentTimeMillis();
            Object result = method.invoke(serviceInstance, params);  // 使用转换后的参数
            long endTime = System.currentTimeMillis();
            log.debug("方法执行完成：{}.{}，耗时：{}ms", request.getInterfaceName(),
                    request.getMethodName(), endTime - startTime);
            // 返回成功响应
            return RpcResponse.success(request.getRequestId(), result);
        } catch (Exception e) {
            log.error("处理RPC请求失败：{}.{}", request.getInterfaceName(),
                    request.getMethodName(), e);
            // 处理不同类型的异常
            if(e instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) e).getTargetException();
                return RpcResponse.fail(request.getRequestId(), targetException);
            } else if(e instanceof IllegalAccessException) {
                return RpcResponse.fail(request.getRequestId(),
                        new RuntimeException("方法访问权限不足：" + request.getMethodName(), e));
            } else if(e instanceof IllegalArgumentException) {
                return RpcResponse.fail(request.getRequestId(), new RuntimeException(
                        "方法参数不匹配：" + request.getMethodName(), e
                ));
            } else {
                return RpcResponse.fail(request.getRequestId(), e);
            }
        }
    }

    /**
     * 查找方法
     *
     * @param serviceClass 服务类
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @return 方法对象，如果未找到返回null
     */
    private Method findMethod(Class<?> serviceClass, String methodName, Class<?>[] parameterTypes) {
        try {
            // 首先尝试精确匹配
            if(parameterTypes != null) {
                return serviceClass.getMethod(methodName, parameterTypes);
            }
            // 如果参数类型为null，查找同名方法
            Method[] methods = serviceClass.getMethods();
            for(Method method: methods) {
                if(method.getName().equals(methodName)) {
                    return method;
                }
            }
            return null;
        } catch (NoSuchMethodException e) {
            log.debug("精确匹配方法失败，尝试模糊匹配：{}.{}", serviceClass.getName(), methodName);
            // 尝试模糊匹配
            Method[] methods = serviceClass.getMethods();
            for(Method method: methods) {
                if(method.getName().equals(methodName)) {
                    if(parameterTypes == null || method.getParameterCount() == parameterTypes.length) {
                        log.debug("找到匹配方法：{}", method);
                        return method;
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接建立：{}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("客户端连接断开：{}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if(event.state() == IdleState.READER_IDLE) {
                log.warn("客户端读超时，关闭连接：{}", ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理客户端请求时发送异常：{}", ctx.channel().remoteAddress(), cause);
        // 尝试发送错误响应
        try {
            RpcResponse errorResponse = RpcResponse.fail(null, cause);
            ctx.writeAndFlush(errorResponse).addListener(future -> {
                // 发送错误响应后关闭连接
                ctx.close();
            });
        } catch (Exception e) {
            log.error("发送错误响应失败", e);
            ctx.close();
        }
    }

    /**
     * 获取处理统计信息
     *
     * @return 统计信息
     */
    public String getStatistics() {
        return String.format("已处理请求数：%d，业务线程池状态：%s", requestCounter.get(), businessExecutor.isShutdown()?
                "已关闭":"运行中");
    }

    /**
     * 销毁处理器，关闭线程池
     */
    public void destroy() {
        if(businessExecutor != null && !businessExecutor.isShutdown()) {
            businessExecutor.shutdown();
            try {
                if(!businessExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    businessExecutor.shutdownNow();
                }
                log.info("业务线程池已关闭");
            } catch (InterruptedException e) {
                businessExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
