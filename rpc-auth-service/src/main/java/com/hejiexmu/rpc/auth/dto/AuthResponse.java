package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 认证响应DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse implements Serializable {
    
    /**
     * 创建Builder实例
     */
    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }
    
    /**
     * Builder类
     */
    public static class AuthResponseBuilder {
        private boolean success;
        private String message;
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private String sessionId;
        private UserInfo userInfo;
        private List<String> permissions;
        private List<String> roles;
        private LocalDateTime loginTime;
        private LocalDateTime expiresAt;
        
        public AuthResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public AuthResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public AuthResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public AuthResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public AuthResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public AuthResponseBuilder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }
        
        public AuthResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public AuthResponseBuilder userInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }
        
        public AuthResponseBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }
        
        public AuthResponseBuilder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }
        
        public AuthResponseBuilder loginTime(LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }
        
        public AuthResponseBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public AuthResponse build() {
            AuthResponse response = new AuthResponse();
            response.success = this.success;
            response.message = this.message;
            response.accessToken = this.accessToken;
            response.refreshToken = this.refreshToken;
            response.tokenType = this.tokenType;
            response.expiresIn = this.expiresIn;
            response.sessionId = this.sessionId;
            response.userInfo = this.userInfo;
            response.permissions = this.permissions;
            response.roles = this.roles;
            response.loginTime = this.loginTime;
            response.expiresAt = this.expiresAt;
            return response;
        }
    }
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";
    
    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户信息
     */
    private UserInfo userInfo;
    
    /**
     * 用户权限
     */
    private List<String> permissions;
    
    /**
     * 用户角色
     */
    private List<String> roles;
    
    /**
     * 登录时间
     */
    private LocalDateTime loginTime;
    
    /**
     * 令牌过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 用户信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户名
         */
        private String username;
        
        /**
         * 邮箱
         */
        private String email;
        
        /**
         * 手机号
         */
        private String phone;
        
        /**
         * 真实姓名
         */
        private String realName;
        
        /**
         * 用户状态
         */
        private Integer status;
        
        /**
         * 创建时间
         */
        private LocalDateTime createdAt;
        
        /**
         * 最后登录时间
         */
        private LocalDateTime lastLoginAt;
        
        /**
         * 获取用户ID
         */
        public Long getUserId() {
            return userId;
        }
        
        /**
         * 获取用户名
         */
        public String getUsername() {
            return username;
        }
        
        /**
         * 获取邮箱
         */
        public String getEmail() {
            return email;
        }
        
        /**
         * 获取手机号
         */
        public String getPhone() {
            return phone;
        }
        
        /**
         * 获取真实姓名
         */
        public String getRealName() {
            return realName;
        }
        
        /**
         * 获取状态
         */
        public Integer getStatus() {
            return status;
        }
        
        /**
         * 获取创建时间
         */
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        /**
         * 获取最后登录时间
         */
        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }
        
        // 手动添加setter方法
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
        
        public void setRealName(String realName) {
            this.realName = realName;
        }
        
        public void setStatus(Integer status) {
            this.status = status;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public void setLastLoginAt(LocalDateTime lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
        }
        
        // 手动添加builder方法
        public static UserInfoBuilder builder() {
            return new UserInfoBuilder();
        }
        
        public static class UserInfoBuilder {
            private Long userId;
            private String username;
            private String email;
            private String phone;
            private String realName;
            private Integer status;
            private LocalDateTime createdAt;
            private LocalDateTime lastLoginAt;
            
            public UserInfoBuilder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public UserInfoBuilder username(String username) {
                this.username = username;
                return this;
            }
            
            public UserInfoBuilder email(String email) {
                this.email = email;
                return this;
            }
            
            public UserInfoBuilder phone(String phone) {
                this.phone = phone;
                return this;
            }
            
            public UserInfoBuilder realName(String realName) {
                this.realName = realName;
                return this;
            }
            
            public UserInfoBuilder status(Integer status) {
                this.status = status;
                return this;
            }
            
            public UserInfoBuilder createdAt(LocalDateTime createdAt) {
                this.createdAt = createdAt;
                return this;
            }
            
            public UserInfoBuilder lastLoginAt(LocalDateTime lastLoginAt) {
                this.lastLoginAt = lastLoginAt;
                return this;
            }
            
            public UserInfo build() {
                UserInfo userInfo = new UserInfo();
                userInfo.setUserId(this.userId);
                userInfo.setUsername(this.username);
                userInfo.setEmail(this.email);
                userInfo.setPhone(this.phone);
                userInfo.setRealName(this.realName);
                userInfo.setStatus(this.status);
                userInfo.setCreatedAt(this.createdAt);
                userInfo.setLastLoginAt(this.lastLoginAt);
                return userInfo;
            }
        }
    }
    
    /**
     * 获取消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取用户信息
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取角色列表
     */
    public List<String> getRoles() {
        return roles;
    }
    
    /**
     * 获取权限列表
     */
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * 创建成功响应
     */
    public static AuthResponse success() {
        return AuthResponse.builder()
                .success(true)
                .build();
    }
    
    /**
     * 创建成功响应（带完整信息）
     */
    public static AuthResponse success(String accessToken, String refreshToken, String sessionId, 
                                     UserInfo userInfo, List<String> permissions, List<String> roles) {
        return AuthResponse.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .sessionId(sessionId)
                .userInfo(userInfo)
                .permissions(permissions)
                .roles(roles)
                .loginTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static AuthResponse failure(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}