package com.hejiexmu.rpc.samples.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * RPC服务消费者示例应用
 * 
 * @author hejiexmu
 */
@Slf4j
@SpringBootApplication
public class ConsumerApplication {
    
    public static void main(String[] args) {
        log.info("启动RPC服务消费者示例应用...");
        
        ConfigurableApplicationContext context = SpringApplication.run(ConsumerApplication.class, args);
        
        log.info("RPC服务消费者启动成功！");
        log.info("Web服务地址：http://localhost:8082");
        log.info("API文档地址：http://localhost:8082/api/users");
        log.info("按 Ctrl+C 停止服务");
        
        // 打印可用的API端点
        log.info("可用的API端点：");
        log.info("  GET  /api/users          - 获取所有用户");
        log.info("  GET  /api/users/{id}     - 根据ID获取用户");
        log.info("  GET  /api/users/username/{username} - 根据用户名获取用户");
        log.info("  GET  /api/users/age-range?minAge=20&maxAge=30 - 根据年龄范围获取用户");
        log.info("  GET  /api/users/exists/{username} - 检查用户名是否存在");
        log.info("  GET  /api/users/count     - 获取用户总数");
        log.info("  POST /api/users          - 创建用户");
        log.info("  PUT  /api/users/{id}     - 更新用户");
        log.info("  DELETE /api/users/{id}   - 删除用户");
        log.info("  POST /api/users/batch    - 批量创建用户");
        
        // 添加优雅关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("正在关闭RPC服务消费者...");
            context.close();
            log.info("RPC服务消费者已关闭");
        }));
    }
}