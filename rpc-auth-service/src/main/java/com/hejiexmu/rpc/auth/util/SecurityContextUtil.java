package com.hejiexmu.rpc.auth.util;

import com.hejiexmu.rpc.auth.service.UserDetailsServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具类
 * 用于管理当前用户的上下文信息
 * 
 * @author hejiexmu
 */
public class SecurityContextUtil {
    
    // ThreadLocal变量用于存储当前线程的用户信息
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentClientIp = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserAgent = new ThreadLocal<>();
    
    /**
     * 获取当前认证用户
     */
    public static UserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails != null) {
            return userDetails.getUsername();
        }
        
        // 从ThreadLocal获取
        return currentUser.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails instanceof UserDetailsServiceImpl.UserDetailsImpl) {
            return ((UserDetailsServiceImpl.UserDetailsImpl) userDetails).getId();
        }
        return null;
    }
    
    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentUserEmail() {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails instanceof UserDetailsServiceImpl.UserDetailsImpl) {
            return ((UserDetailsServiceImpl.UserDetailsImpl) userDetails).getEmail();
        }
        return null;
    }
    
    /**
     * 获取当前用户全名
     */
    public static String getCurrentUserFullName() {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails instanceof UserDetailsServiceImpl.UserDetailsImpl) {
            return ((UserDetailsServiceImpl.UserDetailsImpl) userDetails).getFullName();
        }
        return null;
    }
    
    /**
     * 检查当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails != null) {
            return userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }
    
    /**
     * 检查当前用户是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        UserDetails userDetails = getCurrentUserDetails();
        if (userDetails != null) {
            return userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(permission));
        }
        return false;
    }
    
    /**
     * 检查当前用户是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * 获取当前会话ID
     */
    public static String getCurrentSessionId() {
        return currentSessionId.get();
    }
    
    /**
     * 设置当前会话ID
     */
    public static void setCurrentSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }
    
    /**
     * 获取当前客户端IP
     */
    public static String getCurrentClientIp() {
        return currentClientIp.get();
    }
    
    /**
     * 设置当前客户端IP
     */
    public static void setCurrentClientIp(String clientIp) {
        currentClientIp.set(clientIp);
    }
    
    /**
     * 获取当前用户代理
     */
    public static String getCurrentUserAgent() {
        return currentUserAgent.get();
    }
    
    /**
     * 设置当前用户代理
     */
    public static void setCurrentUserAgent(String userAgent) {
        currentUserAgent.set(userAgent);
    }
    
    /**
     * 设置当前用户
     */
    public static void setCurrentUser(String username) {
        currentUser.set(username);
    }
    
    /**
     * 清除当前线程的所有上下文信息
     */
    public static void clear() {
        currentUser.remove();
        currentSessionId.remove();
        currentClientIp.remove();
        currentUserAgent.remove();
    }
    
    /**
     * 获取当前用户的所有上下文信息
     */
    public static UserContext getCurrentUserContext() {
        return new UserContext(
            getCurrentUsername(),
            getCurrentUserId(),
            getCurrentUserEmail(),
            getCurrentUserFullName(),
            getCurrentSessionId(),
            getCurrentClientIp(),
            getCurrentUserAgent(),
            isAuthenticated(),
            isAdmin()
        );
    }
    
    /**
     * 用户上下文信息类
     */
    public static class UserContext {
        private final String username;
        private final Long userId;
        private final String email;
        private final String fullName;
        private final String sessionId;
        private final String clientIp;
        private final String userAgent;
        private final boolean authenticated;
        private final boolean admin;
        
        public UserContext(String username, Long userId, String email, String fullName,
                          String sessionId, String clientIp, String userAgent,
                          boolean authenticated, boolean admin) {
            this.username = username;
            this.userId = userId;
            this.email = email;
            this.fullName = fullName;
            this.sessionId = sessionId;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.authenticated = authenticated;
            this.admin = admin;
        }
        
        // Getters
        public String getUsername() {
            return username;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getClientIp() {
            return clientIp;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public boolean isAuthenticated() {
            return authenticated;
        }
        
        public boolean isAdmin() {
            return admin;
        }
        
        @Override
        public String toString() {
            return "UserContext{" +
                   "username='" + username + '\'' +
                   ", userId=" + userId +
                   ", email='" + email + '\'' +
                   ", fullName='" + fullName + '\'' +
                   ", sessionId='" + sessionId + '\'' +
                   ", clientIp='" + clientIp + '\'' +
                   ", userAgent='" + userAgent + '\'' +
                   ", authenticated=" + authenticated +
                   ", admin=" + admin +
                   '}';
        }
    }
}