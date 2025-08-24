package com.rpc.example.service;

import com.rpc.example.entity.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步用户服务接口
 * 所有方法都返回CompletableFuture包装的结果
 * 
 * @author 何杰
 * @version 1.0
 */
public interface AsyncUserService {
    /**
     * 根据用户ID获取用户信息（异步）
     *
     * @param userId 用户ID
     * @return 包含用户信息的CompletableFuture
     */
    CompletableFuture<User> getUserById(Long userId);

    /**
     * 根据用户名获取用户信息（异步）
     *
     * @param username 用户名
     * @return 包含用户信息的CompletableFuture
     */
    CompletableFuture<User> getUserByUsername(String username);
    
    /**
     * 创建用户（异步）
     *
     * @param user 用户信息
     * @return 包含创建的用户ID的CompletableFuture
     */
    CompletableFuture<Long> createUser(User user);

    /**
     * 更新用户信息（异步）
     *
     * @param user 用户信息
     * @return 包含是否更新成功的CompletableFuture
     */
    CompletableFuture<Boolean> updateUser(User user);

    /**
     * 删除用户（异步）
     *
     * @param userId 用户ID
     * @return 包含是否删除成功的CompletableFuture
     */
    CompletableFuture<Boolean> deleteUser(Long userId);
    
    /**
     * 获取所有用户列表（异步）
     *
     * @return 包含用户列表的CompletableFuture
     */
    CompletableFuture<List<User>> getAllUsers();

    /**
     * 根据年龄范围查询用户（异步）
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 包含用户列表的CompletableFuture
     */
    CompletableFuture<List<User>> getUsersByAgeRange(int minAge, int maxAge);
    
    /**
     * 检查用户名是否存在（异步）
     *
     * @param username 用户名
     * @return 包含是否存在的CompletableFuture
     */
    CompletableFuture<Boolean> existsByUsername(String username);

    /**
     * 获取用户总数（异步）
     *
     * @return 包含用户总数的CompletableFuture
     */
    CompletableFuture<Long> getUserCount();

    /**
     * 批量创建用户（异步）
     *
     * @param users 用户列表
     * @return 包含创建成功的用户数量的CompletableFuture
     */
    CompletableFuture<Integer> batchCreateUsers(List<User> users);
}