package com.hejiexmu.rpc.auth.config;

import com.hejiexmu.rpc.auth.filter.RpcAuthFilter;
import com.hejiexmu.rpc.auth.interceptor.RpcAuthInterceptor;
import com.hejiexmu.rpc.auth.service.AuthService;
import com.hejiexmu.rpc.auth.util.JwtUtil;
import com.hejiexmu.rpc.auth.util.PasswordUtil;
import com.hejiexmu.rpc.auth.util.SessionUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证配置类
 * 
 * @author hejiexmu
 */
@Configuration
public class AuthConfig {
    
    @Value("${rpc.auth.jwt.secret:rpc-auth-secret-key-2024}")
    private String jwtSecret;
    
    @Value("${rpc.auth.jwt.access-token-expiration:3600}")
    private int accessTokenExpiration;
    
    @Value("${rpc.auth.jwt.refresh-token-expiration:86400}")
    private int refreshTokenExpiration;
    
    @Value("${rpc.auth.jwt.issuer:rpc-auth-service}")
    private String jwtIssuer;
    
    @Value("${rpc.auth.session.default-expiration:1440}")
    private int defaultSessionExpiration;
    
    @Value("${rpc.auth.session.remember-me-expiration:43200}")
    private int rememberMeSessionExpiration;
    
    @Value("${rpc.auth.password.min-length:8}")
    private int passwordMinLength;
    
    @Value("${rpc.auth.password.require-uppercase:true}")
    private boolean passwordRequireUppercase;
    
    @Value("${rpc.auth.password.require-lowercase:true}")
    private boolean passwordRequireLowercase;
    
    @Value("${rpc.auth.password.require-digit:true}")
    private boolean passwordRequireDigit;
    
    @Value("${rpc.auth.password.require-special:true}")
    private boolean passwordRequireSpecial;
    
    @Value("${rpc.auth.captcha.enabled:true}")
    private boolean captchaEnabled;
    
    @Value("${rpc.auth.captcha.expiration:300}")
    private int captchaExpiration;
    
    @Value("${rpc.auth.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${rpc.auth.rate-limit.max-attempts:5}")
    private int maxLoginAttempts;
    
    @Value("${rpc.auth.rate-limit.lockout-duration:900}")
    private int lockoutDuration;
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * JWT工具类
     * 注意：JwtUtil已经是@Component，Spring会自动创建实例
     */
    // JwtUtil jwtUtil() - 已通过@Component自动注册
    
    /**
     * 密码工具类
     * 注意：PasswordUtil已经是@Component，Spring会自动创建实例
     */
    // PasswordUtil passwordUtil() - 已通过@Component自动注册
    
    /**
     * 会话工具类
     * 注意：SessionUtil已经是@Component，Spring会自动创建实例
     */
    // SessionUtil sessionUtil() - 已通过@Component自动注册
    
    /**
     * RPC认证拦截器
     */
    @Bean
    public RpcAuthInterceptor rpcAuthInterceptor(AuthService authService, JwtUtil jwtUtil) {
        return new RpcAuthInterceptor(authService, jwtUtil);
    }
    
    /**
     * RPC认证过滤器
     */
    @Bean
    public RpcAuthFilter rpcAuthFilter(RpcAuthInterceptor authInterceptor) {
        return new RpcAuthFilter(authInterceptor);
    }
    
    /**
     * 认证属性配置
     */
    @Bean
    public AuthProperties authProperties() {
        AuthProperties properties = new AuthProperties();
        
        // JWT配置
        properties.setJwtSecret(jwtSecret);
        properties.setAccessTokenExpiration(accessTokenExpiration);
        properties.setRefreshTokenExpiration(refreshTokenExpiration);
        properties.setJwtIssuer(jwtIssuer);
        
        // 会话配置
        properties.setDefaultSessionExpiration(defaultSessionExpiration);
        properties.setRememberMeSessionExpiration(rememberMeSessionExpiration);
        
        // 密码策略配置
        properties.setPasswordMinLength(passwordMinLength);
        properties.setPasswordRequireUppercase(passwordRequireUppercase);
        properties.setPasswordRequireLowercase(passwordRequireLowercase);
        properties.setPasswordRequireDigit(passwordRequireDigit);
        properties.setPasswordRequireSpecial(passwordRequireSpecial);
        
        // 验证码配置
        properties.setCaptchaEnabled(captchaEnabled);
        properties.setCaptchaExpiration(captchaExpiration);
        
        // 限流配置
        properties.setRateLimitEnabled(rateLimitEnabled);
        properties.setMaxLoginAttempts(maxLoginAttempts);
        properties.setLockoutDuration(lockoutDuration);
        
        return properties;
    }
    
