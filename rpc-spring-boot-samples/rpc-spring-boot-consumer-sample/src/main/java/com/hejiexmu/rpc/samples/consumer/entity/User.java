package com.hejiexmu.rpc.samples.consumer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类 - Consumer端
 * 
 * @author hejiexmu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 用户ID */
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 邮箱 */
    private String email;
    
    /** 手机号 */
    private String phone;
    
    /** 年龄 */
    private Integer age;
    
    /** 性别 */
    private String gender;
    
    /** 地址 */
    private String address;
    
    /** 状态 */
    private String status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /**
     * 简化构造函数
     */
    public User(String username, String email, Integer age) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.status = "ACTIVE";
    }
    
    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 检查用户信息是否有效
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() && 
               email != null && !email.trim().isEmpty();
    }
    
    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    /**
     * 激活用户
     */
    public void activate() {
        this.status = "ACTIVE";
        updateTimestamp();
    }
    
    /**
     * 停用用户
     */
    public void deactivate() {
        this.status = "INACTIVE";
        updateTimestamp();
    }
}