package com.rpc.example.clientexam;

import com.rpc.client.*;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.client.loadbalance.lru.LRULoadBalancer;
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
 * LRU负载均衡器使用示例
 * 演示如何使用LRU（最近最少使用）负载均衡算法
 * 
 * @author RPC Framework
 * @version 1.0
 */
@Slf4j
public class LRUClientExample {
    
    public static void main(String[] args) {
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if(args.length >= 1) {
            registryAddress = args[0];
        }
        
        log.info("启动LRU负载均衡器示例客户端");
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
            
            // 3. 创建LRU负载均衡器
            // LRU负载均衡器会缓存最近使用的服务实例，优先选择缓存中的实例
            // 当缓存满时，会淘汰最近最少使用的服务实例
            LoadBalancer loadBalancer = new LRULoadBalancer(10); // 缓存大小为10
            log.info("LRU负载均衡器创建完成，缓存大小：10");
            
            // 4. 创建RPC客户端
            rpcClient = new RpcClient(serviceRegistry, loadBalancer, 15000, 20);
            log.info("RPC客户端创建完成");
            
            // 5. 演示LRU负载均衡的特性
//            demonstrateLRULoadBalancing(rpcClient, serializationType);
            
            // 6. 演示并发场景下的LRU负载均衡
            demonstrateConcurrentLRU(rpcClient, serializationType);
            
            log.info("LRU负载均衡器示例执行完成");
            
        } catch (Exception e) {
            log.error("执行LRU负载均衡器示例时发生异常", e);
        } finally {
            // 清理资源
            cleanup(rpcClient, serviceRegistry);
        }
    }
    
    /**
     * 演示LRU负载均衡的特性
     * LRU算法会优先选择最近使用过的服务实例
     */
    private static void demonstrateLRULoadBalancing(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示LRU负载均衡特性 ===");
        
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            // 创建代理对象
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            
            log.info("开始连续调用服务，观察LRU缓存的工作机制");
            
            // 连续调用多次，观察LRU缓存的命中情况
            for (int i = 1; i <= 15; i++) {
                try {
                    User user = userService.getUserById((long) i);
                    if (user != null) {
                        log.info("第{}次调用成功，用户：{}", i, user.getUsername());
                    } else {
                        log.info("第{}次调用成功，但用户不存在", i);
                    }
                    
                    // 短暂休眠，便于观察日志
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("第{}次调用失败", i, e);
                }
            }
            
            log.info("LRU负载均衡演示完成");
            
        } catch (Exception e) {
            log.error("演示LRU负载均衡时发生异常", e);
        }
    }
    
    /**
     * 演示并发场景下的LRU负载均衡
     * 多线程并发访问时，LRU缓存的线程安全性和性能
     */
    private static void demonstrateConcurrentLRU(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示并发场景下的LRU负载均衡 ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            // 创建代理对象
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            
            log.info("启动5个并发线程，每个线程调用10次服务");
            
            // 启动多个并发任务
            for (int threadId = 1; threadId <= 5; threadId++) {
                final int currentThreadId = threadId;
                executor.submit(() -> {
                    for (int i = 0; i <= 10; i++) {
                        try {
                            User user = userService.getUserById((long) i);
                            if (user != null) {
                                log.info("线程{}第{}次调用成功，用户：{}", 
                                    currentThreadId, i, user.getUsername());
                            } else {
                                log.info("线程{}第{}次调用成功，但用户不存在", currentThreadId, i);
                            }
                            
                            // 短暂休眠
                            Thread.sleep(50);
                            
                        } catch (Exception e) {
                            log.error("线程{}第{}次调用失败", currentThreadId, i, e);
                        }
                    }
                    log.info("线程{}执行完成", currentThreadId);
                });
            }
            
            // 等待所有任务完成
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("并发任务执行超时，强制关闭线程池");
                executor.shutdownNow();
            }
            
            log.info("并发LRU负载均衡演示完成");
            
        } catch (Exception e) {
            log.error("演示并发LRU负载均衡时发生异常", e);
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * 清理资源
     */
    private static void cleanup(RpcClient rpcClient, ServiceRegistry serviceRegistry) {
        log.info("开始清理资源");
        
        try {
            if (rpcClient != null) {
                rpcClient.close();
                log.info("RPC客户端已关闭");
            }
        } catch (Exception e) {
            log.error("关闭RPC客户端时发生异常", e);
        }
        
        try {
            if (serviceRegistry != null) {
                serviceRegistry.destroy();
                log.info("服务注册中心已销毁");
            }
        } catch (Exception e) {
            log.error("销毁服务注册中心时发生异常", e);
        }
        
        log.info("资源清理完成");
    }
}