package com.rpc.client.factory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Type;
import java.util.List;

import com.rpc.client.RpcClient;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.core.retry.RetryStrategy;
import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.serialization.Serializer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
@Data
public class RpcProxyFactory {
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
    public RpcProxyFactory(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    /**
     * 创建服务代理（自动序列化协商）
     *
     * @param interfaceClass 服务接口类
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass) {
        return createProxy(interfaceClass, version, group, timeout);
    }
    
    /**
     * 创建服务代理
     *
     * @param interfaceClass 服务接口类
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, Byte serializationType) {
        return createProxy(interfaceClass, version, group, timeout, serializationType);
    }
    /**
     * 创建服务代理（自动序列化协商）
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, String version) {
        return createProxy(interfaceClass, version, group, timeout);
    }
    
    /**
     * 创建服务代理
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, String version, Byte serializationType) {
        return createProxy(interfaceClass, version, group, timeout, serializationType);
    }

    /**
     * 创建服务代理（自动序列化协商）
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, String version, String group) {
        return createProxy(interfaceClass, version, group, timeout);
    }
    
    /**
     * 创建服务代理
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> interfaceClass, String version, String group, Byte serializationType) {
        return createProxy(interfaceClass, version, group, timeout, serializationType);
    }

    /**
     * 创建服务代理（自动序列化协商）
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     * @param timeout 请求超时时间
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    public <T> T createProxy(Class<T> interfaceClass, String version, String group, long timeout) {
        // 从服务发现中获取序列化类型
        Byte serializationType = getSerializationTypeFromServiceDiscovery(interfaceClass.getName(), version, group);
        return createProxy(interfaceClass, version, group, timeout, serializationType);
    }
    
    /**
     * 创建服务代理
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     * @param timeout 请求超时时间
     * @param <T> 服务接口类型
     * @return 服务代理对象
     */
    public <T> T createProxy(Class<T> interfaceClass, String version, String group, long timeout, Byte serializationType) {
        // 验证参数
        if(interfaceClass == null) {
            throw new IllegalArgumentException("接口类不能为null");
        }
        if(!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("只能为接口创建代理：" + interfaceClass.getName());
        }
        if(version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("服务版本不能为空");
        }
        if(group == null || group.trim().isEmpty()) {
            throw new IllegalArgumentException("服务分组不能为空");
        }
        if(timeout <= 0) {
            throw new IllegalArgumentException("超时时间必须大于0");
        }
        log.info("创建RPC代理：interface={}，version={}，group={}，timeout={}", interfaceClass.getName(),
                version, group, timeout);
        // 创建动态代理
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new RpcInvocationHandler(interfaceClass, version, group, timeout, serializationType));
    }

    /**
     * 创建异步服务代理
     * 所有方法调用都返回CompletableFuture
     *
     * @param interfaceClass 服务接口类
     * @param <T> 服务接口类型
     * @return 异步服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createAsyncProxy(Class<T> interfaceClass, Byte serializationType) {
        return createAsyncProxy(interfaceClass, version, group, timeout, serializationType);
    }

    /**
     * 创建异步服务代理
     *
     * @param interfaceClass 服务接口类
     * @param version 服务版本
     * @param group 服务分组
     * @param timeout 请求超时时间
     * @param <T> 服务接口类型
     * @return 异步服务代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T createAsyncProxy(Class<T> interfaceClass, String version, String group, long timeout, Byte serializationType) {
        // 验证参数
        if(interfaceClass == null) {
            throw new IllegalArgumentException("接口类不能为null");
        }
        if(!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("只能为接口类创建代理："+ interfaceClass.getName());
        }
        log.info("创建异步RPC代理：interface={}，version={}，group={}。timeout={}", interfaceClass.getName(),
                version, group, timeout);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new AsyncRpcInvocationHandler(interfaceClass, version, group, timeout, serializationType));
    }


    /**
     * RPC调用处理器（同步）
     */
    private class RpcInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final String version;
        private final String group;
        private final long timeout;
        private final Byte serializationType;