    /**
     * 认证属性配置类
     */
    public static class AuthProperties {
        private String jwtSecret;
        private int accessTokenExpiration;
        private int refreshTokenExpiration;
        private String jwtIssuer;
        private int defaultSessionExpiration;
        private int rememberMeSessionExpiration;
        private int passwordMinLength;
        private boolean passwordRequireUppercase;
        private boolean passwordRequireLowercase;
        private boolean passwordRequireDigit;
        private boolean passwordRequireSpecial;
        private boolean captchaEnabled;
        private int captchaExpiration;
        private boolean rateLimitEnabled;
        private int maxLoginAttempts;
        private int lockoutDuration;
        
        // Getters and Setters
        public String getJwtSecret() {
            return jwtSecret;
        }
        
        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }
        
        public int getAccessTokenExpiration() {
            return accessTokenExpiration;
        }
        
        public void setAccessTokenExpiration(int accessTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
        }
        
        public int getRefreshTokenExpiration() {
            return refreshTokenExpiration;
        }
        
        public void setRefreshTokenExpiration(int refreshTokenExpiration) {
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
        
        public String getJwtIssuer() {
            return jwtIssuer;
        }
        
        public void setJwtIssuer(String jwtIssuer) {
            this.jwtIssuer = jwtIssuer;
        }
        
        public int getDefaultSessionExpiration() {
            return defaultSessionExpiration;
        }
        
        public void setDefaultSessionExpiration(int defaultSessionExpiration) {
            this.defaultSessionExpiration = defaultSessionExpiration;
        }
        
        public int getRememberMeSessionExpiration() {
            return rememberMeSessionExpiration;
        }
        
        public void setRememberMeSessionExpiration(int rememberMeSessionExpiration) {
            this.rememberMeSessionExpiration = rememberMeSessionExpiration;
        }
        
        public int getPasswordMinLength() {
            return passwordMinLength;
        }
        
        public void setPasswordMinLength(int passwordMinLength) {
            this.passwordMinLength = passwordMinLength;
        }
        
        public boolean isPasswordRequireUppercase() {
            return passwordRequireUppercase;
        }
        
        public void setPasswordRequireUppercase(boolean passwordRequireUppercase) {
            this.passwordRequireUppercase = passwordRequireUppercase;
        }
        
        public boolean isPasswordRequireLowercase() {
            return passwordRequireLowercase;
        }
        
        public void setPasswordRequireLowercase(boolean passwordRequireLowercase) {
            this.passwordRequireLowercase = passwordRequireLowercase;
        }
        
        public boolean isPasswordRequireDigit() {
            return passwordRequireDigit;
        }
        
        public void setPasswordRequireDigit(boolean passwordRequireDigit) {
            this.passwordRequireDigit = passwordRequireDigit;
        }
        
        public boolean isPasswordRequireSpecial() {
            return passwordRequireSpecial;
        }
        
        public void setPasswordRequireSpecial(boolean passwordRequireSpecial) {
            this.passwordRequireSpecial = passwordRequireSpecial;
        }
        
        public boolean isCaptchaEnabled() {
            return captchaEnabled;
        }
        
        public void setCaptchaEnabled(boolean captchaEnabled) {
            this.captchaEnabled = captchaEnabled;
        }
        
        public int getCaptchaExpiration() {
            return captchaExpiration;
        }
        
        public void setCaptchaExpiration(int captchaExpiration) {
            this.captchaExpiration = captchaExpiration;
        }
        
        public boolean isRateLimitEnabled() {
            return rateLimitEnabled;
        }
        
        public void setRateLimitEnabled(boolean rateLimitEnabled) {
            this.rateLimitEnabled = rateLimitEnabled;
        }
        
        public int getMaxLoginAttempts() {
            return maxLoginAttempts;
        }
        
        public void setMaxLoginAttempts(int maxLoginAttempts) {
            this.maxLoginAttempts = maxLoginAttempts;
        }
        
        public int getLockoutDuration() {
            return lockoutDuration;
        }
        
        public void setLockoutDuration(int lockoutDuration) {
            this.lockoutDuration = lockoutDuration;
        }
    }
}