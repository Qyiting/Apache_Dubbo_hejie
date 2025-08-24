package com.hejiexmu.rpc.spring.boot.config;

import com.rpc.client.RpcClient;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.registry.ServiceRegistry;
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
            RpcClient rpcClient = createRpcClient();
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            byte serializationType = rpcProperties.getProvider().getSerializer();
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
            RpcClient rpcClient = createRpcClient();
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            byte serializationType = rpcProperties.getProvider().getSerializer();
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
    }
}