package com.hejiexmu.rpc.auth.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户会话实体类
 * 
 * @author hejiexmu
 */
@Data
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_status", columnList = "status")
})
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "会话ID不能为空")
    @Column(name = "session_id", unique = true, nullable = false, length = 128)
    private String sessionId;
    
    @NotNull(message = "用户不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @NotNull(message = "过期时间不能为空")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 1:active, 0:expired
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * 检查会话是否有效
     */
    public boolean isValid() {
        return status != null && status == 1 && 
               expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
    
    /**
     * 检查会话是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * 激活会话
     */
    public void activate() {
        this.status = 1;
    }
    
    /**
     * 使会话过期
     */
    public void expire() {
        this.status = 0;
    }
    
    /**
     * 更新最后访问时间
     */
    public void updateLastAccessTime() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * 延长会话过期时间
     */
    public void extendExpiration(long minutes) {
        this.expiresAt = LocalDateTime.now().plusMinutes(minutes);
    }
    
    /**
     * 获取会话剩余时间（分钟）
     */
    public long getRemainingMinutes() {
        if (expiresAt == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt.isBefore(now)) {
            return 0;
        }
        return java.time.Duration.between(now, expiresAt).toMinutes();
    }
    
    /**
     * 设置会话有效性
     */
    public void setValid(boolean valid) {
        this.status = valid ? 1 : 0;
    }
    
    /**
     * 获取用户ID
     */
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    // 手动添加getter和setter方法以解决编译问题
    public Long getId() {
        return id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    /**
     * 获取客户端IP地址（兼容性方法）
     * @return IP地址
     */
    public String getClientIp() {
        return this.ipAddress;
    }
}