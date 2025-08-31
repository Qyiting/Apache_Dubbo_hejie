package com.hejiexmu.rpc.auth.config;

import com.hejiexmu.rpc.auth.filter.JwtAuthenticationFilter;
import com.hejiexmu.rpc.auth.handler.AuthenticationEntryPointImpl;
import com.hejiexmu.rpc.auth.handler.AccessDeniedHandlerImpl;
import com.hejiexmu.rpc.auth.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * 配置认证和授权机制
 * 
 * @author hejiexmu
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
    
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    
    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                         PasswordEncoder passwordEncoder,
                         JwtAuthenticationFilter jwtAuthenticationFilter,
                         AuthenticationEntryPointImpl authenticationEntryPoint,
                         AccessDeniedHandlerImpl accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }
    
    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护（因为使用JWT）
            .csrf().disable()
            
            // 配置CORS
            .cors().configurationSource(corsConfigurationSource())
            
            .and()
            
            // 配置会话管理策略为无状态
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            
            .and()
            
            // 配置授权规则
            .authorizeHttpRequests(authz -> authz
                // 公开接口，无需认证
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/captcha").permitAll()
                .requestMatchers("/api/auth/forgot-password").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/error").permitAll()
                
                // 管理员接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 用户管理接口
                .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "USER_MANAGER")
                
                // 角色管理接口
                .requestMatchers("/api/roles/**").hasAnyRole("ADMIN", "ROLE_MANAGER")
                
                // 权限管理接口
                .requestMatchers("/api/permissions/**").hasRole("ADMIN")
                
                // 会话管理接口
                .requestMatchers("/api/sessions/**").hasAnyRole("ADMIN", "SESSION_MANAGER")
                
                // RPC内部调用接口
                .requestMatchers("/rpc/**").hasRole("RPC_CLIENT")
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 配置异常处理
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler)
            
            .and()
            
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * 配置认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }
    
    /**
     * 配置认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 配置CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Session-Id",
            "X-Client-IP",
            "User-Agent"
        ));
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Session-Id",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));
        
        // 允许发送凭证
        configuration.setAllowCredentials(true);
        
        // 预检请求缓存时间
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * 安全配置属性
     */
    @Configuration
    public static class SecurityProperties {
        
        /**
         * JWT配置
         */
        public static class Jwt {
            private String secret = "mySecretKey";
            private long expiration = 86400000; // 24小时
            private long refreshExpiration = 604800000; // 7天
            private String header = "Authorization";
            private String prefix = "Bearer ";
            
            // Getters and Setters
            public String getSecret() {
                return secret;
            }
            
            public void setSecret(String secret) {
                this.secret = secret;
            }
            
            public long getExpiration() {
                return expiration;
            }
            
            public void setExpiration(long expiration) {
                this.expiration = expiration;
            }
            
            public long getRefreshExpiration() {
                return refreshExpiration;
            }
            
            public void setRefreshExpiration(long refreshExpiration) {
                this.refreshExpiration = refreshExpiration;
            }
            
            public String getHeader() {
                return header;
            }
            
            public void setHeader(String header) {
                this.header = header;
            }
            
            public String getPrefix() {
                return prefix;
            }
            
            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }
        }
        
        /**
         * 会话配置
         */
        public static class Session {
            private int maxConcurrentSessions = 5; // 最大并发会话数
            private boolean maxSessionsPreventsLogin = false; // 是否阻止新登录
            private int sessionTimeout = 1800; // 会话超时时间（秒）
            private boolean invalidateSessionOnLogout = true; // 登出时是否使会话无效
            
            // Getters and Setters
            public int getMaxConcurrentSessions() {
                return maxConcurrentSessions;
            }
            
            public void setMaxConcurrentSessions(int maxConcurrentSessions) {
                this.maxConcurrentSessions = maxConcurrentSessions;
            }
            
            public boolean isMaxSessionsPreventsLogin() {
                return maxSessionsPreventsLogin;
            }
            
            public void setMaxSessionsPreventsLogin(boolean maxSessionsPreventsLogin) {
                this.maxSessionsPreventsLogin = maxSessionsPreventsLogin;
            }
            
            public int getSessionTimeout() {
                return sessionTimeout;
            }
            
            public void setSessionTimeout(int sessionTimeout) {
                this.sessionTimeout = sessionTimeout;
            }
            
            public boolean isInvalidateSessionOnLogout() {
                return invalidateSessionOnLogout;
            }
            
            public void setInvalidateSessionOnLogout(boolean invalidateSessionOnLogout) {
                this.invalidateSessionOnLogout = invalidateSessionOnLogout;
            }
        }
        
        /**
         * 密码策略配置
         */
        public static class Password {
            private int minLength = 8;
            private int maxLength = 128;
            private boolean requireUppercase = true;
            private boolean requireLowercase = true;
            private boolean requireDigit = true;
            private boolean requireSpecialChar = true;
            private int maxFailedAttempts = 5;
            private int lockoutDuration = 300; // 锁定时间（秒）
            
            // Getters and Setters
            public int getMinLength() {
                return minLength;
            }
            
            public void setMinLength(int minLength) {
                this.minLength = minLength;
            }
            
            public int getMaxLength() {
                return maxLength;
            }
            
            public void setMaxLength(int maxLength) {
                this.maxLength = maxLength;
            }
            
            public boolean isRequireUppercase() {
                return requireUppercase;
            }
            
            public void setRequireUppercase(boolean requireUppercase) {
                this.requireUppercase = requireUppercase;
            }
            
            public boolean isRequireLowercase() {
                return requireLowercase;
            }
            
            public void setRequireLowercase(boolean requireLowercase) {
                this.requireLowercase = requireLowercase;
            }
            
            public boolean isRequireDigit() {
                return requireDigit;
            }
            
            public void setRequireDigit(boolean requireDigit) {
                this.requireDigit = requireDigit;
            }
            
            public boolean isRequireSpecialChar() {
                return requireSpecialChar;
            }
            
            public void setRequireSpecialChar(boolean requireSpecialChar) {
                this.requireSpecialChar = requireSpecialChar;
            }
            
            public int getMaxFailedAttempts() {
                return maxFailedAttempts;
            }
            
            public void setMaxFailedAttempts(int maxFailedAttempts) {
                this.maxFailedAttempts = maxFailedAttempts;
            }
            
            public int getLockoutDuration() {
                return lockoutDuration;
            }
            
            public void setLockoutDuration(int lockoutDuration) {
                this.lockoutDuration = lockoutDuration;
            }
        }
    }
}