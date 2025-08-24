package com.hejiexmu.rpc.samples.provider;

import com.hejiexmu.rpc.spring.boot.annotation.RpcProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务提供者示例应用
 * 
 * @author hejiexmu
 */
@Slf4j
@SpringBootApplication
@RpcProvider(port = 8081, host = "localhost", enabled = true)
public class ProviderApplication {

    public static void main(String[] args) {

        log.info("启动RPC服务提供者示例应用...");
        
        ConfigurableApplicationContext context = SpringApplication.run(ProviderApplication.class, args);
        
        log.info("RPC服务提供者启动成功！");
        log.info("服务地址：localhost:8081");
        log.info("Web管理界面：http://localhost:8080");
        log.info("按 Ctrl+C 停止服务");
        
        // 添加优雅关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭RPC服务提供者...");
            context.close();
            log.info("RPC服务提供者已关闭");
        }));
    }
}