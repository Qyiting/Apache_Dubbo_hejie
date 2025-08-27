package com.rpc.example.clientexam;

import com.rpc.client.*;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.client.loadbalance.LoadBalancerFactory;
import com.rpc.client.loadbalance.lfu.LFULoadBalancer;
import com.rpc.client.loadbalance.random.RandomLoadBalancer;
import com.rpc.client.loadbalance.robin.RoundRobinLoadBalancer;
import com.rpc.core.metric.MetricsCollector;
import com.rpc.example.entity.User;
import com.rpc.example.service.AsyncUserService;
import com.rpc.example.service.UserService;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ClientExample {
    public static void main(String[] args) {
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if(args.length >= 1) {
            registryAddress = args[0];
        }
        log.info("启动RPC客户端实例");
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
            // 3. 创建负载均衡器
            // 可选择的负载均衡算法：
            // - RoundRobinLoadBalancer: 轮询负载均衡
            // - RandomLoadBalancer: 随机负载均衡  
            // - ConsistentHashLoadBalancer: 一致性哈希负载均衡
            // - LRULoadBalancer: LRU（最近最少使用）负载均衡
//            LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
            // LoadBalancer loadBalancer = new RandomLoadBalancer();
            // LoadBalancer loadBalancer = new ConsistentHashLoadBalancer();
            // LoadBalancer loadBalancer = new LRULoadBalancer(10); // 缓存大小为10
            LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(LoadBalancer.Algorithm.CONSISTENT_HASH);
            log.info("负载均衡器创建完成：{}", loadBalancer.getAlgorithm());
            // 4. 创建RPC客户端，增加最大连接数和超时时间
            rpcClient = new RpcClient(serviceRegistry, loadBalancer, 15000, 20);
            log.info("RPC客户端创建完成");
            // 5. 演示同步调用
            demonstrateSyncCalls(rpcClient, serializationType);
            // 6. 演示异步调用
//            demonstrateAsyncCalls(rpcClient, serializationType);
            // 7. 演示并发调用
//            demonstrateConcurrentCalls(rpcClient);
            // 8. 演示不同版本和分组的服务调用
//            demonstrateVersionAndGroupCalls(rpcClient);
            // 9. 演示负载均衡
//            demonstrateLoadBalancing(rpcClient);
            // 10. 演示新功能：监控指标、健康检查、重试策略
//            demonstrateNewFeatures(rpcClient);
            log.info("所有示例执行完成");
        } catch (Exception e) {
            log.error("执行客户端示例时发生异常", e);
        } finally {
            // 清理资源
            cleanup(rpcClient, serviceRegistry);
        }
    }

    /**
     * 演示同步调用
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateSyncCalls(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示同步调用 ===");
        try {
            // 创建代理对象（自动序列化协商）
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class);
            log.info("已创建用户服务代理，将自动从服务发现获取序列化类型");
            // 1. 查询用户
            log.info("1. 查询用户ID=1");
            User user = userService.getUserById(1L);
            if(user != null) {
                log.info("查询结果：{}", user);
            } else {
                log.warn("未找到用户");
            }
            // 2. 根据用户名查询
            log.info("2. 根据用户名查询：admin");
            User adminUser = userService.getUserByUsername("admin");
            if(adminUser != null) {
                log.info("查询结果：{}", adminUser);
            }
            // 3. 创建新用户
            log.info("3. 创建新用户");
            User newUser = User.builder().username("textuser").password("test123")
                    .email("test@example.com").phone("13800138888")
                    .age(25).gender("男").address("深圳市南山区")
                    .status("ACTIVE").build();
            Long newUserId = userService.createUser(newUser);
            if(newUserId != null) {
                log.info("用户创建成功，ID：{}", newUserId);
            } else {
                log.warn("用户创建失败");
            }
            // 4. 获取所有用户
            log.info("4. 获取所有用户");
            List<User> allUsers = userService.getAllUsers();
            log.info("用户总数：{}", allUsers.size());
            for(User u: allUsers) {
                log.info(" - {}：{}", u.getId(), u.getUsername());
            }
            // 5. 根据年龄范围查询
            log.info("5. 查询年龄在20-30之间的用户");
            List<User> usersInAgeRange = userService.getUsersByAgeRange(20, 30);
            log.info("符合条件的用户数：{}", usersInAgeRange.size());
            // 6. 检查用户名是否存在
            log.info("6. 检查用户名是否存在");
            boolean exists = userService.existsByUsername("admin");
            log.info("用户名'admin'是否存在：{}", exists);
            // 7. 获取用户总数
            log.info("7. 获取用户总数");
            long userCount = userService.getUserCount();
            log.info("用户总数：{}", userCount);
        } catch (Exception e) {
            log.error("同步调用时发生异常", e);
        }
    }

    /**
     * 演示异步调用
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateAsyncCalls(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示异步调用 ===");
        try {
            // 创建异步代理对象
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            
            // 使用专门的AsyncUserService接口进行异步调用
            AsyncUserService asyncUserService = proxyFactory.createAsyncProxy(AsyncUserService.class, serializationType);
            
            // 1. 异步查询用户
            log.info("1. 异步查询用户ID=1");
            CompletableFuture<User> userFuture = asyncUserService.getUserById(1L);
            userFuture.thenAccept(user -> log.info("异步查询用户结果：{}", user))
                     .exceptionally(ex -> {
                         log.error("异步查询用户失败", ex);
                         return null;
                     });
            
            // 2. 异步获取所有用户
            log.info("2. 异步获取所有用户");
            CompletableFuture<List<User>> allUsersFuture = asyncUserService.getAllUsers();
            allUsersFuture.thenAccept(allUsers -> log.info("异步获取所有用户结果，数量：{}", allUsers.size()))
                          .exceptionally(ex -> {
                              log.error("异步获取所有用户失败", ex);
                              return null;
                          });
            
            // 等待异步操作完成（进一步增加超时时间）
            CompletableFuture.allOf(userFuture, allUsersFuture).get(30, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("异步调用时发生异常", e);
        }
    }

    /**
     * 演示并发调用
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateConcurrentCalls(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示并发调用 ===");
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            // 启动多个并发任务，但控制数量避免连接池耗尽
            for(int i = 0; i < 10; i++) {
                final int taskId = i;
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        // 随机调用不同的方法
                        if (taskId % 3 == 0) {
                            User user = userService.getUserById((long) (taskId % 3 + 1));
                            log.info("并发任务{} - 查询用户：{}", taskId, user != null ? user.getUsername() : "null");
                        } else if (taskId % 3 == 1) {
                            long count = userService.getUserCount();
                            log.info("并发任务数{} - 用户总数：{}", taskId, count);
                        } else {
                            List<User> users = userService.getUsersByAgeRange(20, 40);
                            log.info("并发任务{} - 年龄范围查询结果数：{}", taskId, users.size());
                        }
                        // 添加小延迟避免瞬时并发过高
                        Thread.sleep(100);
                    } catch (Exception e) {
                        log.error("并发任务{}执行异常", taskId, e);
                    }
                }, executor);
                futures.add(future);
            }
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            log.info("所有并发任务执行完成");
        } catch (Exception e) {
            log.error("并发调用演示时发生异常", e);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * 演示不同版本和分组的服务调用
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateVersionAndGroupCalls(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示版本和分组调用 ===");
        try {
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            // 1. 调用默认版本和分组的服务
            log.info("1. 调用默认版本和分组的服务");
            UserService defaultService = proxyFactory.createProxy(UserService.class, serializationType);
            long count1 = defaultService.getUserCount();
            log.info("默认服务用户数：{}", count1);
            // 2. 调用指定版本的服务
            log.info("2. 调用版本2.0.0的服务");
            UserService v2Service = proxyFactory.createProxy(UserService.class, "2.0.0", "default", serializationType);
            long count2 = v2Service.getUserCount();
            log.info("v2.0.0服务用户数：{}", count2);
            // 3. 调用指定分组的服务
            log.info("3. 调用测试分组的服务");
            UserService testGroupService = proxyFactory.createProxy(UserService.class, "1.0.0", "test", serializationType);
            long count3 = testGroupService.getUserCount();
            log.info("测试分组服务用户数：{}", count3);
        } catch (Exception e) {
            log.error("版本和分组调用演示时发生异常", e);
        }
    }

    /**
     * 演示负载均衡
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateLoadBalancing(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示负载均衡 ===");
        try {
            // 1. 使用轮询负载均衡
            rpcClient.setLoadBalancer(new RoundRobinLoadBalancer());
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, serializationType);
            for(int i = 0; i < 5; i++) {
                long count = userService.getUserCount();
                log.info("轮询调用{} - 用户数：{}", i+1, count);
                Thread.sleep(100);
            }
            // 2. 使用随机负载均衡
            log.info("2. 使用随机负载均衡");
            rpcClient.setLoadBalancer(new RandomLoadBalancer());
            for(int i = 0; i < 5; i++) {
                long count = userService.getUserCount();
                log.info("随机调用{} - 用户数：{}", i+1, count);
                Thread.sleep(100);
            }
        } catch (Exception e) {
            log.error("负载均衡演示时发生异常", e);
        }
    }

    /**
     * 演示新功能：监控指标、健康检查、重试策略
     *
     * @param rpcClient RPC客户端
     */
    private static void demonstrateNewFeatures(RpcClient rpcClient) {
        log.info("=== 演示新功能 ===");
        try {
            // 1. 展示监控指标
            log.info("1. 监控指标统计：");
            MetricsCollector.MetricsSummary metricsSummary = rpcClient.getMetricsSummary();
            log.info("服务发现次数: {}", metricsSummary.getServiceDiscoveryCount());
            log.info("RPC请求总数: {}", metricsSummary.getTotalRequestCount());
            log.info("RPC请求成功数: {}", metricsSummary.getSuccessRequestCount());
            log.info("RPC请求失败数: {}", metricsSummary.getFailedRequestCount());
            log.info("连接创建数: {}", metricsSummary.getConnectionCreatedCount());
            log.info("连接关闭数: {}", metricsSummary.getConnectionClosedCount());
            log.info("活跃连接数: {}", metricsSummary.getActiveConnectionCount());
            log.info("健康检查成功数: {}", metricsSummary.getHealthCheckSuccessCount());
            log.info("健康检查失败数: {}", metricsSummary.getHealthCheckFailedCount());
            if (metricsSummary.getTotalRequestCount() > 0) {
                log.info("请求成功率: {}%", String.format("%.2f", metricsSummary.getRequestSuccessRate() * 100));
            }
            // 2. 展示健康检查状态
            log.info("\n2. 连接健康检查状态：");
            RpcClient.HealthCheckerStatus healthStatus = rpcClient.getHealthCheckerStatus();
            log.info("健康检查器运行状态: {}", healthStatus.isRunning() ? "运行中" : "已停止");
            log.info("检查间隔: {}ms", healthStatus.getCheckInterval());
            log.info("连接超时阈值: {}ms", healthStatus.getConnectionTimeout());
            log.info("最大重试次数: {}", healthStatus.getMaxRetries());
            log.info("不健康连接数: {}", healthStatus.getUnhealthyConnectionCount());
            // 3. 演示重试策略（通过故意调用不存在的服务来触发重试）
            log.info("\n3. 重试策略演示：");
            log.info("尝试调用不存在的服务以演示重试机制...");
            try {
                RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
                // 创建一个不存在的服务代理来触发重试
                UserService nonExistentService = proxyFactory.createProxy(UserService.class, "999.0.0", "nonexistent", (byte) 1);
                nonExistentService.getUserById(1L);
            } catch (Exception e) {
                log.info("重试后仍然失败（这是预期的）: {}", e.getMessage());
            }
            // 4. 再次展示更新后的监控指标
            log.info("\n4. 重试后的监控指标：");
            metricsSummary = rpcClient.getMetricsSummary();
            log.info("RPC请求总数: {}", metricsSummary.getTotalRequestCount());
            log.info("RPC请求失败数: {}", metricsSummary.getFailedRequestCount());
        } catch (Exception e) {
            log.error("演示新功能时发生异常", e);
        }
    }
    
    /**
     * 清理资源
     *
     * @param rpcClient RPC客户端
     * @param serviceRegistry 服务注册中心
     */
    private static void cleanup(RpcClient rpcClient, ServiceRegistry serviceRegistry) {
        log.info("正在清理资源...");
        try {
            // 关闭RPC客户端
            if(rpcClient != null) {
                rpcClient.close();
                log.info("RPC客户端已关闭");
            }
            // 关闭服务注册中心
            if(serviceRegistry != null) {
                serviceRegistry.destroy();
                log.info("服务注册中心已关闭");
            }
            // 清理序列化器资源
            SerializerFactory.cleanup();
            log.info("序列化器资源已清理");
        } catch (Exception e) {
            log.error("清理资源时发生异常", e);
        }
        log.info("资源清理完成");
    }

    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("RPC客户端示例使用说明:");
        System.out.println("java -cp rpc-example.jar com.rpc.example.ClientExam.ClientExample [registry_address]");
        System.out.println("参数说明:");
        System.out.println("  registry_address - 注册中心地址 (默认: localhost:2181)");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp rpc-example.jar com.rpc.example.ClientExam.ClientExample");
        System.out.println("  java -cp rpc-example.jar com.rpc.example.ClientExam.ClientExample 192.168.1.200:2181");
    }
}
