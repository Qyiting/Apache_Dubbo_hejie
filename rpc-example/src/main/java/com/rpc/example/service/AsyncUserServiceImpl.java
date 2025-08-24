package com.rpc.example.service;

import com.rpc.example.entity.User;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步用户服务实现类
 * 包装同步UserService，提供异步接口
 * 
 * @author 何杰
 * @version 1.0
 */
public class AsyncUserServiceImpl implements AsyncUserService {
    
    private final UserService userService;
    private final ExecutorService executorService;
    
    public AsyncUserServiceImpl(UserService userService) {
        this.userService = userService;
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @Override
    public CompletableFuture<User> getUserById(Long userId) {
        return CompletableFuture.supplyAsync(() -> userService.getUserById(userId), executorService);
    }

    @Override
    public CompletableFuture<User> getUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> userService.getUserByUsername(username), executorService);
    }

    @Override
    public CompletableFuture<Long> createUser(User user) {
        return CompletableFuture.supplyAsync(() -> userService.createUser(user), executorService);
    }

    @Override
    public CompletableFuture<Boolean> updateUser(User user) {
        return CompletableFuture.supplyAsync(() -> userService.updateUser(user), executorService);
    }

    @Override
    public CompletableFuture<Boolean> deleteUser(Long userId) {
        return CompletableFuture.supplyAsync(() -> userService.deleteUser(userId), executorService);
    }

    @Override
    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> userService.getAllUsers(), executorService);
    }

    @Override
    public CompletableFuture<List<User>> getUsersByAgeRange(int minAge, int maxAge) {
        return CompletableFuture.supplyAsync(() -> userService.getUsersByAgeRange(minAge, maxAge), executorService);
    }

    @Override
    public CompletableFuture<Boolean> existsByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> userService.existsByUsername(username), executorService);
    }

    @Override
    public CompletableFuture<Long> getUserCount() {
        return CompletableFuture.supplyAsync(() -> userService.getUserCount(), executorService);
    }

    @Override
    public CompletableFuture<Integer> batchCreateUsers(List<User> users) {
        return CompletableFuture.supplyAsync(() -> userService.batchCreateUsers(users), executorService);
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}