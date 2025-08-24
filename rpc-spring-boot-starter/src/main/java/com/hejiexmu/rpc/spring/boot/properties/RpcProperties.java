package com.hejiexmu.rpc.spring.boot.properties;

import com.rpc.serialization.Serializer;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC框架配置属性
 * 
 * @author hejiexmu
 */
@Data
@ConfigurationProperties(prefix = "rpc")
public class RpcProperties {
    
    /**
     * 是否启用RPC框架
     */
    private boolean enabled = true;
    
    /**
     * 默认负载均衡策略
     */
    private String loadBalancer = "random";
    
    /**
     * 注册中心配置
     */
    private Registry registry = new Registry();
    
    /**
     * 服务提供者配置
     */
    private Provider provider = new Provider();
    
    /**
     * 服务消费者配置
     */
    private Consumer consumer = new Consumer();
    
    /**
     * 注册中心配置
     */
    @Data
    public static class Registry {
        /**
         * 注册中心类型
         */
        private String type = "zookeeper";
        
        /**
         * 注册中心地址
         */
        private String address = "127.0.0.1:2181";
        
        /**
         * 连接超时时间
         */
        private int connectTimeout = 5000;
        
        /**
         * 会话超时时间
         */
        private int sessionTimeout = 60000;
    }
    
    /**
     * 服务提供者配置
     */
    @Data
    public static class Provider {
        /**
         * 是否启用服务提供者
         */
        private boolean enabled = true;
        
        /**
         * 服务提供者主机
         */
        private String host = "localhost";
        
        /**
         * 服务提供者端口
         */
        private int port = 9999;
        
        /**
         * 序列化类型
         */
        private byte serializer = Serializer.SerializerType.KRYO;
        
        /**
         * 工作线程数
         */
        private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    }
    
    /**
     * 服务消费者配置
     */
    @Data
    public static class Consumer {
        /**
         * 是否启用服务消费者
         */
        private boolean enabled = true;
        
        /**
         * 默认超时时间
         */
        private long timeout = 5000L;
        
        /**
         * 默认重试次数
         */
        private int retryCount = 3;
        
        /**
         * 连接池大小
         */
        private int connectionPoolSize = 10;
    }
}