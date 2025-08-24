package com.rpc.example;

import com.rpc.client.RpcClient;
import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.client.loadbalance.LoadBalancerFactory;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SPI负载均衡器示例
 * 演示如何使用SPI机制动态加载和切换负载均衡算法
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class SPILoadBalancerExample {
    
    public static void main(String[] args) {
        try {
            // 演示LoadBalancerFactory的使用
            demonstrateLoadBalancerFactory();
            
            // 演示RpcClient的SPI支持
            demonstrateRpcClientSPI();
            
        } catch (Exception e) {
            log.error("示例运行失败", e);
        }
    }
    
    /**
     * 演示LoadBalancerFactory的使用
     */
    private static void demonstrateLoadBalancerFactory() {
        log.info("=== LoadBalancerFactory 示例 ===");
        
        // 1. 获取所有支持的负载均衡算法
        Set<String> supportedAlgorithms = LoadBalancerFactory.getSupportedAlgorithms();
        log.info("支持的负载均衡算法: {}", supportedAlgorithms);
        
        // 2. 检查特定算法是否支持
        String[] testAlgorithms = {"random", "round_robin", "consistent_hash", "lru", "lfu", "unknown"};
        for (String algorithm : testAlgorithms) {
            boolean supported = LoadBalancerFactory.isSupported(algorithm);
            log.info("算法 '{}' 是否支持: {}", algorithm, supported);
        }
        
        // 3. 动态创建负载均衡器
        for (String algorithm : supportedAlgorithms) {
            try {
                LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(algorithm);
                if (loadBalancer != null) {
                    log.info("成功创建负载均衡器: {} -> {}", algorithm, loadBalancer.getClass().getSimpleName());
                } else {
                    log.warn("创建负载均衡器失败: {}", algorithm);
                }
            } catch (Exception e) {
                log.error("创建负载均衡器异常: {}", algorithm, e);
            }
        }
        
        // 4. 获取负载均衡器信息
        Map<String, String> loadBalancerInfo = LoadBalancerFactory.getLoadBalancerInfo();
        log.info("负载均衡器详细信息:");
        loadBalancerInfo.forEach((algorithm, className) -> 
            log.info("  {} -> {}", algorithm, className));
    }
    
    /**
     * 演示RpcClient的SPI支持
     */
    private static void demonstrateRpcClientSPI() {
        log.info("\n=== RpcClient SPI 示例 ===");
        
        // 模拟服务注册中心（实际使用时需要真实的注册中心）
        ServiceRegistry serviceRegistry = createMockServiceRegistry();
        
        // 1. 使用字符串算法名称创建RpcClient
        String[] algorithms = {"random", "round_robin", "lru", "lfu", "consistent_hash"};
        
        for (String algorithm : algorithms) {
            try {
                log.info("创建使用 '{}' 算法的RpcClient", algorithm);
                RpcClient rpcClient = new RpcClient(serviceRegistry, algorithm);
                
                // 获取当前负载均衡算法
                String currentAlgorithm = rpcClient.getCurrentLoadBalancerAlgorithm();
                log.info("当前负载均衡算法: {}", currentAlgorithm);
                
                // 关闭客户端
                rpcClient.close();
                
            } catch (Exception e) {
                log.error("创建RpcClient失败: {}", algorithm, e);
            }
        }
        
        // 2. 演示动态切换负载均衡算法
        try {
            log.info("\n演示动态切换负载均衡算法:");
            RpcClient rpcClient = new RpcClient(serviceRegistry, "random");
            log.info("初始算法: {}", rpcClient.getCurrentLoadBalancerAlgorithm());
            
            // 动态切换算法
            String[] switchAlgorithms = {"round_robin", "lru", "consistent_hash"};
            for (String algorithm : switchAlgorithms) {
                rpcClient.setLoadBalancer(algorithm);
                log.info("切换后算法: {}", rpcClient.getCurrentLoadBalancerAlgorithm());
            }
            
            // 获取所有支持的算法
            List<String> supportedAlgorithms = RpcClient.getSupportedLoadBalancerAlgorithms();
            log.info("RpcClient支持的算法: {}", supportedAlgorithms);
            
            rpcClient.close();
            
        } catch (Exception e) {
            log.error("动态切换算法示例失败", e);
        }
        
        // 3. 演示错误处理
        try {
            log.info("\n演示错误处理:");
            RpcClient rpcClient = new RpcClient(serviceRegistry, "unknown_algorithm");
            log.info("使用未知算法创建的客户端算法: {}", rpcClient.getCurrentLoadBalancerAlgorithm());
            rpcClient.close();
        } catch (Exception e) {
            log.info("预期的错误: {}", e.getMessage());
        }
    }
    
    /**
     * 创建模拟的服务注册中心
     * 实际使用时应该使用真实的注册中心实现
     */
    private static ServiceRegistry createMockServiceRegistry() {
        // 这里返回null，实际使用时需要创建真实的服务注册中心
        // 例如: return new ZookeeperServiceRegistry("localhost:2181");
        return new MockServiceRegistry();
    }
    
    /**
     * 模拟服务注册中心实现
     */
    private static class MockServiceRegistry implements ServiceRegistry {
        @Override
        public void register(com.rpc.core.serviceinfo.ServiceInfo serviceInfo) {
            // 模拟实现
        }
        
        @Override
        public void unregister(com.rpc.core.serviceinfo.ServiceInfo serviceInfo) {
            // 模拟实现
        }
        
        @Override
        public java.util.List<com.rpc.core.serviceinfo.ServiceInfo> discover(String serviceName) {
            // 模拟实现
            return java.util.Collections.emptyList();
        }
        
        @Override
        public java.util.List<com.rpc.core.serviceinfo.ServiceInfo> discover(String serviceName, String version, String group) {
            // 模拟实现
            return java.util.Collections.emptyList();
        }
        
        @Override
        public void subscribe(String serviceName, ServiceChangeListener listener) {
            // 模拟实现
        }
        
        @Override
        public void unsubscribe(String serviceName, ServiceChangeListener listener) {
            // 模拟实现
        }
        
        @Override
        public java.util.List<String> getAllServiceNames() {
            // 模拟实现
            return java.util.Collections.emptyList();
        }
        
        @Override
        public boolean exists(String serviceName) {
            // 模拟实现
            return false;
        }
        
        @Override
        public int getServiceInstanceCount(String serviceName) {
            // 模拟实现
            return 0;
        }
        
        @Override
        public void destroy() {
            // 模拟实现
        }
        
        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}