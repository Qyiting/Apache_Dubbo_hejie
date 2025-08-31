package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.dto.*;
import com.hejiexmu.rpc.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户服务接口
 * 
 * @author hejiexmu
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * 用户登出
     */
    boolean logout(String sessionId);
    
    /**
     * 刷新访问令牌
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);
    
    /**
     * 根据ID获取用户详情
     */
    UserDTO getUserDetails(Long id);
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 创建用户（通过注册请求）
     */
    UserDTO createUser(RegisterRequest request);
    
    /**
     * 更新用户信息
     */
    User updateUser(User user);
    
    /**
     * 更新用户信息（通过DTO）
     */
    UserDTO updateUser(UserDTO userDTO);
    
    /**
     * 删除用户
     */
    boolean deleteUser(Long userId);
    
    /**
     * 启用用户
     */
    boolean enableUser(Long userId);
    
    /**
     * 禁用用户
     */
    boolean disableUser(Long userId);
    
    /**
     * 锁定用户
     */
    boolean lockUser(Long userId);
    
    /**
     * 解锁用户
     */
    boolean unlockUser(Long userId);
    
    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 重置密码
     */
    String resetPassword(Long userId);
    
    /**
     * 重置密码（通过邮箱）
     */
    boolean resetPasswordByEmail(String email);
    
    /**
     * 获取用户角色
     */
    Set<String> getUserRoles(Long userId);
    
    /**
     * 获取用户权限
     */
    Set<String> getUserPermissions(Long userId);
    
    /**
     * 为用户分配角色
     */
    boolean assignRole(Long userId, String roleName);
    
    /**
     * 为用户分配多个角色
     */
    boolean assignRoles(Long userId, Set<String> roleNames);
    
    /**
     * 移除用户角色
     */
    boolean removeRole(Long userId, String roleName);
    
    /**
     * 移除用户的多个角色
     */
    boolean removeRoles(Long userId, Set<String> roleNames);
    
    /**
     * 检查用户是否有指定角色
     */
    boolean hasRole(Long userId, String roleName);
    
    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String permissionName);
    
    /**
     * 检查用户是否有任一权限
     */
    boolean hasAnyPermission(Long userId, Set<String> permissionNames);
    
    /**
     * 检查用户是否有所有权限
     */
    boolean hasAllPermissions(Long userId, Set<String> permissionNames);
    
    /**
     * 分页查询用户
     */
    Page<UserDTO> findUsers(Pageable pageable);
    
    /**
     * 分页查询用户（带搜索条件）
     */
    PageResponse<UserDTO> findUsers(int page, int size, String username, String email, Boolean enabled);
    
    /**
     * 分页查询所有用户
     */
    Page<UserDTO> findAllUsers(Pageable pageable);
    
    /**
     * 根据状态分页查询用户
     */
    Page<UserDTO> findUsersByStatus(Integer status, Pageable pageable);
    
    /**
     * 根据角色分页查询用户
     */
    Page<UserDTO> findUsersByRole(String roleName, Pageable pageable);
    
    /**
     * 搜索用户
     */
    Page<UserDTO> searchUsers(String keyword, Pageable pageable);
    
    /**
     * 获取最近登录的用户
     */
    List<UserDTO> getRecentlyLoggedInUsers(int limit);
    
    /**
     * 获取在线用户
     */
    List<UserDTO> getOnlineUsers();
    
    /**
     * 获取用户统计信息
     */
    UserStatistics getUserStatistics();
    
    /**
     * 更新用户最后登录信息
     */
    void updateLastLoginInfo(Long userId, String ipAddress, String userAgent);
    
    /**
     * 验证用户凭据
     */
    boolean validateCredentials(String usernameOrEmail, String password);
    
    /**
     * 检查用户账户状态
     */
    AccountStatus checkAccountStatus(Long userId);
    
    /**
     * 用户统计信息内部类
     */
    class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long lockedUsers;
        private long disabledUsers;
        private long onlineUsers;
        private long todayRegistrations;
        private long todayLogins;
        
        // Getters and Setters
        public long getTotalUsers() {
            return totalUsers;
        }
        
        public void setTotalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
        }
        
        public long getActiveUsers() {
            return activeUsers;
        }
        
        public void setActiveUsers(long activeUsers) {
            this.activeUsers = activeUsers;
        }
        
        public long getInactiveUsers() {
            return inactiveUsers;
        }
        
        public void setInactiveUsers(long inactiveUsers) {
            this.inactiveUsers = inactiveUsers;
        }
        
        public long getLockedUsers() {
            return lockedUsers;
        }
        
        public void setLockedUsers(long lockedUsers) {
            this.lockedUsers = lockedUsers;
        }
        
        public long getDisabledUsers() {
            return disabledUsers;
        }
        
        public void setDisabledUsers(long disabledUsers) {
            this.disabledUsers = disabledUsers;
        }
        
        public long getOnlineUsers() {
            return onlineUsers;
        }
        
        public void setOnlineUsers(long onlineUsers) {
            this.onlineUsers = onlineUsers;
        }
        
        public long getTodayRegistrations() {
            return todayRegistrations;
        }
        
        public void setTodayRegistrations(long todayRegistrations) {
            this.todayRegistrations = todayRegistrations;
        }
        
        public long getTodayLogins() {
            return todayLogins;
        }
        
        public void setTodayLogins(long todayLogins) {
            this.todayLogins = todayLogins;
        }
    }
    
    /**
     * 账户状态枚举
     */
    enum AccountStatus {
        ACTIVE("正常"),
        DISABLED("已禁用"),
        LOCKED("已锁定"),
        EXPIRED("已过期"),
        PENDING("待激活");
        
        private final String description;
        
        AccountStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}