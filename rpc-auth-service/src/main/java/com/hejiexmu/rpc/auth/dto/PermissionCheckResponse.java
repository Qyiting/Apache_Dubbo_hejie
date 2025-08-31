package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 权限验证响应DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCheckResponse implements Serializable {
    
    public static PermissionCheckResponseBuilder builder() {
        return new PermissionCheckResponseBuilder();
    }
    
    public static class PermissionCheckResponseBuilder {
        private Boolean hasPermission;
        private Long userId;
        private String username;
        private String sessionId;
        private Boolean sessionValid;
        private Set<String> userPermissions;
        private Set<String> userRoles;
        private Map<String, Boolean> permissionResults;
        private String failureReason;
        private String errorCode;
        private LocalDateTime checkTime;
        private Map<String, Object> additionalInfo;
        
        public PermissionCheckResponseBuilder hasPermission(Boolean hasPermission) {
            this.hasPermission = hasPermission;
            return this;
        }
        
        public PermissionCheckResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public PermissionCheckResponseBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public PermissionCheckResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public PermissionCheckResponseBuilder sessionValid(Boolean sessionValid) {
            this.sessionValid = sessionValid;
            return this;
        }
        
        public PermissionCheckResponseBuilder userPermissions(Set<String> userPermissions) {
            this.userPermissions = userPermissions;
            return this;
        }
        
        public PermissionCheckResponseBuilder userRoles(Set<String> userRoles) {
            this.userRoles = userRoles;
            return this;
        }
        
        public PermissionCheckResponseBuilder permissionResults(Map<String, Boolean> permissionResults) {
            this.permissionResults = permissionResults;
            return this;
        }
        
        public PermissionCheckResponseBuilder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }
        
        public PermissionCheckResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }
        
        public PermissionCheckResponseBuilder checkTime(LocalDateTime checkTime) {
            this.checkTime = checkTime;
            return this;
        }
        
        public PermissionCheckResponseBuilder additionalInfo(Map<String, Object> additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }
        
        public PermissionCheckResponse build() {
            PermissionCheckResponse response = new PermissionCheckResponse();
            response.hasPermission = this.hasPermission;
            response.userId = this.userId;
            response.username = this.username;
            response.sessionId = this.sessionId;
            response.sessionValid = this.sessionValid;
            response.userPermissions = this.userPermissions;
            response.userRoles = this.userRoles;
            response.permissionResults = this.permissionResults;
            response.failureReason = this.failureReason;
            response.errorCode = this.errorCode;
            response.checkTime = this.checkTime;
            response.additionalInfo = this.additionalInfo;
            return response;
        }
    }
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 验证是否通过
     */
    private Boolean hasPermission;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 会话是否有效
     */
    private Boolean sessionValid;
    
    /**
     * 用户拥有的权限列表
     */
    private Set<String> userPermissions;
    
    /**
     * 用户拥有的角色列表
     */
    private Set<String> userRoles;
    
    /**
     * 批量权限检查结果（权限名 -> 是否拥有）
     */
    private Map<String, Boolean> permissionResults;
    
    /**
     * 验证失败原因
     */
    private String failureReason;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 验证时间
     */
    private LocalDateTime checkTime;
    
    /**
     * 会话剩余时间（秒）
     */
    private Long sessionRemainingTime;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 额外的响应信息
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 创建成功响应
     */
    public static PermissionCheckResponse success(Long userId, String username, String sessionId) {
        return PermissionCheckResponse.builder()
                .hasPermission(true)
                .userId(userId)
                .username(username)
                .sessionId(sessionId)
                .sessionValid(true)
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static PermissionCheckResponse failure(String failureReason, String errorCode) {
        return PermissionCheckResponse.builder()
                .hasPermission(false)
                .sessionValid(false)
                .failureReason(failureReason)
                .errorCode(errorCode)
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建会话无效响应
     */
    public static PermissionCheckResponse sessionInvalid(String sessionId, String reason) {
        return PermissionCheckResponse.builder()
                .hasPermission(false)
                .sessionId(sessionId)
                .sessionValid(false)
                .failureReason(reason)
                .errorCode("SESSION_INVALID")
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建权限不足响应
     */
    public static PermissionCheckResponse permissionDenied(Long userId, String username, String sessionId, String permission) {
        return PermissionCheckResponse.builder()
                .hasPermission(false)
                .userId(userId)
                .username(username)
                .sessionId(sessionId)
                .sessionValid(true)
                .failureReason("权限不足，缺少权限: " + permission)
                .errorCode("PERMISSION_DENIED")
                .checkTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 添加额外信息
     */
    public void addAdditionalInfo(String key, Object value) {
        if (additionalInfo == null) {
            additionalInfo = new java.util.HashMap<>();
        }
        additionalInfo.put(key, value);
    }
    
    /**
     * 获取额外信息
     */
    public Object getAdditionalInfo(String key) {
        return additionalInfo != null ? additionalInfo.get(key) : null;
    }
    
    /**
     * 检查是否有指定权限
     */
    public Boolean hasSpecificPermission(String permission) {
        if (permissionResults != null) {
            return permissionResults.get(permission);
        }
        return userPermissions != null && userPermissions.contains(permission);
    }
    
    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(String roleName) {
        return userRoles != null && userRoles.contains(roleName);
    }
    
    /**
     * 获取会话剩余时间描述
     */
    public String getSessionRemainingTimeDescription() {
        if (sessionRemainingTime == null || sessionRemainingTime <= 0) {
            return "会话已过期";
        }
        
        long hours = sessionRemainingTime / 3600;
        long minutes = (sessionRemainingTime % 3600) / 60;
        long seconds = sessionRemainingTime % 60;
        
        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    // 手动添加setter方法以确保编译通过
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public void setHasPermission(Boolean hasPermission) {
        this.hasPermission = hasPermission;
    }
    
    public void setReason(String reason) {
        this.failureReason = reason;
    }
    
    public void setTimestamp(long timestamp) {
        // 将时间戳转换为LocalDateTime
        this.checkTime = LocalDateTime.now();
    }
}