package com.hejiexmu.rpc.spring.boot.config;

import com.rpc.client.RpcClient;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.server.RpcServer;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import com.hejiexmu.rpc.spring.boot.properties.RpcProperties;
import com.hejiexmu.rpc.spring.boot.processor.RpcReferenceProcessor;
import com.hejiexmu.rpc.spring.boot.processor.RpcServiceProcessor;
import com.hejiexmu.rpc.spring.boot.config.RpcServerAutoStarter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

/**
 * RPC框架自动配置类
 * 
 * @author hejiexmu
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RpcProperties.class)
@ConditionalOnProperty(prefix = "rpc", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({RpcCompatibilityConfiguration.class})
public class RpcAutoConfiguration {
    
    public RpcAutoConfiguration() {
        log.info("RpcAutoConfiguration 正在初始化...");
    }
    
    /**
     * 配置服务注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.registry", name = "type", havingValue = "zookeeper", matchIfMissing = true)
    public ServiceRegistry serviceRegistry(RpcProperties rpcProperties) {
        RpcProperties.Registry registryConfig = rpcProperties.getRegistry();
        log.info("初始化服务注册中心：类型={}, 地址={}", registryConfig.getType(), registryConfig.getAddress());
        
        return new ZookeeperServiceRegistry(
            registryConfig.getAddress(),
            registryConfig.getSessionTimeout(),
            3 // 重试次数
        );
    }
    
    /**
     * 配置序列化器
     */
    @Bean
    @ConditionalOnMissingBean
    public Serializer serializer(RpcProperties rpcProperties) {
        byte serializerType = rpcProperties.getProvider().getSerializer();
        log.info("初始化序列化器：类型={}", serializerType);
        
        return SerializerFactory.getSerializer(serializerType);
    }
    
    /**
     * 配置RPC服务器（服务提供者）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.provider", name = "enabled", havingValue = "true")
    @DependsOn({"serviceRegistry", "serializer"})
    public RpcServer rpcServer(ServiceRegistry serviceRegistry, 
                              Serializer serializer,
                              RpcProperties rpcProperties) {
        RpcProperties.Provider providerConfig = rpcProperties.getProvider();
        log.info("初始化RPC服务器：主机={}, 端口={}", providerConfig.getHost(), providerConfig.getPort());
        
        return new RpcServer(
            providerConfig.getHost(),
            providerConfig.getPort(),
            null, // 使用默认ServiceProvider
            serviceRegistry,
            serializer
        );
    }
    
    /**
     * 配置RPC客户端（服务消费者）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.consumer", name = "enabled", havingValue = "true")
    @DependsOn("serviceRegistry")
    public RpcClient rpcClient(ServiceRegistry serviceRegistry, RpcProperties rpcProperties) {
        RpcProperties.Consumer consumerConfig = rpcProperties.getConsumer();
        String loadBalancer = rpcProperties.getLoadBalancer();
        
        log.info("初始化RPC客户端：负载均衡={}, 超时={}, 连接池大小={}", 
                loadBalancer, consumerConfig.getTimeout(), consumerConfig.getConnectionPoolSize());
        
        return new RpcClient(
            serviceRegistry,
            loadBalancer,
            consumerConfig.getTimeout(),
            consumerConfig.getConnectionPoolSize()
        );
    }
    
    /**
     * 配置RPC服务处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rpc.provider", name = "enabled", havingValue = "true")
    public RpcServiceProcessor rpcServiceProcessor(RpcServer rpcServer) {
        log.info("初始化RPC服务处理器");
        return new RpcServiceProcessor(rpcServer);
    }
    
    /**
     * 配置RPC引用处理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "rpc.consumer", name = "enabled", havingValue = "true")
    public RpcReferenceProcessor rpcReferenceProcessor(RpcClient rpcClient, RpcProperties rpcProperties) {
        log.info("初始化RPC引用处理器");
        return new RpcReferenceProcessor(rpcClient, rpcProperties);
    }
    
    /**
     * 配置RPC服务器自动启动器
     */
    @Bean
    @ConditionalOnMissingBean(RpcServerAutoStarter.class)
    @ConditionalOnProperty(prefix = "rpc.provider", name = "enabled", havingValue = "true")
    @DependsOn("rpcServer")
    public RpcServerAutoStarter rpcServerAutoStarter(RpcServer rpcServer) {
        log.info("初始化RPC服务器自动启动器");
        log.info("RpcServer实例: {}", rpcServer != null ? "存在" : "不存在");
        return new RpcServerAutoStarter(rpcServer);
    }
}