package com.hejiexmu.rpc.samples.consumer;

import com.hejiexmu.rpc.samples.api.entity.User;
import com.hejiexmu.rpc.samples.api.service.UserService;
import com.hejiexmu.rpc.spring.boot.config.RpcCompatibilityConfiguration.RpcProgrammaticHelper;
import com.hejiexmu.rpc.spring.boot.properties.RpcProperties;
import com.rpc.client.RpcClient;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 编程式API使用示例
 * 展示如何在Spring Boot环境中使用编程式RPC API
 * 
 * @author hejiexmu
 */
@Slf4j
// @Component  // 暂时禁用自动执行，避免在启动时调用不存在的服务版本
public class ProgrammaticApiExample implements CommandLineRunner {
    

    
    @Autowired
    private RpcProgrammaticHelper rpcHelper;
    
    @Autowired(required = false)
    private RpcClient rpcClient;
    
    @Autowired(required = false)
    private RpcProxyFactory rpcProxyFactory;
    
    @Autowired
    private ServiceRegistry serviceRegistry;
    
    @Autowired
    private RpcProperties rpcProperties;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("=== 编程式API使用示例 ===");
        
        // 等待服务启动
        Thread.sleep(2000);
        
        try {
            // 示例1：使用RpcProgrammaticHelper创建服务代理
            demonstrateHelperApi();
            
            // 示例2：直接使用RpcProxyFactory（如果可用）
            if (rpcProxyFactory != null) {
                demonstrateProxyFactoryApi();
            }
            
            // 示例3：直接使用RpcClient（如果可用）
            if (rpcClient != null) {
                demonstrateRpcClientApi();
            }
            
        } catch (Exception e) {
            log.warn("编程式API示例执行失败（可能是服务未启动）：{}", e.getMessage());
        }
    }
    
    /**
     * 示例1：使用RpcProgrammaticHelper创建服务代理
     */
    private void demonstrateHelperApi() {
        log.info("--- 示例1：使用RpcProgrammaticHelper ---");
        
        try {
            // 创建默认版本的服务代理
            UserService userService = rpcHelper.createServiceProxy(com.hejiexmu.rpc.samples.api.service.UserService.class);
            
            // 调用服务方法
            List<User> users = userService.getAllUsers();
            log.info("通过Helper API获取到 {} 个用户", users.size());
            
            if (!users.isEmpty()) {
                User firstUser = users.get(0);
                log.info("第一个用户：{}", firstUser.getUsername());
            }
            
            // 创建指定版本和分组的服务代理
            UserService userServiceV2 = rpcHelper.createServiceProxy(com.hejiexmu.rpc.samples.api.service.UserService.class, "2.0.0", "test");
            Long userCount = userServiceV2.getUserCount();
            log.info("通过Helper API (v2.0.0) 获取用户总数：{}", userCount);
            
        } catch (Exception e) {
            log.error("Helper API示例执行失败", e);
        }
    }
    
    /**
     * 示例2：直接使用RpcProxyFactory
     */
    private void demonstrateProxyFactoryApi() {
        log.info("--- 示例2：使用RpcProxyFactory ---");
        
        try {
            // 获取序列化类型
            byte serializationType = rpcProperties.getProvider().getSerializer();
            
            // 创建服务代理
            UserService userService = rpcProxyFactory.createProxy(com.hejiexmu.rpc.samples.api.service.UserService.class, serializationType);
            
            // 调用服务方法
            boolean exists = userService.existsByUsername("admin");
            log.info("通过ProxyFactory API检查用户'admin'是否存在：{}", exists);
            
            // 获取年龄范围内的用户
            List<User> youngUsers = userService.getUsersByAgeRange(20, 30);
            log.info("通过ProxyFactory API获取20-30岁用户数量：{}", youngUsers.size());
            
        } catch (Exception e) {
            log.error("ProxyFactory API示例执行失败", e);
        }
    }
    
    /**
     * 示例3：直接使用RpcClient（低级API）
     */
    private void demonstrateRpcClientApi() {
        log.info("--- 示例3：使用RpcClient低级API ---");
        
        try {
            // 注意：这里只是演示RpcClient的存在性
            // 实际的低级API调用需要构建RpcRequest等，比较复杂
            log.info("RpcClient实例可用，可以进行低级API调用");
            log.info("RpcClient类型：{}", rpcClient.getClass().getSimpleName());
            
            // 在实际应用中，推荐使用代理工厂而不是直接使用RpcClient
            
        } catch (Exception e) {
            log.error("RpcClient API示例执行失败", e);
        }
    }
    
    /**
     * 演示如何在业务代码中混合使用编程式和注解式API
     */
    public void demonstrateMixedUsage() {
        log.info("=== 混合使用编程式和注解式API ===");
        
        try {
            // 编程式：动态创建不同版本的服务代理
            UserService defaultService = rpcHelper.createServiceProxy(com.hejiexmu.rpc.samples.api.service.UserService.class);
            UserService testService = rpcHelper.createServiceProxy(com.hejiexmu.rpc.samples.api.service.UserService.class, "1.0.0", "test");
            
            // 比较不同版本服务的响应
            Long defaultCount = defaultService.getUserCount();
            Long testCount = testService.getUserCount();
            
            log.info("默认服务用户数：{}，测试服务用户数：{}", defaultCount, testCount);
            
            // 注解式API会在Controller中自动注入使用
            // 编程式API可以在需要动态选择服务版本时使用
            
        } catch (Exception e) {
            log.error("混合使用示例执行失败", e);
        }
    }
}