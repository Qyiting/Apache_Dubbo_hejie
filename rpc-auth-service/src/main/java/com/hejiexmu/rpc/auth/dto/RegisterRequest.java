package com.hejiexmu.rpc.auth.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * 用户注册请求DTO
 * 
 * @author hejiexmu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{6,}$", 
             message = "密码必须包含至少一个大写字母、一个小写字母和一个数字")
    private String password;
    
    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
    
    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 真实姓名
     */
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;
    
    /**
     * 客户端IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String captcha;
    
    /**
     * 验证码令牌
     */
    @NotBlank(message = "验证码令牌不能为空")
    private String captchaToken;
    
    /**
     * 邮箱验证码
     */
    private String emailCode;
    
    /**
     * 短信验证码
     */
    private String smsCode;
    
    /**
     * 是否同意用户协议
     */
    @NotNull(message = "必须同意用户协议")
    @AssertTrue(message = "必须同意用户协议")
    private Boolean agreeTerms;
    
    /**
     * 验证密码是否一致
     */
    public boolean isPasswordMatch() {
        return password != null && password.equals(confirmPassword);
    }
    
    /**
     * 设置客户端IP地址
     */
    public void setClientIp(String clientIp) {
        this.ipAddress = clientIp;
    }
    
    /**
     * 设置用户代理信息
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    /**
     * 获取IP地址
     */
    public String getIpAddress() {
        return this.ipAddress;
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
     * 获取密码
     */
    public String getPassword() {
        return password;
    }
    
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取用户代理信息
     */
    public String getUserAgent() {
        return userAgent;
    }
}