package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话信息DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * 会话状态
     * 0: 已过期, 1: 活跃
     */
    private Integer status;
    
    /**
     * 剩余时间（秒）
     */
    private Long remainingTime;
    
    /**
     * 是否当前会话
     */
    private Boolean current;
    
    /**
     * 设备类型
     */
    private String deviceType;
    
    /**
     * 浏览器类型
     */
    private String browserType;
    
    /**
     * 操作系统
     */
    private String operatingSystem;
    
    /**
     * 地理位置
     */
    private String location;
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "已过期";
            case 1:
                return "活跃";
            default:
                return "未知";
        }
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
        return status == null || status == 0 || 
               expiresAt == null || expiresAt.isBefore(LocalDateTime.now());
    }
    
    /**
     * 获取会话持续时间（分钟）
     */
    public Long getDurationMinutes() {
        if (createdAt == null || lastAccessedAt == null) {
            return 0L;
        }
        return java.time.Duration.between(createdAt, lastAccessedAt).toMinutes();
    }
    
    /**
     * 获取剩余时间描述
     */
    public String getRemainingTimeDescription() {
        if (remainingTime == null || remainingTime <= 0) {
            return "已过期";
        }
        
        long hours = remainingTime / 3600;
        long minutes = (remainingTime % 3600) / 60;
        long seconds = remainingTime % 60;
        
        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return String.format("%d秒", seconds);
        }
    }
}