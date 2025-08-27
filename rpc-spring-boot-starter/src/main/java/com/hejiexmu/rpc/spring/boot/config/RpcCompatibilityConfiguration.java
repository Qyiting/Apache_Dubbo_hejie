package com.hejiexmu.rpc.spring.boot.config;

import com.rpc.client.RpcClient;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.registry.ServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.server.RpcServer;
import com.rpc.server.provider.ServiceProvider;
import com.hejiexmu.rpc.spring.boot.properties.RpcProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RPC框架兼容性配置类
 * 确保编程式API和注解式API可以同时使用
 * 
 * @author hejiexmu
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "rpc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RpcCompatibilityConfiguration {
    
    /**
     * 提供RpcProxyFactory Bean，支持编程式创建代理
     */
    @Bean
    @ConditionalOnBean(RpcClient.class)
    @ConditionalOnMissingBean
    public RpcProxyFactory rpcProxyFactory(RpcClient rpcClient) {
        log.info("初始化RPC代理工厂，支持编程式API");
        return new RpcProxyFactory(rpcClient);
    }
    
    /**
     * 暴露ServiceProvider Bean，支持编程式服务注册
     */
    @Bean
    @ConditionalOnBean(RpcServer.class)
    @ConditionalOnMissingBean
    public ServiceProvider serviceProvider(RpcServer rpcServer) {
        log.info("暴露ServiceProvider Bean，支持编程式服务注册");
        return rpcServer.getServiceProvider();
    }
    
    /**
     * 创建RPC编程式API助手类
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcProgrammaticHelper rpcProgrammaticHelper(ServiceRegistry serviceRegistry,
                                                      RpcProperties rpcProperties) {
        log.info("初始化RPC编程式API助手类");
        return new RpcProgrammaticHelper(serviceRegistry, rpcProperties);
    }
    
    /**
     * RPC编程式API助手类
     * 提供便捷的编程式API使用方法
     */
    public static class RpcProgrammaticHelper {
        private final ServiceRegistry serviceRegistry;
        private final RpcProperties rpcProperties;
        private volatile RpcClient sharedRpcClient; // 共享的RpcClient实例
        
        public RpcProgrammaticHelper(ServiceRegistry serviceRegistry, RpcProperties rpcProperties) {
            this.serviceRegistry = serviceRegistry;
            this.rpcProperties = rpcProperties;
        }
        
        /**
         * 创建RPC客户端
         * 
         * @return RPC客户端实例
         */
        public RpcClient createRpcClient() {
            RpcProperties.Consumer consumerConfig = rpcProperties.getConsumer();
            String loadBalancer = rpcProperties.getLoadBalancer();
            
            return new RpcClient(
                serviceRegistry,
                loadBalancer,
                consumerConfig.getTimeout(),
                consumerConfig.getConnectionPoolSize()
            );
        }
        
        /**
         * 获取共享的RPC客户端实例
         * 
         * @return 共享的RPC客户端实例
         */
        private RpcClient getSharedRpcClient() {
            if (sharedRpcClient == null) {
                synchronized (this) {
                    if (sharedRpcClient == null) {
                        sharedRpcClient = createRpcClient();
                    }
                }
            }
            return sharedRpcClient;
        }
        
        /**
         * 创建RPC服务器
         * 
         * @return RPC服务器实例
         */
        public RpcServer createRpcServer() {
            RpcProperties.Provider providerConfig = rpcProperties.getProvider();
            
            return new RpcServer(
                providerConfig.getHost(),
                providerConfig.getPort(),
                serviceRegistry
            );
        }
        
        /**
         * 创建服务代理
         * 
         * @param interfaceClass 服务接口类
         * @param <T> 服务接口类型
         * @return 服务代理对象
         */
        public <T> T createServiceProxy(Class<T> interfaceClass) {
            RpcClient rpcClient = getSharedRpcClient();
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            byte serializationType = getSerializationTypeFromServiceDiscovery(rpcClient, interfaceClass.getName(), "1.0.0", "default");
            return proxyFactory.createProxy(interfaceClass, serializationType);
        }
        
        /**
         * 创建服务代理（指定版本和分组）
         * 
         * @param interfaceClass 服务接口类
         * @param version 服务版本
         * @param group 服务分组
         * @param <T> 服务接口类型
         * @return 服务代理对象
         */
        public <T> T createServiceProxy(Class<T> interfaceClass, String version, String group) {
            RpcClient rpcClient = getSharedRpcClient();
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            byte serializationType = getSerializationTypeFromServiceDiscovery(rpcClient, interfaceClass.getName(), version, group);
            return proxyFactory.createProxy(interfaceClass, version, group, serializationType);
        }
        
        /**
         * 获取服务注册中心
         * 
         * @return 服务注册中心实例
         */
        public ServiceRegistry getServiceRegistry() {
            return serviceRegistry;
        }
        
        /**
         * 获取RPC配置属性
         * 
         * @return RPC配置属性
         */
        public RpcProperties getRpcProperties() {
            return rpcProperties;
        }
        
        /**
          * 从服务发现中获取序列化类型
          * 
          * @param rpcClient RPC客户端
          * @param serviceName 服务名称
          * @param version 服务版本
          * @param group 服务分组
          * @return 序列化类型
          */
         private byte getSerializationTypeFromServiceDiscovery(RpcClient rpcClient, String serviceName, String version, String group) {
             try {
                 // 从服务注册中心发现服务
                 java.util.List<ServiceInfo> serviceInfos = rpcClient.getServiceRegistry().discover(serviceName, version, group);
                 
                 if (serviceInfos != null && !serviceInfos.isEmpty()) {
                     // 获取第一个可用服务的序列化类型
                     ServiceInfo serviceInfo = serviceInfos.get(0);
                     byte serializationType = serviceInfo.getSerializerType();
                     log.debug("从服务发现获取序列化类型：服务={}，版本={}，分组={}，序列化类型={}", 
                              serviceName, version, group, serializationType);
                     return serializationType;
                 } else {
                     log.warn("未发现服务实例：服务={}，版本={}，分组={}，使用默认序列化类型KRYO", 
                             serviceName, version, group);
                     return Serializer.SerializerType.KRYO; // 默认使用KRYO
                 }
             } catch (Exception e) {
                 log.error("从服务发现获取序列化类型失败：服务={}，版本={}，分组={}，使用默认序列化类型KRYO", 
                          serviceName, version, group, e);
                 return Serializer.SerializerType.KRYO; // 出错时使用默认序列化类型
             }
         }
    }
}