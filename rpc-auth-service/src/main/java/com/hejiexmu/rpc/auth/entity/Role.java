package com.hejiexmu.rpc.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 
 * @author hejiexmu
 */
@Data
@Entity
@Table(name = "roles")
@EqualsAndHashCode(exclude = {"users", "permissions"})
@ToString(exclude = {"users", "permissions"})
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;
    
    @Size(max = 255, message = "角色描述长度不能超过255个字符")
    @Column(name = "description")
    private String description;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1:active, 0:inactive
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 角色用户关联
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
    
    // 角色权限关联
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查角色是否激活
     */
    public boolean isActive() {
        return status != null && status == 1;
    }
    
    /**
     * 激活角色
     */
    public void activate() {
        this.status = 1;
    }
    
    /**
     * 禁用角色
     */
    public void deactivate() {
        this.status = 0;
    }
    
    /**
     * 添加权限
     */
    public void addPermission(Permission permission) {
        if (permission != null) {
            this.permissions.add(permission);
            permission.getRoles().add(this);
        }
    }
    
    /**
     * 移除权限
     */
    public void removePermission(Permission permission) {
        if (permission != null) {
            this.permissions.remove(permission);
            permission.getRoles().remove(this);
        }
    }
    
    /**
     * 检查角色是否拥有指定权限
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
            .anyMatch(permission -> permission.getPermissionName().equals(permissionName));
    }
    
    /**
     * 获取角色的所有权限名称
     */
    public Set<String> getPermissionNames() {
        Set<String> permissionNames = new HashSet<>();
        for (Permission permission : permissions) {
            permissionNames.add(permission.getPermissionName());
        }
        return permissionNames;
    }
    
    // 手动添加getter方法以解决编译问题
    public Long getId() {
        return id;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * 设置创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    public Set<User> getUsers() {
        return users;
    }
}