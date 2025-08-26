package com.hejiexmu.rpc.samples.provider;

import com.hejiexmu.rpc.spring.boot.annotation.RpcProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

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
        
        // 获取配置信息
        Environment env = context.getEnvironment();
        String webPort = env.getProperty("server.port", "8081");
        String rpcPort = env.getProperty("rpc.provider.port", "9081");
        String host = env.getProperty("rpc.provider.host", "localhost");
        
        log.info("RPC服务提供者启动成功！");
        log.info("服务地址：{}:{}", host, rpcPort);
        log.info("Web管理界面：http://{}:{}", host, webPort);
        log.info("按 Ctrl+C 停止服务");
        
        // 添加优雅关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭RPC服务提供者...");
            context.close();
            log.info("RPC服务提供者已关闭");
        }));
    }
    

}