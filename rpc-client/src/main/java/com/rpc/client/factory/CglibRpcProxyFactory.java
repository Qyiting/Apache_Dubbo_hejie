package com.rpc.client.factory;

import com.rpc.client.RpcClient;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * @author 何杰
 * @version 1.0
 * CGLIB动态代理工厂类
 */
@Slf4j
@Data
public class CglibRpcProxyFactory {
    /** RPC客户端 */
    private final RpcClient rpcClient;
    /** 服务版本 */
    private String version = "1.0.0";
    /** 服务分组 */
    private String group = "default";
    /** 请求超时时间（毫秒）*/
    private long timeout = 5000;

    /**
     * 构造函数
     *
     * @param rpcClient RPC客户端
     */
    public CglibRpcProxyFactory(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    /**
     * 创建服务代理
     *
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> targetClass, Byte serializationType) {
        return createProxy(targetClass, version, group, timeout, serializationType);
    }

    /**
     * 创建服务代理
     *
     * @param targetClass 目标类
     * @param version 服务版本
     * @param group 服务分组
     * @param timeout 请求超时时间
     * @param <T> 目标类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> targetClass, String version, String group, long timeout, Byte serializationType) {
        // 验证参数
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类不能为null");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("服务版本不能为空");
        }
        if (group == null || group.trim().isEmpty()) {
            throw new IllegalArgumentException("服务分组不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于0");
        }

        log.info("创建CGLIB RPC代理：targetClass={}，version={}，group={}，timeout={}", 
                targetClass.getName(), version, group, timeout);

        try {
            // 创建CGLIB动态代理
            Enhancer enhancer = new Enhancer();
            
            if (targetClass.isInterface()) {
                // 如果是接口，使用接口代理模式
                enhancer.setInterfaces(new Class[]{targetClass});
            } else {
                // 如果是类，使用类代理模式
                enhancer.setSuperclass(targetClass);
            }
            
            enhancer.setCallback(new RpcMethodInterceptor(targetClass, version, group, timeout, serializationType));
            
            return (T) enhancer.create();
            
        } catch (ExceptionInInitializerError e) {
            log.error("CGLIB初始化失败，请检查Java版本和JVM参数", e);
            log.error("解决方案：添加JVM参数 --add-opens java.base/java.lang=ALL-UNNAMED");
            throw new RuntimeException("CGLIB初始化失败，请参考CGLIB_JVM_ARGS.md解决", e);
        }
    }

    /**
     * 创建异步服务代理
     *
     * @param targetClass 目标类
     * @param <T> 目标类型
     * @return 异步服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createAsyncProxy(Class<T> targetClass, Byte serializationType) {
        return createAsyncProxy(targetClass, version, group, timeout, serializationType);
    }

    /**
     * 创建异步服务代理
     *
     * @param targetClass 目标类
     * @param version 服务版本
     * @param group 服务分组
     * @param timeout 请求超时时间
     * @param <T> 目标类型
     * @return 异步服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createAsyncProxy(Class<T> targetClass, String version, String group, long timeout, Byte serializationType) {
        // 验证参数
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类不能为null");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("服务版本不能为空");
        }
        if (group == null || group.trim().isEmpty()) {
            throw new IllegalArgumentException("服务分组不能为空");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于0");
        }

        log.info("创建CGLIB异步RPC代理：targetClass={}，version={}，group={}，timeout={}", 
                targetClass.getName(), version, group, timeout);

        try {
            // 创建CGLIB动态代理
            Enhancer enhancer = new Enhancer();
            
            if (targetClass.isInterface()) {
                // 如果是接口，使用接口代理模式
                enhancer.setInterfaces(new Class[]{targetClass});
            } else {
                // 如果是类，使用类代理模式
                enhancer.setSuperclass(targetClass);
            }
            
            enhancer.setCallback(new AsyncRpcMethodInterceptor(targetClass, version, group, timeout, serializationType));
            
            return (T) enhancer.create();
            
        } catch (ExceptionInInitializerError e) {
            log.error("CGLIB初始化失败，请检查Java版本和JVM参数", e);
            log.error("解决方案：添加JVM参数 --add-opens java.base/java.lang=ALL-UNNAMED");
            throw new RuntimeException("CGLIB初始化失败，请参考CGLIB_JVM_ARGS.md解决", e);
        }
    }

    /**
     * RPC方法拦截器（同步）
     */
    private class RpcMethodInterceptor implements MethodInterceptor {
        private final Class<?> targetClass;
        private final String version;
        private final String group;
        private final long timeout;
        private final Byte serializationType;

        public RpcMethodInterceptor(Class<?> targetClass, String version, String group, long timeout, Byte serializationType) {
            this.targetClass = targetClass;
            this.version = version;
            this.group = group;
            this.timeout = timeout;
            this.serializationType = serializationType;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            // 处理Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                return proxy.invokeSuper(obj, args);
            }

            // 构建RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            
            // 发送请求并获取响应
            RpcResponse response = rpcClient.sendRequest(request, timeout, serializationType);
            
            // 处理响应
            if (response.getException() != null) {
                throw response.getException();
            }
            
            return response.getResult();
        }

        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            RpcRequest request = new RpcRequest();
            request.setInterfaceName(targetClass.getName());
            request.setMethodName(method.getName());
            request.setParameterTypes(method.getParameterTypes());
            request.setParameters(args);
            request.setVersion(version);
            request.setGroup(group);
            request.setTimeout(timeout);
            return request;
        }
    }

    /**
     * 异步RPC方法拦截器
     */
    private class AsyncRpcMethodInterceptor implements MethodInterceptor {
        private final Class<?> targetClass;
        private final String version;
        private final String group;
        private final long timeout;
        private final Byte serializationType;

        public AsyncRpcMethodInterceptor(Class<?> targetClass, String version, String group, long timeout, Byte serializationType) {
            this.targetClass = targetClass;
            this.version = version;
            this.group = group;
            this.timeout = timeout;
            this.serializationType = serializationType;
        }

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            // 处理Object类的方法
            if (method.getDeclaringClass() == Object.class) {
                return proxy.invokeSuper(obj, args);
            }

            // 构建RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            
            // 异步发送请求
            CompletableFuture<RpcResponse> future = rpcClient.sendRequestAsync(request, timeout, serializationType);
            
            // 转换为方法返回类型的CompletableFuture
            return future.thenApply(response -> {
                if (response.getException() != null) {
                    throw new RuntimeException(response.getException());
                }
                return response.getResult();
            });
        }

        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            RpcRequest request = new RpcRequest();
            request.setInterfaceName(targetClass.getName());
            request.setMethodName(method.getName());
            request.setParameterTypes(method.getParameterTypes());
            request.setParameters(args);
            request.setVersion(version);
            request.setGroup(group);
            request.setTimeout(timeout);
            return request;
        }
    }
}