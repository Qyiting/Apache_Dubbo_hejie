package com.hejiexmu.rpc.auth.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import com.hejiexmu.rpc.auth.enums.PermissionStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限实体类
 * 
 * @author hejiexmu
 */
@Data
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_resource", columnList = "resource"),
    @Index(name = "idx_action", columnList = "action")
})
@EqualsAndHashCode(exclude = {"roles"})
@ToString(exclude = {"roles"})
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    @Column(name = "permission_name", unique = true, nullable = false, length = 100)
    private String permissionName;
    
    @NotBlank(message = "资源不能为空")
    @Size(max = 100, message = "资源长度不能超过100个字符")
    @Column(name = "resource", nullable = false, length = 100)
    private String resource;
    
    @NotBlank(message = "操作不能为空")
    @Size(max = 50, message = "操作长度不能超过50个字符")
    @Column(name = "action", nullable = false, length = 50)
    private String action;
    
    @Size(max = 255, message = "权限描述长度不能超过255个字符")
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1:active, 0:inactive
    
    // 权限角色关联
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    private Set<Role> roles = new HashSet<>();
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    /**
     * 检查权限是否匹配指定的资源和操作
     */
    public boolean matches(String resource, String action) {
        return this.resource.equals(resource) && this.action.equals(action);
    }
    
    /**
     * 检查权限是否匹配指定的资源（支持通配符）
     */
    public boolean matchesResource(String resource) {
        if ("*".equals(this.resource)) {
            return true;
        }
        return this.resource.equals(resource);
    }
    
    /**
     * 检查权限是否匹配指定的操作（支持通配符）
     */
    public boolean matchesAction(String action) {
        if ("*".equals(this.action)) {
            return true;
        }
        return this.action.equals(action);
    }
    
    /**
     * 获取权限的完整标识符
     */
    public String getFullIdentifier() {
        return resource + ":" + action;
    }
    
    // 手动添加getter方法以解决编译问题
    public String getPermissionName() {
        return permissionName;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
    
    public Integer getStatus() {
        return status;
    }
}