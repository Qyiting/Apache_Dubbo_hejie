package com.hejiexmu.rpc.samples.provider.service.impl;

import com.hejiexmu.rpc.samples.provider.entity.User;
import com.hejiexmu.rpc.samples.provider.service.UserService;
import com.hejiexmu.rpc.spring.boot.annotation.RpcService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 用户服务实现类 - Spring Boot版本
 * 使用@RpcService注解自动注册为RPC服务
 * 
 * @author hejiexmu
 */
@Slf4j
@Service
@RpcService(interfaceClass = UserService.class, version = "1.0.0", group = "default", weight = 100)
public class UserServiceImpl implements UserService {
    
    /** 用户数据存储 */
    private final ConcurrentMap<Long, User> userStorage = new ConcurrentHashMap<>();
    
    /** 用户名索引 */
    private final ConcurrentMap<String, Long> usernameIndex = new ConcurrentHashMap<>();
    
    /** ID生成器 */
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @PostConstruct
    public void initTestData() {
        log.info("初始化测试数据...");
        
        // 创建测试用户
        User user1 = User.builder()
                .id(1L)
                .username("alice")
                .password("password123")
                .email("alice@example.com")
                .phone("13800138001")
                .age(25)
                .gender("女")
                .address("北京市朝阳区")
                .status("ACTIVE")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
                
        User user2 = User.builder()
                .id(2L)
                .username("bob")
                .password("password456")
                .email("bob@example.com")
                .phone("13800138002")
                .age(30)
                .gender("男")
                .address("上海市浦东新区")
                .status("ACTIVE")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
                
        User user3 = User.builder()
                .id(3L)
                .username("charlie")
                .password("password789")
                .email("charlie@example.com")
                .phone("13800138003")
                .age(28)
                .gender("男")
                .address("广州市天河区")
                .status("ACTIVE")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        
        // 存储用户数据
        userStorage.put(1L, user1);
        userStorage.put(2L, user2);
        userStorage.put(3L, user3);
        
        // 建立用户名索引
        usernameIndex.put("alice", 1L);
        usernameIndex.put("bob", 2L);
        usernameIndex.put("charlie", 3L);
        
        // 设置ID生成器起始值
        idGenerator.set(4L);
        
        log.info("测试数据初始化完成，共{}个用户", userStorage.size());
    }
    
    @Override
    public User getUserById(Long userId) {
        log.info("根据ID获取用户：{}", userId);
        User user = userStorage.get(userId);
        if (user != null) {
            log.info("找到用户：{}", user.getUsername());
        } else {
            log.warn("用户不存在：{}", userId);
        }
        return user;
    }
    
    @Override
    public User getUserByUsername(String username) {
        log.info("根据用户名获取用户：{}", username);
        Long userId = usernameIndex.get(username);
        if (userId != null) {
            return userStorage.get(userId);
        }
        log.warn("用户不存在：{}", username);
        return null;
    }
    
    @Override
    public Long createUser(User user) {
        log.info("创建用户：{}", user.getUsername());
        
        // 检查用户名是否已存在
        if (usernameIndex.containsKey(user.getUsername())) {
            log.warn("用户名已存在：{}", user.getUsername());
            throw new RuntimeException("用户名已存在：" + user.getUsername());
        }
        
        // 生成用户ID
        Long userId = idGenerator.getAndIncrement();
        user.setId(userId);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }
        
        // 存储用户
        userStorage.put(userId, user);
        usernameIndex.put(user.getUsername(), userId);
        
        log.info("用户创建成功：ID={}, 用户名={}", userId, user.getUsername());
        return userId;
    }
    
    @Override
    public boolean updateUser(User user) {
        log.info("更新用户：ID={}, 用户名={}", user.getId(), user.getUsername());
        
        if (user.getId() == null || !userStorage.containsKey(user.getId())) {
            log.warn("用户不存在：{}", user.getId());
            return false;
        }
        
        User existingUser = userStorage.get(user.getId());
        
        // 如果用户名发生变化，需要更新索引
        if (!existingUser.getUsername().equals(user.getUsername())) {
            // 检查新用户名是否已被其他用户使用
            Long existingUserId = usernameIndex.get(user.getUsername());
            if (existingUserId != null && !existingUserId.equals(user.getId())) {
                log.warn("用户名已被其他用户使用：{}", user.getUsername());
                throw new RuntimeException("用户名已被其他用户使用：" + user.getUsername());
            }
            
            // 更新用户名索引
            usernameIndex.remove(existingUser.getUsername());
            usernameIndex.put(user.getUsername(), user.getId());
        }
        
        // 更新时间戳
        user.setUpdateTime(LocalDateTime.now());
        
        // 更新用户数据
        userStorage.put(user.getId(), user);
        
        log.info("用户更新成功：ID={}, 用户名={}", user.getId(), user.getUsername());
        return true;
    }
    
    @Override
    public boolean deleteUser(Long userId) {
        log.info("删除用户：{}", userId);
        
        User user = userStorage.get(userId);
        if (user == null) {
            log.warn("用户不存在：{}", userId);
            return false;
        }
        
        // 删除用户数据和索引
        userStorage.remove(userId);
        usernameIndex.remove(user.getUsername());
        
        log.info("用户删除成功：ID={}, 用户名={}", userId, user.getUsername());
        return true;
    }
    
    @Override
    public List<User> getAllUsers() {
        log.info("获取所有用户，当前用户数：{}", userStorage.size());
        return userStorage.values().stream()
                .sorted((u1, u2) -> u1.getId().compareTo(u2.getId()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> getUsersByAgeRange(int minAge, int maxAge) {
        log.info("根据年龄范围获取用户：{}-{}", minAge, maxAge);
        List<User> result = userStorage.values().stream()
                .filter(user -> user.getAge() != null && 
                               user.getAge() >= minAge && 
                               user.getAge() <= maxAge)
                .sorted((u1, u2) -> u1.getId().compareTo(u2.getId()))
                .collect(Collectors.toList());
        log.info("找到{}个符合条件的用户", result.size());
        return result;
    }
    
    @Override
    public boolean existsByUsername(String username) {
        boolean exists = usernameIndex.containsKey(username);
        log.info("检查用户名是否存在：{} -> {}", username, exists);
        return exists;
    }
    
    @Override
    public long getUserCount() {
        long count = userStorage.size();
        log.info("获取用户总数：{}", count);
        return count;
    }
    
    @Override
    public int batchCreateUsers(List<User> users) {
        log.info("批量创建用户，数量：{}", users.size());
        
        int successCount = 0;
        for (User user : users) {
            try {
                createUser(user);
                successCount++;
            } catch (Exception e) {
                log.warn("创建用户失败：{}, 错误：{}", user.getUsername(), e.getMessage());
            }
        }
        
        log.info("批量创建用户完成，成功：{}/{}", successCount, users.size());
        return successCount;
    }
}