        public RpcInvocationHandler(Class<?> interfaceClass, String version, String group, long timeout, Byte serializationType) {
            this.interfaceClass = interfaceClass;
            this.version = version;
            this.group = group;
            this.timeout = timeout;
            this.serializationType = serializationType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object类的方法
            if(method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            // 构建RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            
            // 使用重试策略发送请求，支持序列化协商
            String serviceName = interfaceClass.getName();
            RpcResponse response;
            
            try {
                response = RetryStrategy.executeWithRetry(() -> {
                    try {
                        return sendRequestWithSerializationNegotiation(request, timeout);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, serviceName);
            } catch (Exception e) {
                // 如果重试后仍然失败，检查是否是业务异常
                if (e.getCause() instanceof Exception) {
                    Exception cause = (Exception) e.getCause();
                    if (cause instanceof RuntimeException && cause.getMessage() != null && 
                        cause.getMessage().startsWith("RPC调用失败")) {
                        throw cause;
                    }
                }
                throw e;
            }
            
            // 处理响应
            if(response.getException() != null) {
                throw response.getException();
            }
            
            // 处理JSON序列化的类型转换问题
            Object result = response.getResult();
            if (result != null && serializationType != null && serializationType == Serializer.SerializerType.JSON) { // JSON序列化类型
                result = convertJsonResult(result, method.getReturnType(), method.getGenericReturnType());
            }
            
            return result;
        }
        
        /**
         * 发送请求并支持序列化协商
         */
        private RpcResponse sendRequestWithSerializationNegotiation(RpcRequest request, long timeout) throws Exception {
            byte currentSerializationType = serializationType;
            
            try {
                // 首先尝试使用当前序列化类型
                return rpcClient.sendRequest(request, timeout, currentSerializationType);
            } catch (Exception e) {
                // 如果失败且错误信息包含序列化相关内容，尝试协商
                if (isSerializationError(e)) {
                    log.warn("序列化类型不匹配，尝试协商：当前类型={}, 错误={}", currentSerializationType, e.getMessage());
                    
                    // 尝试其他序列化类型
                    byte[] fallbackTypes = {Serializer.SerializerType.KRYO, Serializer.SerializerType.JSON, 
                                           Serializer.SerializerType.HESSIAN, Serializer.SerializerType.PROTOSTUFF};
                    
                    for (byte fallbackType : fallbackTypes) {
                        if (fallbackType != currentSerializationType) {
                            try {
                                log.info("尝试使用序列化类型：{}", fallbackType);
                                RpcResponse response = rpcClient.sendRequest(request, timeout, fallbackType);
                                log.info("序列化协商成功，使用类型：{}", fallbackType);
                                return response;
                            } catch (Exception fallbackException) {
                                log.debug("序列化类型 {} 协商失败：{}", fallbackType, fallbackException.getMessage());
                            }
                        }
                    }
                }
                
                // 如果协商失败，抛出原始异常
                throw e;
            }
        }
        

        
        /**
         * 转换JSON反序列化的结果
         * 解决JSON反序列化时将复杂对象反序列化为LinkedHashMap的问题
         */
        private Object convertJsonResult(Object result, Class<?> returnType, Type genericReturnType) {
            if (result == null || returnType == null || returnType == Object.class) {
                return result;
            }
            
            // 如果结果已经是期望的类型，直接返回
            if (returnType.isInstance(result)) {
                return result;
            }
            
            // 如果是LinkedHashMap或ArrayList，需要重新序列化和反序列化
            if (result instanceof LinkedHashMap || (result instanceof List && !returnType.isInstance(result))) {
                try {
                    // 使用Jackson重新转换
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.registerModule(new JavaTimeModule());
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    
                    // 将结果转换为JSON字符串
                    String json = mapper.writeValueAsString(result);
                    
                    // 使用泛型类型信息进行反序列化
                    if (genericReturnType != null && genericReturnType != returnType) {
                        JavaType javaType = mapper.getTypeFactory().constructType(genericReturnType);
                        return mapper.readValue(json, javaType);
                    } else {
                        return mapper.readValue(json, returnType);
                    }
                } catch (Exception e) {
                    log.warn("JSON结果类型转换失败，返回原始结果: {}", e.getMessage());
                    return result;
                }
            }
            
            return result;
        }
        
        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            RpcRequest request = new RpcRequest();
            request.setInterfaceName(interfaceClass.getName());
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
     * 异步RPC调用处理器
     */
    private class AsyncRpcInvocationHandler implements InvocationHandler {
        private final Class<?> interfaceClass;
        private final String version;
        private final String group;
        private final long timeout;
        private final Byte serializationType;

        public AsyncRpcInvocationHandler(Class<?> interfaceClass, String version, String group, long timeout, Byte serializationType) {
            this.interfaceClass = interfaceClass;
            this.version = version;
            this.group = group;
            this.timeout = timeout;
            this.serializationType = serializationType;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object类的方法
            if(method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            // 检查方法返回类型是否为CompletableFuture
            Class<?> returnType = method.getReturnType();
            if (!CompletableFuture.class.isAssignableFrom(returnType)) {
                throw new IllegalArgumentException("异步代理方法必须返回CompletableFuture类型: " + method.getName());
            }
            // 构建RPC请求
            RpcRequest request = buildRpcRequest(method, args);
            // 异步发送请求并处理CompletableFuture嵌套问题
            return sendRequestAsyncWithSerializationNegotiation(request, timeout)
                    .thenCompose(response -> {
                        if(response.getException() != null) {
                            CompletableFuture<Object> failedFuture = new CompletableFuture<>();
                            failedFuture.completeExceptionally(response.getException());
                            return failedFuture;
                        }
                        
                        Object result = response.getResult();
                        // 如果服务端返回的已经是CompletableFuture，直接返回它
                        if (result instanceof CompletableFuture) {
                            return (CompletableFuture<Object>) result;
                        } else {
                            // 如果不是CompletableFuture，包装成CompletableFuture
                            return CompletableFuture.completedFuture(result);
                        }
                    });
        }
        
        private CompletableFuture<RpcResponse> sendRequestAsyncWithSerializationNegotiation(RpcRequest request, long timeout) {
            // 首先尝试使用当前序列化类型
            return rpcClient.sendRequestAsync(request, timeout, serializationType)
                    .handle((response, throwable) -> {
                        if (throwable != null && isSerializationError((Exception) throwable)) {
                            log.warn("序列化类型 {} 失败，尝试协商其他序列化类型: {}", serializationType, throwable.getMessage());
                            // 尝试其他序列化类型
                            return tryFallbackSerializationTypesAsync(request, timeout, throwable);
                        }
                        if (throwable != null) {
                            CompletableFuture<RpcResponse> failedFuture = new CompletableFuture<>();
                            failedFuture.completeExceptionally(throwable);
                            return failedFuture;
                        }
                        return CompletableFuture.completedFuture(response);
                    })
                    .thenCompose(future -> future);
        }
        
        private CompletableFuture<RpcResponse> tryFallbackSerializationTypesAsync(RpcRequest request, long timeout, Throwable originalException) {
            Byte[] fallbackTypes = {Serializer.SerializerType.KRYO, Serializer.SerializerType.JSON, Serializer.SerializerType.HESSIAN, Serializer.SerializerType.PROTOSTUFF};
            
            CompletableFuture<RpcResponse> result = CompletableFuture.completedFuture(null);
            
            for (Byte fallbackType : fallbackTypes) {
                if (!fallbackType.equals(serializationType)) {
                    result = result.thenCompose(response -> {
                        if (response != null) {
                            return CompletableFuture.completedFuture(response);
                        }
                        log.info("尝试使用序列化类型: {}", fallbackType);
                        return rpcClient.sendRequestAsync(request, timeout, fallbackType)
                                .handle((resp, ex) -> {
                                    if (ex == null) {
                                        log.info("序列化协商成功，使用类型: {}", fallbackType);
                                        return resp;
                                    }
                                    return null;
                                });
                    });
                }
            }
            
            return result.thenCompose(response -> {
                if (response != null) {
                    return CompletableFuture.completedFuture(response);
                }
                CompletableFuture<RpcResponse> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(originalException);
                return failedFuture;
            });
        }

        private RpcRequest buildRpcRequest(Method method, Object[] args) {
            RpcRequest request = new RpcRequest();
            request.setInterfaceName(interfaceClass.getName());
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
     * 判断是否为序列化相关错误
     */
    private boolean isSerializationError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        return message.contains("序列化") || message.contains("反序列化") || 
               message.contains("serialization") || message.contains("deserialization") ||
               message.contains("ClassCastException") || message.contains("类型转换");
    }
    
    /**
     * 从服务发现中获取序列化类型
     *
     * @param serviceName 服务名称
     * @param version 服务版本
     * @param group 服务分组
     * @return 序列化类型
     */
    private Byte getSerializationTypeFromServiceDiscovery(String serviceName, String version, String group) {
        try {
            // 从服务注册中心发现服务
            List<ServiceInfo> serviceInfos = rpcClient.getServiceRegistry().discover(serviceName, version, group);
            
            if (serviceInfos == null || serviceInfos.isEmpty()) {
                log.warn("未发现服务：{}，使用默认序列化类型 KRYO", serviceName);
                return Serializer.SerializerType.KRYO;
            }
            
            // 查找匹配版本和分组的服务
            for (ServiceInfo serviceInfo : serviceInfos) {
                if (version.equals(serviceInfo.getVersion()) && group.equals(serviceInfo.getGroup())) {
                    Byte serializationType = serviceInfo.getSerializerType();
                    if (serializationType != null) {
                        log.info("从服务发现获取序列化类型：服务={}，版本={}，分组={}，序列化类型={}", 
                                serviceName, version, group, serializationType);
                        return serializationType;
                    }
                }
            }
            
            // 如果没有找到匹配的版本和分组，使用第一个可用的服务的序列化类型
            ServiceInfo firstService = serviceInfos.get(0);
            Byte serializationType = firstService.getSerializerType();
            if (serializationType != null) {
                log.info("使用第一个可用服务的序列化类型：服务={}，序列化类型={}", serviceName, serializationType);
                return serializationType;
            }
            
            log.warn("服务 {} 未包含序列化类型信息，使用默认序列化类型 KRYO", serviceName);
            return Serializer.SerializerType.KRYO;
            
        } catch (Exception e) {
            log.error("从服务发现获取序列化类型失败：服务={}，错误={}", serviceName, e.getMessage());
            log.info("使用默认序列化类型 KRYO");
            return Serializer.SerializerType.KRYO;
        }
    }

}
