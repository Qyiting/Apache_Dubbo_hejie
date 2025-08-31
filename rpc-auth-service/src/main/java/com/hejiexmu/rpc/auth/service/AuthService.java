package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.dto.AuthResponse;
import com.hejiexmu.rpc.auth.dto.LoginRequest;
import com.hejiexmu.rpc.auth.dto.PermissionCheckRequest;
import com.hejiexmu.rpc.auth.dto.PermissionCheckResponse;
import com.hejiexmu.rpc.auth.dto.RegisterRequest;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.entity.UserSession;

import java.util.Optional;

/**
 * 认证服务接口
 * 
 * @author hejiexmu
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * 用户登出
     */
    boolean logout(String sessionId);
    
    /**
     * 用户登出（通过访问令牌）
     */
    boolean logoutByToken(String accessToken);
    
    /**
     * 刷新访问令牌
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * 验证访问令牌
     */
    boolean validateToken(String accessToken);
    
    /**
     * 验证刷新令牌
     */
    boolean validateRefreshToken(String refreshToken);
    
    /**
     * 从访问令牌获取用户信息
     */
    Optional<User> getUserFromToken(String accessToken);
    
    /**
     * 从访问令牌获取会话信息
     */
    Optional<UserSession> getSessionFromToken(String accessToken);
    
    /**
     * 从访问令牌获取用户ID
     */
    Optional<Long> getUserIdFromToken(String accessToken);
    
    /**
     * 从访问令牌获取会话ID
     */
    Optional<String> getSessionIdFromToken(String accessToken);
    
    /**
     * 检查用户权限
     */
    PermissionCheckResponse checkPermission(PermissionCheckRequest request);
    
    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(String accessToken, String resource, String action);
    
    /**
     * 检查用户是否可以访问RPC服务
     */
    boolean canAccessRpcService(String accessToken, String serviceName, String methodName);
    
    /**
     * 验证会话
     */
    boolean validateSession(String sessionId);
    
    /**
     * 验证会话并更新最后访问时间
     */
    boolean validateAndUpdateSession(String sessionId);
    
    /**
     * 使会话过期
     */
    boolean expireSession(String sessionId);
    
    /**
     * 使用户的所有会话过期
     */
    int expireAllUserSessions(Long userId);
    
    /**
     * 使用户的其他会话过期（保留当前会话）
     */
    int expireOtherUserSessions(Long userId, String currentSessionId);
    
    /**
     * 强制用户下线
     */
    boolean forceUserOffline(Long userId);
    
    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);
    
    /**
     * 获取用户的有效会话数量
     */
    long getUserValidSessionCount(Long userId);
    
    /**
     * 验证用户密码
     */
    boolean validatePassword(String username, String password);
    
    /**
     * 修改用户密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 重置用户密码
     */
    boolean resetPassword(String username, String newPassword, String resetToken);
    
    /**
     * 生成密码重置令牌
     */
    String generatePasswordResetToken(String username);
    
    /**
     * 验证密码重置令牌
     */
    boolean validatePasswordResetToken(String username, String resetToken);
    
    /**
     * 启用用户账户
     */
    boolean enableUser(Long userId);
    
    /**
     * 禁用用户账户
     */
    boolean disableUser(Long userId);
    
    /**
     * 锁定用户账户
     */
    boolean lockUser(Long userId);
    
    /**
     * 解锁用户账户
     */
    boolean unlockUser(Long userId);
    
    /**
     * 检查用户账户状态
     */
    boolean isUserEnabled(Long userId);
    
    /**
     * 检查用户账户是否被锁定
     */
    boolean isUserLocked(Long userId);
    
    /**
     * 记录登录失败
     */
    void recordLoginFailure(String username, String ipAddress, String reason);
    
    /**
     * 清除登录失败记录
     */
    void clearLoginFailures(String username);
    
    /**
     * 检查是否需要验证码
     */
    boolean requiresCaptcha(String username, String ipAddress);
    
    /**
     * 验证验证码
     */
    boolean validateCaptcha(String captcha, String captchaToken);
    
    /**
     * 生成验证码
     */
    CaptchaInfo generateCaptcha();
    
    /**
     * 发送邮箱验证码
     */
    boolean sendEmailVerificationCode(String email);
    
    /**
     * 验证邮箱验证码
     */
    boolean validateEmailVerificationCode(String email, String code);
    
    /**
     * 发送短信验证码
     */
    boolean sendSmsVerificationCode(String phone);
    
    /**
     * 验证短信验证码
     */
    boolean validateSmsVerificationCode(String phone, String code);
    
    /**
     * 检查用户名是否存在
     */
    boolean usernameExists(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean emailExists(String email);
    
    /**
     * 检查手机号是否存在
     */
    boolean phoneExists(String phone);
    
    /**
     * 获取认证统计信息
     */
    AuthStatistics getAuthStatistics();
    
    /**
     * 清理过期的令牌和会话
     */
    int cleanupExpiredTokensAndSessions();
    
    /**
     * 验证IP地址是否被限制
     */
    boolean isIpRestricted(String ipAddress);
    
    /**
     * 添加IP地址到黑名单
     */
    boolean addIpToBlacklist(String ipAddress, String reason);
    
    /**
     * 从黑名单移除IP地址
     */
    boolean removeIpFromBlacklist(String ipAddress);
    
    /**
     * 检查用户代理是否被限制
     */
    boolean isUserAgentRestricted(String userAgent);
    
    /**
     * 获取安全事件日志
     */
    void logSecurityEvent(String eventType, String username, String ipAddress, String details);
    
    /**
     * 验证码信息内部类
     */
    class CaptchaInfo {
        private String captchaToken;
        private String captchaImage; // Base64编码的图片
        private int expirationSeconds;
        
        public CaptchaInfo(String captchaToken, String captchaImage, int expirationSeconds) {
            this.captchaToken = captchaToken;
            this.captchaImage = captchaImage;
            this.expirationSeconds = expirationSeconds;
        }
        
        // Getters and Setters
        public String getCaptchaToken() {
            return captchaToken;
        }
        
        public void setCaptchaToken(String captchaToken) {
            this.captchaToken = captchaToken;
        }
        
        public String getCaptchaImage() {
            return captchaImage;
        }
        
        public void setCaptchaImage(String captchaImage) {
            this.captchaImage = captchaImage;
        }
        
        public int getExpirationSeconds() {
            return expirationSeconds;
        }
        
        public void setExpirationSeconds(int expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }
    
    /**
     * 认证统计信息内部类
     */
    class AuthStatistics {
        private long totalUsers;
        private long activeUsers;
        private long onlineUsers;
        private long todayLogins;
        private long todayRegistrations;
        private long failedLogins;
        private long lockedAccounts;
        private long expiredSessions;
        
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
        
        public long getOnlineUsers() {
            return onlineUsers;
        }
        
        public void setOnlineUsers(long onlineUsers) {
            this.onlineUsers = onlineUsers;
        }
        
        public long getTodayLogins() {
            return todayLogins;
        }
        
        public void setTodayLogins(long todayLogins) {
            this.todayLogins = todayLogins;
        }
        
        public long getTodayRegistrations() {
            return todayRegistrations;
        }
        
        public void setTodayRegistrations(long todayRegistrations) {
            this.todayRegistrations = todayRegistrations;
        }
        
        public long getFailedLogins() {
            return failedLogins;
        }
        
        public void setFailedLogins(long failedLogins) {
            this.failedLogins = failedLogins;
        }
        
        public long getLockedAccounts() {
            return lockedAccounts;
        }
        
        public void setLockedAccounts(long lockedAccounts) {
            this.lockedAccounts = lockedAccounts;
        }
        
        public long getExpiredSessions() {
            return expiredSessions;
        }
        
        public void setExpiredSessions(long expiredSessions) {
            this.expiredSessions = expiredSessions;
        }
    }
}