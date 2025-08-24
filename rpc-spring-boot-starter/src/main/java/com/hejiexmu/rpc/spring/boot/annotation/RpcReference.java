package com.hejiexmu.rpc.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * RPC服务消费者注解
 * 用于标记需要注入的RPC服务引用
 * 
 * @author hejiexmu
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {
    
    /**
     * 服务接口类型，默认为字段类型
     */
    Class<?> interfaceClass() default void.class;
    
    /**
     * 服务版本号
     */
    String version() default "1.0.0";
    
    /**
     * 服务分组
     */
    String group() default "default";
    
    /**
     * 负载均衡策略
     * 可选值：random, round_robin, consistent_hash, lru, lfu
     */
    String loadBalancer() default "random";
    
    /**
     * 超时时间（毫秒）
     */
    long timeout() default 5000L;
    
    /**
     * 重试次数
     */
    int retryCount() default 3;
    
    /**
     * 是否异步调用
     */
    boolean async() default false;
}