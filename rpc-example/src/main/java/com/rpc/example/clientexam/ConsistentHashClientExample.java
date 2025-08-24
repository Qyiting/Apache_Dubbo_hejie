package com.rpc.example.clientexam;

import com.rpc.client.*;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.client.loadbalance.hash.ConsistentHashLoadBalancer;
import com.rpc.example.entity.User;
import com.rpc.example.service.UserService;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 一致性哈希负载均衡器示例
 * 演示如何使用一致性哈希算法进行负载均衡
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ConsistentHashClientExample {
    
    public static void main(String[] args) {
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if (args.length >= 1) {
            registryAddress = args[0];
        }
        
        log.info("启动一致性哈希负载均衡RPC客户端示例");
        log.info("注册中心地址：{}", registryAddress);
        
        RpcClient rpcClient = null;
        ServiceRegistry serviceRegistry = null;
        
        try {
            // 1. 初始化序列化器
            SerializerFactory.setDefaultSerializerType(Serializer.SerializerType.PROTOSTUFF);
            byte serializationType = SerializerFactory.getDefaultSerializer().getType();
            log.info("序列化器初始化完成：{}", SerializerFactory.getDefaultSerializer().getName());
            
            // 2. 创建服务注册中心
            serviceRegistry = new ZookeeperServiceRegistry(registryAddress);
            log.info("服务注册中心创建完成");
            
            // 3. 创建一致性哈希负载均衡器
            LoadBalancer consistentHashLoadBalancer = new ConsistentHashLoadBalancer();
            log.info("一致性哈希负载均衡器创建完成：{}", consistentHashLoadBalancer.getAlgorithm());
            
            // 4. 创建RPC客户端
            rpcClient = new RpcClient(serviceRegistry, consistentHashLoadBalancer, 15000, 20);
            log.info("RPC客户端创建完成");
            
            // 5. 演示一致性哈希负载均衡
            demonstrateConsistentHashLoadBalancing(rpcClient, serializationType);
            
            // 6. 演示并发调用下的一致性
//            demonstrateConcurrentConsistency(rpcClient, serializationType);
            
            log.info("一致性哈希负载均衡示例执行完成");
            
        } catch (Exception e) {
            log.error("执行一致性哈希负载均衡示例时发生异常", e);
        } finally {
            // 清理资源
            cleanup(rpcClient, serviceRegistry);
        }
    }
    
    /**
     * 演示一致性哈希负载均衡
     */
    private static void demonstrateConsistentHashLoadBalancing(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示一致性哈希负载均衡 ===");
        
        try {
            // 创建代理对象
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            
            // 多次调用相同的请求，验证一致性
            log.info("多次调用相同请求，验证请求一致性：");
            for (int i = 0; i < 5; i++) {
                User user = userService.getUserById(1L);
                log.info("第{}次调用结果：{}", i + 1, user != null ? user.getUsername() : "null");
                Thread.sleep(100); // 短暂延迟
            }
            
            // 调用不同的请求，观察负载分布
            log.info("\n调用不同请求，观察负载分布：");
            for (long userId = 1L; userId <= 10L; userId++) {
                User user = userService.getUserById(userId);
                log.info("查询用户ID={}，结果：{}", userId, user != null ? user.getUsername() : "null");
                Thread.sleep(50);
            }
            
        } catch (Exception e) {
            log.error("演示一致性哈希负载均衡时发生异常", e);
        }
    }
    
    /**
     * 演示并发调用下的一致性
     */
    private static void demonstrateConcurrentConsistency(RpcClient rpcClient, byte serializationType) {
        log.info("\n=== 演示并发调用下的一致性 ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        try {
            // 创建代理对象
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            
            // 并发调用相同的请求
            log.info("启动10个线程并发调用相同请求：");
            for (int i = 0; i < 10; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 3; j++) {
                            User user = userService.getUserById(1L);
                            log.info("线程{}第{}次调用结果：{}", threadId, j + 1, 
                                   user != null ? user.getUsername() : "null");
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        log.error("线程{}执行异常", threadId, e);
                    }
                });
            }
            
            // 等待所有任务完成
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("并发测试超时，强制关闭线程池");
                executor.shutdownNow();
            }
            
        } catch (Exception e) {
            log.error("演示并发一致性时发生异常", e);
        }
    }
    
    /**
     * 清理资源
     */
    private static void cleanup(RpcClient rpcClient, ServiceRegistry serviceRegistry) {
        log.info("开始清理资源...");
        
        if (rpcClient != null) {
            try {
                rpcClient.close();
                log.info("RPC客户端已关闭");
            } catch (Exception e) {
                log.warn("关闭RPC客户端时发生异常", e);
            }
        }
        
        if (serviceRegistry != null) {
            try {
                serviceRegistry.destroy();
                log.info("服务注册中心已关闭");
            } catch (Exception e) {
                log.warn("关闭服务注册中心时发生异常", e);
            }
        }
        
        log.info("资源清理完成");
    }
}