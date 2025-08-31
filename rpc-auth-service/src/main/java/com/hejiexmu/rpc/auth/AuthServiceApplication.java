package com.hejiexmu.rpc.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 认证服务启动类
 * 
 * @author hejiexmu
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = {
    "com.hejiexmu.rpc.auth",
    "com.hejiexmu.rpc.common"
})
public class AuthServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}