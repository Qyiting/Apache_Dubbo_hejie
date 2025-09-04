package com.hejiexmu.rpc.auth.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis配置类
 * 配置Redis连接和序列化策略
 * 
 * @author hejiexmu
 */
@Configuration
public class RedisConfig {
    
    /**
     * Redis连接工厂
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        // 判断是否使用集群模式
        if (redisProperties.getCluster() != null && redisProperties.getCluster().getNodes() != null 
                && !redisProperties.getCluster().getNodes().isEmpty()) {
            // 集群模式
            return createClusterConnectionFactory(redisProperties);
        } else {
            // 单机模式
            return createStandaloneConnectionFactory(redisProperties);
        }
    }
    
    /**
     * 创建单机模式连接工厂
     */
    private RedisConnectionFactory createStandaloneConnectionFactory(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setDatabase(redisProperties.getDatabase());
        
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            config.setPassword(redisProperties.getPassword());
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setValidateConnection(true);
        return factory;
    }
    
    /**
     * 创建集群模式连接工厂
     */
    private RedisConnectionFactory createClusterConnectionFactory(RedisProperties redisProperties) {
        RedisClusterConfiguration clusterConfig =
                new RedisClusterConfiguration();
        
        // 添加集群节点
        for (String node : redisProperties.getCluster().getNodes()) {
            String[] parts = node.split(":");
            if (parts.length == 2) {
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                clusterConfig.addClusterNode(new RedisNode(host, port));
            }
        }
        
        // 设置最大重定向次数
        if (redisProperties.getCluster().getMaxRedirects() > 0) {
            clusterConfig.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
        }
        
        // 设置密码（如果有）
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isEmpty()) {
            clusterConfig.setPassword(redisProperties.getPassword());
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig);
        factory.setValidateConnection(true);
        return factory;
    }
    
    /**
     * RedisTemplate配置
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 配置序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = jackson2JsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        // value序列化方式采用jackson
        template.setValueSerializer(jackson2JsonRedisSerializer);
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Jackson2JsonRedisSerializer配置
     */
    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        
        // 注册JavaTimeModule以支持LocalDateTime等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        
        // Spring Data Redis 3.0+ 使用构造函数传入ObjectMapper
        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
    
    /**
     * Redis属性配置
     */
    @Component
    @ConfigurationProperties(prefix = "spring.redis")
    public static class RedisProperties {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private int timeout = 2000;
        private Pool pool = new Pool();
        private Cluster cluster;
        
        // Getters and Setters
        public String getHost() {
            return host;
        }
        
        public void setHost(String host) {
            this.host = host;
        }
        
        public int getPort() {
            return port;
        }
        
        public void setPort(int port) {
            this.port = port;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public int getDatabase() {
            return database;
        }
        
        public void setDatabase(int database) {
            this.database = database;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
        
        public Pool getPool() {
            return pool;
        }
        
        public void setPool(Pool pool) {
            this.pool = pool;
        }
        
        public Cluster getCluster() {
            return cluster;
        }
        
        public void setCluster(Cluster cluster) {
            this.cluster = cluster;
        }
        
        /**
         * 连接池配置
         */
        public static class Pool {
            private int maxActive = 8;
            private int maxWait = -1;
            private int maxIdle = 8;
            private int minIdle = 0;
            
            // Getters and Setters
            public int getMaxActive() {
                return maxActive;
            }
            
            public void setMaxActive(int maxActive) {
                this.maxActive = maxActive;
            }
            
            public int getMaxWait() {
                return maxWait;
            }
            
            public void setMaxWait(int maxWait) {
                this.maxWait = maxWait;
            }
            
            public int getMaxIdle() {
                return maxIdle;
            }
            
            public void setMaxIdle(int maxIdle) {
                this.maxIdle = maxIdle;
            }
            
            public int getMinIdle() {
                return minIdle;
            }
            
            public void setMinIdle(int minIdle) {
                this.minIdle = minIdle;
            }
        }
        
        /**
         * 集群配置
         */
        public static class Cluster {
            private List<String> nodes;
            private int maxRedirects = 3;
            
            public List<String> getNodes() {
                return nodes;
            }
            
            public void setNodes(List<String> nodes) {
                this.nodes = nodes;
            }
            
            public int getMaxRedirects() {
                return maxRedirects;
            }
            
            public void setMaxRedirects(int maxRedirects) {
                this.maxRedirects = maxRedirects;
            }
        }
    }
}