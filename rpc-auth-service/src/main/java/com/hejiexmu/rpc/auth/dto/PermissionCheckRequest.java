package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * 权限验证请求DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionCheckRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 资源名称
     */
    @NotBlank(message = "资源名称不能为空")
    private String resource;
    
    /**
     * 操作名称
     */
    @NotBlank(message = "操作名称不能为空")
    private String action;
    
    /**
     * 权限名称（可选，如果提供则直接检查此权限）
     */
    private String permissionName;
    
    /**
     * 权限代码（用于权限检查）
     */
    private String permissionCode;
    
    /**
     * 需要检查的权限列表（批量检查）
     */
    private Set<String> permissions;
    
    /**
     * RPC服务名称（用于RPC权限检查）
     */
    private String serviceName;
    
    /**
     * RPC方法名称（用于RPC权限检查）
     */
    private String methodName;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 额外的上下文信息
     */
    private Map<String, Object> context;
    
    /**
     * 是否严格模式（严格模式下会检查会话有效性）
     */
    private Boolean strictMode = true;
    
    /**
     * 是否更新最后访问时间
     */
    private Boolean updateLastAccess = true;
    
    /**
     * 获取完整的权限标识符
     */
    public String getFullPermissionIdentifier() {
        if (permissionName != null && !permissionName.trim().isEmpty()) {
            return permissionName;
        }
        if (resource != null && action != null) {
            return resource + ":" + action;
        }
        return null;
    }
    
    /**
     * 获取RPC服务方法标识符
     */
    public String getRpcServiceMethodIdentifier() {
        if (serviceName != null && methodName != null) {
            return serviceName + "." + methodName;
        }
        return null;
    }
    
    /**
     * 检查是否为RPC权限检查请求
     */
    public boolean isRpcPermissionCheck() {
        return serviceName != null && methodName != null;
    }
    
    /**
     * 检查是否为批量权限检查请求
     */
    public boolean isBatchPermissionCheck() {
        return permissions != null && !permissions.isEmpty();
    }
    
    /**
     * 添加上下文信息
     */
    public void addContext(String key, Object value) {
        if (context == null) {
            context = new java.util.HashMap<>();
        }
        context.put(key, value);
    }
    
    /**
     * 获取上下文信息
     */
    public Object getContext(String key) {
        return context != null ? context.get(key) : null;
    }
    
    // 手动添加getter方法以确保编译通过
    public Long getUserId() {
        return userId;
    }
    
    public String getPermissionCode() {
        return permissionCode;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
}