package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户登录请求DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户名或邮箱
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 是否记住登录状态
     */
    private Boolean rememberMe = false;
    
    /**
     * 验证码（可选）
     */
    private String captcha;
    
    /**
     * 验证码令牌（可选）
     */
    private String captchaToken;
    
    // 手动添加getter和setter方法以解决编译问题
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * 获取密码
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * 设置用户代理
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * 获取是否记住登录状态
     */
    public boolean isRememberMe() {
        return rememberMe != null ? rememberMe : false;
    }
    
    /**
     * 设置是否记住登录状态
     */
    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
    
    /**
     * 获取用户代理信息
     */
    public String getUserAgent() {
        return userAgent;
    }
}