package com.rpc.example.clientexam;

import com.rpc.client.factory.CglibRpcProxyFactory;
import com.rpc.client.loadbalance.random.RandomLoadBalancer;
import com.rpc.client.RpcClient;
import com.rpc.example.entity.User;
import com.rpc.example.service.UserServiceImpl;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author 何杰
 * @version 1.0
 * 
 * 运行前请添加JVM参数：
 * --add-opens java.base/java.lang=ALL-UNNAMED
 * --add-opens java.base/java.lang.reflect=ALL-UNNAMED
 * --add-opens java.base/java.util=ALL-UNNAMED
 */
@Slf4j
public class CglibClientExample {
    public static void main(String[] args) {
        String registryAddress = "192.168.109.103:2181";
        // 解析命令行参数
        if (args.length >= 1) {
            registryAddress = args[0];
        }
        log.info("启动RPC客户端实例");
        log.info("注册中心地址：{}", registryAddress);
        log.info("请确保添加了JVM参数：--add-opens java.base/java.lang=ALL-UNNAMED");
        log.info("Java版本：{}", System.getProperty("java.version"));

        RpcClient rpcClient = null;
        ServiceRegistry serviceRegistry = null;
        try {
            // 1. 初始化序列化器
            SerializerFactory.setDefaultSerializerType(Serializer.SerializerType.KRYO);
            log.info("序列化器初始化完成：{}", SerializerFactory.getDefaultSerializer().getName());
            byte serializationType = SerializerFactory.getDefaultSerializer().getType();
            // 2. 创建服务注册中心
            serviceRegistry = new ZookeeperServiceRegistry(registryAddress);
            log.info("服务注册中心创建完成");
            // 3. 创建负载均衡器
            RandomLoadBalancer loadBalancer = new RandomLoadBalancer();
            log.info("负载均衡器创建完成：{}", loadBalancer.getAlgorithm());
            // 4. 创建RPC客户端，增加最大连接数和超时时间
            rpcClient = new RpcClient(serviceRegistry, loadBalancer, 15000, 20);
            log.info("RPC客户端创建完成");

            // 5. 演示同步调用
            demonstrateSyncCalls(rpcClient, serializationType);
            log.info("所有示例执行完成");
        } catch (ExceptionInInitializerError e) {
            log.error("CGLIB初始化失败", e);
            log.error("请添加JVM参数：--add-opens java.base/java.lang=ALL-UNNAMED");
            log.error("参考：CGLIB_JVM_ARGS.md 文件获取详细解决方案");
        } catch (Exception e) {
            log.error("执行客户端示例时发送异常", e);
        }
    }

    /**
     * 演示同步调用
     *
     * @param rpcClient RPC客户端
     */
    public static void demonstrateSyncCalls(RpcClient rpcClient, byte serializationType) {
        log.info("=== 演示同步调用 ===");
        try {
            // 创建代理对象
            CglibRpcProxyFactory cglibRpcProxyFactory = new CglibRpcProxyFactory(rpcClient);
            UserServiceImpl userService = cglibRpcProxyFactory.createProxy(UserServiceImpl.class, serializationType);
            log.info("CGLIB代理创建成功！代理类名：{}", userService.getClass().getName());

            // 1. 查询用户
            log.info("1. 查询用户ID=1");
            User user = userService.getUserById(1L);
            if (user != null) {
                log.info("查询结果：{}", user);
            } else {
                log.warn("未找到用户");
            }
            // 2. 根据用户名查询
            log.info("2. 根据用户名查询：admin");
            User adminUser = userService.getUserByUsername("admin");
            if (adminUser != null) {
                log.info("查询结果：{}", adminUser);
            }
            // 3. 创建新用户
            log.info("3. 创建新用户");
            User newUser = User.builder().username("textuser").password("test123")
                    .email("test@example.com").phone("13800138888")
                    .age(25).gender("女").address("上海市宝山区")
                    .status("ACTIVE").build();
            Long newUserId = userService.createUser(newUser);
            if (newUserId != null) {
                log.info("用户创建成功，ID：{}", newUserId);
            } else {
                log.warn("用户创建失败");
            }
            // 4. 获取所有用户
            log.info("4. 获取所有用户");
            List<User> allUsers = userService.getAllUsers();
            log.info("用户总数：{}", allUsers.size());
            for (User u : allUsers) {
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
            //7. 获取用户总数
            log.info("7. 获取用户总数");
            long userCount = userService.getUserCount();
            log.info("用户总数：{}", userCount);
        } catch (Exception e) {
            log.error("同步调用时发送异常", e);
        }
    }
}
