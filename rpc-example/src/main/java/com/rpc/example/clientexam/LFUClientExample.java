package com.rpc.example.clientexam;

import com.rpc.client.loadbalance.lfu.LFULoadBalancer;
import com.rpc.client.RpcClient;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.example.entity.User;
import com.rpc.example.service.UserService;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class LFUClientExample {

    public static void main(String[] args) {
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if(args.length >= 1) {
            registryAddress = args[0];
        }
        log.info("启动LFU负载均衡器示例客户端");
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
            // 3. 创建LFU负载均衡器
            // LFU负载均衡器会优先选择使用频率最少的服务示例
            LFULoadBalancer loadBalancer = new LFULoadBalancer(50);// 最多跟踪50个服务实例
            log.info("LFU负载均衡器创建完成，最多跟踪{}个服务实例", loadBalancer.getMaxServices());
            // 4. 创建RPC客户端
            rpcClient = new RpcClient(serviceRegistry, loadBalancer, 15000, 20);
            log.info("RPC客户端创建完成");
            // 5. 演示LFU负载均衡的特性
//            demonstrateLFULoadBalancing(rpcClient, serializationType);
            // 6. 演示并发场景下的LFU负载均衡
//            demonstarteConcurrentLFU(rpcClient, serializationType);
            // 7. 演示服务淘汰机制（在真实RPC调用中）
//            demonstrateServiceEvictionInRealCalls(rpcClient, serializationType, loadBalancer);
            // 8. 演示动态服务管理（在真实RPC调用中）
            demonstrateDynamicServiceManagementInRealCalls(rpcClient, serializationType, loadBalancer);
            log.info("LFU负载均衡器示例执行完成");
        } catch (Exception e) {
            log.error("执行LFU负载均衡器示例时发生异常", e);
        } finally {
            // 清理资源
            cleanup(rpcClient, serviceRegistry);
        }
    }


    /**
     * 演示LFU负载均衡的特性
     * LFU算法会优先选择最少使用过的服务实例
     */
    private static void demonstrateLFULoadBalancing(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示LFU负载均衡特性 ===");
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            // 创建代理对象
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            log.info("开始连续调用服务，观察LFU缓存的工作机制");
            // 连续调用多次，观察LFU频率变化情况
            for(int i = 0; i < 15; i++) {
                try {
                    User user = userService.getUserById((long) i);
                    if(user != null) {
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
            log.info("LFU负载均衡演示完成");
        } catch (Exception e) {
            log.error("演示LFU负载均衡时发生异常", e);
        }
    }

    /**
     * 演示并发场景下的LFU负载均衡
     * 多线程并发访问时，LFU缓存的线程安全性和性能
     */
    private static void demonstarteConcurrentLFU(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示并发场景下的LFU负载均衡 ===");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            // 创建代理对象
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            log.info("启动5个并发线程，每个线程调用10次服务");
            // 启动多个并发任务
            for(int threadId = 1; threadId <= 5; threadId++) {
                final int currentThreadId = threadId;
                executor.submit(() -> {
                    for(int i = 0; i <= 10; i++) {
                        try {
                            User user = userService.getUserById((long) i);
                            if(user != null) {
                                log.info("线程{}第{}次调用成功，用户：{}", currentThreadId, i, user.getUsername());
                            } else {
                                log.info("线程{}第{}次调用成功，但用户不存在", currentThreadId, i);
                            }
                            // 短暂休眠
                            Thread.sleep(100);
                        } catch (Exception e) {
                            log.error("线程{}第{}次调用失败", currentThreadId, i, e);
                        }
                    }
                    log.info("线程{}执行完成", currentThreadId);
                });
            }
            // 等待所有任务完成
            executor.shutdown();
            if(!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("并发任务执行超时，强制关闭线程池");
                executor.shutdown();
            }
            log.info("并发LFU负载均衡演示完成");
        } catch (Exception e) {
            log.error("演示并发LFU负载均衡时发生异常", e);
        } finally {
            if(!executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

    /**
     * 演示服务淘汰机制（在真实RPC调用中）
     * 通过真实的RPC调用来观察LFU算法的服务淘汰行为
     */
    private static void demonstrateServiceEvictionInRealCalls(RpcClient rpcClient, byte serializationType, LFULoadBalancer loadBalancer) {
        log.info("=== 演示服务淘汰机制（真实RPC调用） ===");
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            log.info("开始通过真实RPC调用演示LFU服务淘汰机制");
            log.info("当前LFU负载均衡器最大跟踪服务数：{}", loadBalancer.getMaxServices());
            // 阶段1：建立初始访问模式
            log.info("\n阶段1：建立初始访问模式");
            for (int i = 1; i <= 20; i++) {
                try {
                    User user = userService.getUserById((long) (i % 5 + 1)); // 循环访问用户ID 1-5
                    if (user != null) {
                        log.info("第{}次调用成功，用户：{}", i, user.getUsername());
                    } else {
                        log.info("第{}次调用成功，但用户不存在", i);
                    }
                    
                    // 每5次调用后显示频率统计
                    if (i % 5 == 0) {
                        log.info("第{}次调用后的频率统计：{}", i, loadBalancer.getServiceFrequencies());
                        log.info("当前最小频率：{}", loadBalancer.getMinFrequency());
                    }
                    
                    Thread.sleep(200); // 短暂休眠便于观察
                } catch (Exception e) {
                    log.error("第{}次调用失败", i, e);
                }
            }
            // 阶段2：集中访问特定服务，观察频率变化
            log.info("\n阶段2：集中访问特定用户，观察频率分布变化");
            for (int i = 1; i <= 15; i++) {
                try {
                    // 主要访问用户1和用户2，建立明显的频率差异
                    long userId = (i % 3 == 0) ? 3L : (i % 2 == 0 ? 2L : 1L);
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        log.info("集中访问第{}次，用户ID：{}，用户名：{}", i, userId, user.getUsername());
                    } else {
                        log.info("集中访问第{}次，用户ID：{}，但用户不存在", i, userId);
                    }
                    if (i % 5 == 0) {
                        log.info("集中访问{}次后的频率统计：{}", i, loadBalancer.getServiceFrequencies());
                    }
                    Thread.sleep(150);
                } catch (Exception e) {
                    log.error("集中访问第{}次调用失败", i, e);
                }
            }
            // 阶段3：访问新的服务，触发可能的淘汰
            log.info("\n阶段3：访问更多不同用户，观察服务淘汰行为");
            for (int i = 1; i <= 10; i++) {
                try {
                    // 访问更大范围的用户ID，可能触发新服务实例的选择
                    long userId = (i % 10 + 1); // 用户ID 1-10
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        log.info("扩展访问第{}次，用户ID：{}，用户名：{}", i, userId, user.getUsername());
                    } else {
                        log.info("扩展访问第{}次，用户ID：{}，但用户不存在", i, userId);
                    }
                    // 显示当前状态
                    Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();
                    log.info("当前跟踪服务数量：{}，频率统计：{}", frequencies.size(), frequencies);
                    Thread.sleep(200);
                } catch (Exception e) {
                    log.error("扩展访问第{}次调用失败", i, e);
                }
            }
            // 最终统计
            log.info("\n=== 服务淘汰机制演示总结 ===");
            Map<String, Integer> finalFrequencies = loadBalancer.getServiceFrequencies();
            log.info("最终跟踪的服务数量：{}", finalFrequencies.size());
            log.info("最终频率统计：{}", finalFrequencies);
            log.info("最终最小频率：{}", loadBalancer.getMinFrequency());
            if (!finalFrequencies.isEmpty()) {
                int maxFreq = finalFrequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                int minFreq = loadBalancer.getMinFrequency();
                log.info("频率范围：{} - {}", minFreq, maxFreq);
                log.info("通过真实RPC调用成功演示了LFU算法的服务淘汰机制");
            }
        } catch (Exception e) {
            log.error("演示服务淘汰机制时发生异常", e);
        }
        log.info("服务淘汰机制演示完成\n");
    }
    
    /**
     * 演示动态服务管理（在真实RPC调用中）
     * 通过真实的RPC调用来观察服务列表变化时的负载均衡行为
     */
    private static void demonstrateDynamicServiceManagementInRealCalls(RpcClient rpcClient, byte serializationType, LFULoadBalancer loadBalancer) {
        log.info("=== 演示动态服务管理（真实RPC调用） ===");
        try {
            // 创建代理工厂
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            log.info("开始通过真实RPC调用演示动态服务管理");
            // 阶段1：正常负载均衡阶段
            log.info("\n阶段1：正常负载均衡阶段");
            for (int i = 1; i <= 12; i++) {
                try {
                    User user = userService.getUserById((long) (i % 4 + 1));
                    if (user != null) {
                        log.info("正常阶段第{}次调用，用户：{}", i, user.getUsername());
                    } else {
                        log.info("正常阶段第{}次调用，用户不存在", i);
                    }
                    if (i % 4 == 0) {
                        log.info("正常阶段{}次调用后频率统计：{}", i, loadBalancer.getServiceFrequencies());
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("正常阶段第{}次调用失败", i, e);
                }
            }
            // 阶段2：模拟高频访问，建立明显的使用模式
            log.info("\n阶段2：建立明显的服务使用模式");
            for (int i = 1; i <= 20; i++) {
                try {
                    // 80%的请求访问用户1，20%访问用户2，建立明显的访问模式
                    long userId = (i % 5 == 0) ? 2L : 1L;
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        log.info("模式建立第{}次调用，用户ID：{}，用户名：{}", i, userId, user.getUsername());
                    } else {
                        log.info("模式建立第{}次调用，用户ID：{}，用户不存在", i, userId);
                    }
                    if (i % 5 == 0) {
                        Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();
                        log.info("模式建立{}次后频率统计：{}", i, frequencies);
                        // 分析负载分布
                        if (!frequencies.isEmpty()) {
                            int maxFreq = frequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                            int minFreq = loadBalancer.getMinFrequency();
                            double balance = minFreq > 0 ? (double) minFreq / maxFreq : 0.0;
                            log.info("当前负载均衡度：{}", String.format("%.2f", balance));
                        }
                    }
                    Thread.sleep(80);
                } catch (Exception e) {
                    log.error("模式建立第{}次调用失败", i, e);
                }
            }
            // 阶段3：改变访问模式，观察LFU的自适应
            log.info("\n阶段3：改变访问模式，观察LFU自适应能力");
            for (int i = 1; i <= 15; i++) {
                try {
                    // 现在主要访问之前较少访问的用户
                    long userId = (i % 6 + 3); // 用户ID 3-8
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        log.info("模式转换第{}次调用，用户ID：{}，用户名：{}", i, userId, user.getUsername());
                    } else {
                        log.info("模式转换第{}次调用，用户ID：{}，用户不存在", i, userId);
                    }
                    if (i % 5 == 0) {
                        log.info("模式转换{}次后频率统计：{}", i, loadBalancer.getServiceFrequencies());
                    }
                    Thread.sleep(120);
                } catch (Exception e) {
                    log.error("模式转换第{}次调用失败", i, e);
                }
            }
            // 阶段4：混合访问模式，测试负载均衡效果
            log.info("\n阶段4：混合访问模式，测试最终负载均衡效果");
            for (int i = 1; i <= 10; i++) {
                try {
                    // 随机访问不同用户
                    long userId = (i % 8 + 1);
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        log.info("混合访问第{}次调用，用户ID：{}，用户名：{}", i, userId, user.getUsername());
                    } else {
                        log.info("混合访问第{}次调用，用户ID：{}，用户不存在", i, userId);
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("混合访问第{}次调用失败", i, e);
                }
            }
            // 最终分析
            log.info("\n=== 动态服务管理演示总结 ===");
            Map<String, Integer> finalFrequencies = loadBalancer.getServiceFrequencies();
            log.info("最终跟踪的服务数量：{}", finalFrequencies.size());
            log.info("最终频率统计：{}", finalFrequencies);
            if (!finalFrequencies.isEmpty()) {
                int maxFreq = finalFrequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                int minFreq = loadBalancer.getMinFrequency();
                double finalBalance = minFreq > 0 ? (double) minFreq / maxFreq : 0.0;
                log.info("=== LFU负载均衡效果分析 ===");
                log.info("- 最高访问频率：{}", maxFreq);
                log.info("- 最低访问频率：{}", minFreq);
                log.info("- 最终负载均衡度：{}", String.format("%.2f", finalBalance));
                log.info("- 频率分布范围：{}", maxFreq - minFreq);
                log.info("通过真实RPC调用成功演示了LFU算法的动态适应能力");
            }
        } catch (Exception e) {
            log.error("演示动态服务管理时发生异常", e);
        }
        log.info("动态服务管理演示完成\n");
    }

    /**
     * 清理资源
     */
    private static void cleanup(RpcClient rpcClient, ServiceRegistry serviceRegistry) {
        log.info("开始清理资源");
        try {
            if(rpcClient != null) {
                rpcClient.close();
                log.info("RPC客户端已关闭");
            }
        } catch (Exception e) {
            log.error("关闭RPC客户端时发生异常", e);
        }
        try {
            if(serviceRegistry != null) {
                serviceRegistry.destroy();
                log.info("服务注册中心已销毁");
            }
        } catch (Exception e) {
            log.error("销毁服务注册中心时发生异常", e);
        }
        log.info("资源清理完成");
    }
}
