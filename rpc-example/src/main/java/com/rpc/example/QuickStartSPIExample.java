package com.rpc.example;

import com.rpc.client.RpcClient;
import com.rpc.client.loadbalance.LoadBalancerFactory;
import com.rpc.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * SPI负载均衡器快速入门示例
 * 展示最常用的SPI功能
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class QuickStartSPIExample {
    
    public static void main(String[] args) {
        // 1. 查看所有可用的负载均衡算法
        log.info("=== 可用的负载均衡算法 ===");
        List<String> algorithms = new ArrayList<>(LoadBalancerFactory.getSupportedAlgorithms());
        algorithms.forEach(algorithm -> log.info("- {}", algorithm));
        
        // 2. 使用字符串创建RpcClient（推荐方式）
        log.info("\n=== 使用SPI创建RpcClient ===");
        
        // 模拟服务注册中心
        ServiceRegistry registry = new MockServiceRegistry();
        
        // 使用不同的负载均衡算法创建客户端
        createClientWithAlgorithm(registry, "random");
        createClientWithAlgorithm(registry, "round_robin");
        createClientWithAlgorithm(registry, "lru");
        
        // 3. 动态切换负载均衡算法
        log.info("\n=== 动态切换负载均衡算法 ===");
        demonstrateDynamicSwitching(registry);
    }
    
    /**
     * 使用指定算法创建RpcClient
     */
    private static void createClientWithAlgorithm(ServiceRegistry registry, String algorithm) {
        try {
            RpcClient client = new RpcClient(registry, algorithm);
            log.info("✓ 成功创建使用 '{}' 算法的客户端", client.getCurrentLoadBalancerAlgorithm());
            client.close();
        } catch (Exception e) {
            log.error("✗ 创建客户端失败: {}", e.getMessage());
        }
    }
    
    /**
     * 演示动态切换负载均衡算法
     */
    private static void demonstrateDynamicSwitching(ServiceRegistry registry) {
        try {
            // 创建初始客户端
            RpcClient client = new RpcClient(registry, "random");
            log.info("初始算法: {}", client.getCurrentLoadBalancerAlgorithm());
            
            // 动态切换到不同算法
            String[] algorithms = {"round_robin", "lru", "consistent_hash"};
            for (String algorithm : algorithms) {
                client.setLoadBalancer(algorithm);
                log.info("切换到: {}", client.getCurrentLoadBalancerAlgorithm());
            }
            
            client.close();
            log.info("✓ 动态切换演示完成");
            
        } catch (Exception e) {
            log.error("✗ 动态切换失败: {}", e.getMessage());
        }
    }
    
    /**
     * 简化的模拟服务注册中心
     */
    private static class MockServiceRegistry implements ServiceRegistry {
        @Override
        public void register(com.rpc.core.serviceinfo.ServiceInfo serviceInfo) {}
        
        @Override
        public void unregister(com.rpc.core.serviceinfo.ServiceInfo serviceInfo) {}
        
        @Override
        public java.util.List<com.rpc.core.serviceinfo.ServiceInfo> discover(String serviceName) {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public java.util.List<com.rpc.core.serviceinfo.ServiceInfo> discover(String serviceName, String version, String group) {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public void subscribe(String serviceName, ServiceChangeListener listener) {}
        
        @Override
        public void unsubscribe(String serviceName, ServiceChangeListener listener) {}
        
        @Override
        public java.util.List<String> getAllServiceNames() {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public boolean exists(String serviceName) {
            return false;
        }
        
        @Override
        public int getServiceInstanceCount(String serviceName) {
            return 0;
        }
        
        @Override
        public void destroy() {}
        
        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}