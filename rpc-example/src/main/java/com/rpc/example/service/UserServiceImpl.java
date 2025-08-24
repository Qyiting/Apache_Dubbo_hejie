package com.rpc.example.service;

import com.rpc.example.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class UserServiceImpl implements UserService{
    /** 用户数据存储（模拟数据库） */
    private final Map<Long, User> userStorage = new ConcurrentHashMap<>();
    /** 用户名索引 */
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    /** ID生成器 */
    private final AtomicLong idGenerator = new AtomicLong(1);
    /**
     * 构造函数，初始化一些测试数据
     */
    public UserServiceImpl() {
        initTestData();
        log.info("用户服务已初始化，当前用户数量：{}", userStorage.size());
    }

    /**
     * 初始化测试数据
     */
    private void initTestData() {
        // 创建一些测试用户
        User user1 = User.builder()
                .id(1L)
                .username("admin")
                .password("admin123")
                .email("admin@example.com")
                .phone("13800138001")
                .age(30)
                .gender("男")
                .address("北京市朝阳区")
                .status("ACTIVE")
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user1")
                .password("user123")
                .email("user1@example.com")
                .phone("13800138002")
                .age(25)
                .gender("女")
                .address("上海市浦东新区")
                .status("ACTIVE")
                .build();

        User user3 = User.builder()
                .id(3L)
                .username("user2")
                .password("user123")
                .email("user2@example.com")
                .phone("13800138003")
                .age(28)
                .gender("男")
                .address("广州市天河区")
                .status("INACTIVE")
                .build();

        // 存储用户数据
        userStorage.put(user1.getId(), user1);
        userStorage.put(user2.getId(), user2);
        userStorage.put(user3.getId(), user3);

        // 建立用户名索引
        usernameIndex.put(user1.getUsername(), user1.getId());
        usernameIndex.put(user2.getUsername(), user2.getId());
        usernameIndex.put(user3.getUsername(), user3.getId());

        // 更新ID生成器
        idGenerator.set(4L);
    }

    @Override
    public User getUserById(Long userId) {
        log.debug("根据ID获取用户：{}", userId);
        if(userId == null) {
            log.warn("用户ID不能为null");
            return null;
        }
        User user = userStorage.get(userId);
        if(user != null) {
            log.debug("找到用户：{}", user.getUsername());
        } else {
            log.debug("未找到ID为{}的用户", userId);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        log.debug("根据用户名获取用户：{}", username);
        if(username == null) {
            log.warn("用户名不能为空");
            return null;
        }
        Long userId = usernameIndex.get(username);
        if(userId != null) {
            User user = userStorage.get(userId);
            log.debug("找到用户：{}", user != null?user.getId():"null");
            return user;
        }
        log.debug("未找到用户名为{}的用户", username);
        return null;
    }

    @Override
    public Long createUser(User user) {
        log.debug("创建用户：{}", user != null?user.getUsername():"null");
        if(user == null) {
            log.warn("用户信息不能为空");
            return null;
        }
        if(!user.isValid()) {
            log.warn("用户信息无效：{}", user);
            return null;
        }
        // 检查用户名是否已存在
        if(usernameIndex.containsKey(user.getUsername())) {
            log.warn("用户名已存在：{}", user.getUsername());
            return null;
        }
        // 生成新的用户ID
        long newUserId = idGenerator.getAndIncrement();
        user.setId(newUserId);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 存储用户
        userStorage.put(newUserId, user);
        usernameIndex.put(user.getUsername(), newUserId);
        log.info("用户创建成功：ID={}，用户名={}", newUserId, user.getUsername());
        return newUserId;
    }

    @Override
    public boolean updateUser(User user) {
        log.debug("更新用户：{}", user != null?user.getUsername():"null");
        if(user == null || user.getId() == null) {
            log.warn("用户信息或用户ID不能为null");
            return false;
        }
        if(!user.isValid()) {
            log.warn("用户信息无效：{}", user);
            return false;
        }
        User existingUser = userStorage.get(user.getId());
        if(existingUser == null) {
            log.warn("用户不存在：{}", user.getId());
            return false;
        }
        // 检查用户名是否被其他用户使用
        if(!existingUser.getUsername().equals(user.getUsername())) {
            Long existingUserId = usernameIndex.get(user.getUsername());
            if(existingUserId != null && !existingUserId.equals(user.getId())) {
                log.warn("用户名已被其他用户使用：{}", user.getUsername());
                return false;
            }
            // 更新用户名索引
            usernameIndex.remove(existingUser.getUsername());
            usernameIndex.put(user.getUsername(), user.getId());
        }
        // 保留创建时间，更新修改时间
        user.setCreateTime(existingUser.getCreateTime());
        user.setUpdateTime(LocalDateTime.now());
        // 更新用户信息
        userStorage.put(user.getId(), user);
        log.info("用户更新成功：ID={}，用户名={}", user.getId(), user.getUsername());
        return true;
    }

    @Override
    public boolean deleteUser(Long userId) {
        log.debug("删除用户：{}", userId);
        if(userId == null) {
            log.warn("用户ID不能为null");
            return false;
        }
        User user = userStorage.get(userId);
        if(user == null) {
            log.warn("用户不存在：{}", userId);
            return false;
        }
        // 删除用户数据和索引
        userStorage.remove(userId);
        usernameIndex.remove(user.getUsername());
        log.info("用户删除成功：ID={}，用户名={}", userId, user.getUsername());
        return true;
    }

    @Override
    public List<User> getAllUsers() {
        log.debug("获取所有用户列表");
        List<User> users = new ArrayList<>(userStorage.values());
        log.debug("返回用户列表，数量：{}", users.size());
        users.sort(Comparator.comparing(User::getId));
        return users;
    }

    @Override
    public List<User> getUsersByAgeRange(int minAge, int maxAge) {
        log.debug("根据年龄范围查询用户：{}-{}", minAge, maxAge);
        if(minAge < 0 || maxAge < 0 || minAge > maxAge) {
            log.warn("年龄范围参数无效：{}-{}", minAge, maxAge);
            return new ArrayList<>();
        }
        List<User> result = userStorage.values().stream().filter(user ->
                user.getAge() != null && user.getAge() >= minAge && user.getAge() <= maxAge
        ).sorted(Comparator.comparing(User::getAge)).collect(Collectors.toList());
        log.debug("年龄范围{}-{}的用户数量：{}", minAge, maxAge, result.size());
        return result;
    }

    @Override
    public boolean existsByUsername(String username) {
        log.debug("检查用户名是否存在：{}", username);
        if(username == null || username.trim().isEmpty()) {
            return false;
        }
        boolean exists = usernameIndex.containsKey(username);
        log.debug("用户名{}存在性:{}", username, exists);
        return exists;
    }

    @Override
    public long getUserCount() {
        int count = userStorage.size();
        log.debug("用户总数：{}", count);
        return count;
    }

    @Override
    public int batchCreateUsers(List<User> users) {
        log.debug("批量创建用户，数量：{}", users!= null?users.size():0);
        if(users == null || users.isEmpty()) {
            log.warn("用户列表为空");
            return 0;
        }
        int successCount = 0;
        for(User user: users) {
            try {
                Long userId = createUser(user);
                if(userId != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量创建用户时发送异常：{}", user!= null?user.getUsername():"null", e);
            }
        }
        log.info("批量创建用户完成，成功{}，总数：{}", successCount, users.size());
        return successCount;
    }

    /**
     * 获取服务统计信息
     *
     * @return 统计信息
     */
    public String getStatistics() {
        return String.format("用户总数: %d, 活跃用户: %d, 非活跃用户: %d",
                userStorage.size(),
                userStorage.values().stream().mapToInt(user -> user.isActive() ? 1 : 0).sum(),
                userStorage.values().stream().mapToInt(user -> user.isActive() ? 0 : 1).sum());
    }

    /**
     * 清空所有用户数据（仅用于测试）
     */
    public void clearAllUsers() {
        userStorage.clear();
        usernameIndex.clear();
        idGenerator.set(1L);
        log.info("所有用户数据已清空");
    }
}
