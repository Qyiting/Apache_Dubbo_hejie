package com.hejiexmu.rpc.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * RPC服务提供者应用注解
 * 用于标记Spring Boot应用为RPC服务提供者
 * 
 * @author hejiexmu
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcProvider {
    
    /**
     * 服务提供者监听端口
     */
    int port() default 8080;
    
    /**
     * 服务提供者主机地址
     */
    String host() default "localhost";
    
    /**
     * 序列化类型
     * 可选值：kryo, json, protobuf, hessian
     */
    String serializer() default "kryo";
    
    /**
     * 是否启用服务提供者
     */
    boolean enabled() default true;
}