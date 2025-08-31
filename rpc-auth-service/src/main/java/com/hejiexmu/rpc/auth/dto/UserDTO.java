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
 * 用户信息DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long id;
    
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
     * 0: 禁用, 1: 启用, 2: 锁定
     */
    private Integer status;
    
    /**
     * 用户角色
     */
    private List<String> roles;
    
    /**
     * 用户权限
     */
    private List<String> permissions;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 最后登录IP
     */
    private String lastLoginIp;
    
    /**
     * 登录次数
     */
    private Integer loginCount;
    
    /**
     * 是否在线
     */
    private Boolean online;
    
    /**
     * 会话数量
     */
    private Integer sessionCount;
    
    /**
     * 获取用户ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * 设置用户ID
     */
    public void setId(Long id) {
        this.id = id;
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
     * 设置用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }
    
    /**
     * 设置邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }
    
    /**
     * 设置手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    /**
     * 设置真实姓名
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    /**
     * 设置状态
     */
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    /**
     * 设置角色列表
     */
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    
    /**
     * 设置权限列表
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * 设置创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 设置更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 设置最后登录时间
     */
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    /**
     * 设置最后登录IP
     */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }
    
    /**
     * 设置登录次数
     */
    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }
    
    /**
     * 设置在线状态
     */
    public void setOnline(Boolean online) {
        this.online = online;
    }
    
    /**
     * 设置会话数量
     */
    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "禁用";
            case 1:
                return "启用";
            case 2:
                return "锁定";
            default:
                return "未知";
        }
    }
    
    /**
     * 检查用户是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
    
    /**
     * 检查用户是否被锁定
     */
    public boolean isLocked() {
        return status != null && status == 2;
    }
    
    /**
     * 检查用户是否有指定角色
     */
    public boolean hasRole(String roleName) {
        return roles != null && roles.contains(roleName);
    }
    
    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(String permissionName) {
        return permissions != null && permissions.contains(permissionName);
    }
}