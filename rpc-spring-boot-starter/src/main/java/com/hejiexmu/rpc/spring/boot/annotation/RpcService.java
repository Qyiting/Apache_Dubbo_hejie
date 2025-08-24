package com.hejiexmu.rpc.spring.boot.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * RPC服务提供者注解
 * 用于标记需要暴露为RPC服务的实现类
 * 
 * @author hejiexmu
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {
    
    /**
     * 服务接口类型，默认为实现的第一个接口
     */
    Class<?> interfaceClass() default void.class;
    
    /**
     * 服务版本号，用于服务版本控制
     */
    String version() default "1.0.0";
    
    /**
     * 服务分组，用于服务分组管理
     */
    String group() default "default";
    
    /**
     * 服务权重，用于负载均衡
     */
    int weight() default 1;
    
    /**
     * 是否启用该服务
     */
    boolean enabled() default true;
}