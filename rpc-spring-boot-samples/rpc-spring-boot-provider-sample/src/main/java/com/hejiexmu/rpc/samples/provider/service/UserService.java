package com.hejiexmu.rpc.samples.provider.service;

import com.hejiexmu.rpc.samples.provider.entity.User;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author hejiexmu
 */
public interface UserService {
    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 创建用户
     *
     * @param user 用户信息
     * @return 创建的用户ID
     */
    Long createUser(User user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 是否更新成功
     */
    boolean updateUser(User user);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);
    
    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 根据年龄范围获取用户
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    List<User> getUsersByAgeRange(int minAge, int maxAge);
    
    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 获取用户总数
     *
     * @return 用户总数
     */
    long getUserCount();

    /**
     * 批量创建用户
     *
     * @param users 用户列表
     * @return 创建成功的用户数量
     */
    int batchCreateUsers(List<User> users);
}