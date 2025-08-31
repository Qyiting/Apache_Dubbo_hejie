package com.hejiexmu.rpc.auth.entity;

import lombok.Data;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * RPC服务权限实体类
 * 
 * @author hejiexmu
 */
@Data
@Entity
@Table(name = "rpc_service_permissions", indexes = {
    @Index(name = "idx_service_name", columnList = "serviceName"),
    @Index(name = "idx_required_permission", columnList = "requiredPermission")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_service_method", columnNames = {"serviceName", "methodName"})
})
public class RpcServicePermission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称长度不能超过100个字符")
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @NotBlank(message = "方法名称不能为空")
    @Size(max = 100, message = "方法名称长度不能超过100个字符")
    @Column(name = "method_name", nullable = false, length = 100)
    private String methodName;
    
    @NotBlank(message = "所需权限不能为空")
    @Size(max = 100, message = "所需权限长度不能超过100个字符")
    @Column(name = "required_permission", nullable = false, length = 100)
    private String requiredPermission;
    
    @Size(max = 255, message = "描述长度不能超过255个字符")
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
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
     * 检查是否匹配指定的服务和方法
     */
    public boolean matches(String serviceName, String methodName) {
        return this.serviceName.equals(serviceName) && this.methodName.equals(methodName);
    }
    
    /**
     * 检查是否匹配指定的服务（支持通配符）
     */
    public boolean matchesService(String serviceName) {
        if ("*".equals(this.serviceName)) {
            return true;
        }
        return this.serviceName.equals(serviceName);
    }
    
    /**
     * 检查是否匹配指定的方法（支持通配符）
     */
    public boolean matchesMethod(String methodName) {
        if ("*".equals(this.methodName)) {
            return true;
        }
        return this.methodName.equals(methodName);
    }
    
    /**
     * 获取服务方法的完整标识符
     */
    public String getFullIdentifier() {
        return serviceName + "." + methodName;
    }
}