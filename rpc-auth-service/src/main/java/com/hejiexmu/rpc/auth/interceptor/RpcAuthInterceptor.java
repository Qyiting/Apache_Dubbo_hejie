package com.hejiexmu.rpc.auth.interceptor;

import com.hejiexmu.rpc.auth.dto.PermissionCheckRequest;
import com.hejiexmu.rpc.auth.dto.PermissionCheckResponse;
import com.hejiexmu.rpc.auth.service.AuthService;
import com.hejiexmu.rpc.auth.util.JwtUtil;
import com.hejiexmu.rpc.auth.exception.BusinessException;
import com.hejiexmu.rpc.auth.exception.RpcException;
import com.hejiexmu.rpc.auth.dto.RpcRequest;
import com.hejiexmu.rpc.auth.dto.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC认证拦截器
 * 在所有RPC调用前进行身份验证和权限检查
 * 
 * @author hejiexmu
 */
@Component
public class RpcAuthInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcAuthInterceptor.class);
    
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String CLIENT_IP_HEADER = "X-Client-IP";
    private static final String USER_AGENT_HEADER = "User-Agent";
    
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    
    // 缓存服务方法的权限要求
    private final Map<String, String> methodPermissionCache = new ConcurrentHashMap<>();
    
    // 白名单服务（不需要认证）
    private final Map<String, Boolean> whitelistServices = new ConcurrentHashMap<>();
    
    public RpcAuthInterceptor(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        initializeWhitelist();
    }
    
    /**
     * 初始化白名单服务
     */
    private void initializeWhitelist() {
        // 认证相关的服务不需要认证
        whitelistServices.put("AuthService.login", true);
        whitelistServices.put("AuthService.register", true);
        whitelistServices.put("AuthService.refreshToken", true);
        whitelistServices.put("AuthService.generateCaptcha", true);
        whitelistServices.put("AuthService.validateCaptcha", true);
        whitelistServices.put("AuthService.sendEmailVerificationCode", true);
        whitelistServices.put("AuthService.sendSmsVerificationCode", true);
        
        // 健康检查等系统服务
        whitelistServices.put("HealthService.check", true);
        whitelistServices.put("MetricsService.getMetrics", true);
    }
    
    /**
     * RPC调用前的认证拦截
     */
    public boolean preHandle(RpcRequest request, Map<String, String> headers) throws RpcException {
        try {
            String serviceName = request.getServiceName();
            String methodName = request.getMethodName();
            String serviceMethod = serviceName + "." + methodName;
            
            logger.debug("RPC认证拦截: service={}, method={}", serviceName, methodName);
            
            // 检查是否在白名单中
            if (isWhitelisted(serviceMethod)) {
                logger.debug("服务在白名单中，跳过认证: {}", serviceMethod);
                return true;
            }
            
            // 提取认证信息
            String accessToken = extractAccessToken(headers);
            String sessionId = extractSessionId(headers);
            String clientIp = extractClientIp(headers);
            String userAgent = extractUserAgent(headers);
            
            // 验证访问令牌
            if (!StringUtils.hasText(accessToken)) {
                logger.warn("RPC调用缺少访问令牌: service={}, method={}", serviceName, methodName);
                throw new RpcException("UNAUTHORIZED", "访问令牌缺失");
            }
            
            if (!authService.validateToken(accessToken)) {
                logger.warn("RPC调用访问令牌无效: service={}, method={}", serviceName, methodName);
                throw new RpcException("UNAUTHORIZED", "访问令牌无效");
            }
            
            // 获取用户信息
            Long userId = jwtUtil.getUserIdFromToken(accessToken);
            if (userId == null) {
                logger.warn("无法从令牌中获取用户ID: service={}, method={}", serviceName, methodName);
                throw new RpcException("UNAUTHORIZED", "令牌格式错误");
            }
            
            // 验证会话
            if (StringUtils.hasText(sessionId)) {
                if (!authService.validateAndUpdateSession(sessionId)) {
                    logger.warn("会话验证失败: sessionId={}, service={}, method={}", sessionId, serviceName, methodName);
                    throw new RpcException("SESSION_EXPIRED", "会话已过期");
                }
            }
            
            // 检查RPC服务权限
            if (!authService.canAccessRpcService(accessToken, serviceName, methodName)) {
                logger.warn("用户无权限访问RPC服务: userId={}, service={}, method={}", userId, serviceName, methodName);
                throw new RpcException("FORBIDDEN", "无权限访问该服务");
            }
            
            // 记录访问日志
            logRpcAccess(userId, serviceName, methodName, clientIp, userAgent, true);
            
            // 将用户信息添加到请求上下文
            addUserContextToRequest(request, userId, sessionId);
            
            logger.debug("RPC认证通过: userId={}, service={}, method={}", userId, serviceName, methodName);
            return true;
            
        } catch (RpcException e) {
            throw e;
        } catch (Exception e) {
            logger.error("RPC认证拦截异常: service={}, method={}", request.getServiceName(), request.getMethodName(), e);
            throw new RpcException("INTERNAL_ERROR", "认证服务异常");
        }
    }
    
    /**
     * RPC调用后的处理
     */
    public void postHandle(RpcRequest request, RpcResponse response, Map<String, String> headers) {
        try {
            String serviceName = request.getServiceName();
            String methodName = request.getMethodName();
            
            // 记录调用结果
            boolean success = response != null && response.getException() == null;
            
            // 从请求上下文获取用户信息
            Long userId = getUserIdFromContext(request);
            String clientIp = extractClientIp(headers);
            String userAgent = extractUserAgent(headers);
            
            // 记录访问日志
            if (userId != null) {
                logRpcAccess(userId, serviceName, methodName, clientIp, userAgent, success);
            }
            
            logger.debug("RPC调用完成: service={}, method={}, success={}", serviceName, methodName, success);
            
        } catch (Exception e) {
            logger.error("RPC后置处理异常: service={}, method={}", request.getServiceName(), request.getMethodName(), e);
        }
    }
    
    /**
     * 异常处理
     */
    public void afterCompletion(RpcRequest request, RpcResponse response, Exception ex, Map<String, String> headers) {
        try {
            if (ex != null) {
                String serviceName = request.getServiceName();
                String methodName = request.getMethodName();
                Long userId = getUserIdFromContext(request);
                String clientIp = extractClientIp(headers);
                
                logger.warn("RPC调用异常: userId={}, service={}, method={}, clientIp={}, error={}", 
                           userId, serviceName, methodName, clientIp, ex.getMessage());
                
                // 记录安全事件
                if (ex instanceof RpcException) {
                    RpcException rpcEx = (RpcException) ex;
                    if ("UNAUTHORIZED".equals(rpcEx.getCode()) || "FORBIDDEN".equals(rpcEx.getCode())) {
                        authService.logSecurityEvent("RPC_ACCESS_DENIED", 
                                                    userId != null ? userId.toString() : "unknown", 
                                                    clientIp, 
                                                    String.format("Service: %s.%s, Reason: %s", serviceName, methodName, rpcEx.getMessage()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("RPC异常处理失败", e);
        }
    }
    
    /**
     * 检查服务是否在白名单中
     */
    private boolean isWhitelisted(String serviceMethod) {
        return whitelistServices.getOrDefault(serviceMethod, false);
    }
    
    /**
     * 提取访问令牌
     */
    private String extractAccessToken(Map<String, String> headers) {
        String authHeader = headers.get(AUTH_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    /**
     * 提取会话ID
     */
    private String extractSessionId(Map<String, String> headers) {
        return headers.get(SESSION_HEADER);
    }
    
    /**
     * 提取客户端IP
     */
    private String extractClientIp(Map<String, String> headers) {
        String clientIp = headers.get(CLIENT_IP_HEADER);
        if (!StringUtils.hasText(clientIp)) {
            clientIp = headers.get("X-Forwarded-For");
        }
        if (!StringUtils.hasText(clientIp)) {
            clientIp = headers.get("X-Real-IP");
        }
        return clientIp;
    }
    
    /**
     * 提取用户代理
     */
    private String extractUserAgent(Map<String, String> headers) {
        return headers.get(USER_AGENT_HEADER);
    }
    
    /**
     * 将用户信息添加到请求上下文
     */
    private void addUserContextToRequest(RpcRequest request, Long userId, String sessionId) {
        if (request.getAttachments() == null) {
            request.setAttachments(new ConcurrentHashMap<>());
        }
        request.getAttachments().put("userId", userId.toString());
        if (StringUtils.hasText(sessionId)) {
            request.getAttachments().put("sessionId", sessionId);
        }
    }
    
    /**
     * 从请求上下文获取用户ID
     */
    private Long getUserIdFromContext(RpcRequest request) {
        if (request.getAttachments() != null) {
            String userIdStr = (String) request.getAttachments().get("userId");
            if (StringUtils.hasText(userIdStr)) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    logger.warn("无效的用户ID格式: {}", userIdStr);
                }
            }
        }
        return null;
    }
    
    /**
     * 记录RPC访问日志
     */
    private void logRpcAccess(Long userId, String serviceName, String methodName, String clientIp, String userAgent, boolean success) {
        try {
            String eventType = success ? "RPC_ACCESS_SUCCESS" : "RPC_ACCESS_FAILURE";
            String details = String.format("Service: %s.%s, ClientIP: %s, UserAgent: %s", 
                                          serviceName, methodName, clientIp, userAgent);
            
            authService.logSecurityEvent(eventType, userId.toString(), clientIp, details);
        } catch (Exception e) {
            logger.error("记录RPC访问日志失败", e);
        }
    }
    
    /**
     * 添加服务到白名单
     */
    public void addToWhitelist(String serviceName, String methodName) {
        String serviceMethod = serviceName + "." + methodName;
        whitelistServices.put(serviceMethod, true);
        logger.info("服务已添加到白名单: {}", serviceMethod);
    }
    
    /**
     * 从白名单移除服务
     */
    public void removeFromWhitelist(String serviceName, String methodName) {
        String serviceMethod = serviceName + "." + methodName;
        whitelistServices.remove(serviceMethod);
        logger.info("服务已从白名单移除: {}", serviceMethod);
    }
    
    /**
     * 获取白名单服务列表
     */
    public Map<String, Boolean> getWhitelistServices() {
        return new ConcurrentHashMap<>(whitelistServices);
    }
    
    /**
     * 清空方法权限缓存
     */
    public void clearMethodPermissionCache() {
        methodPermissionCache.clear();
        logger.info("方法权限缓存已清空");
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getCacheStatistics() {
        CacheStatistics stats = new CacheStatistics();
        stats.setWhitelistSize(whitelistServices.size());
        stats.setPermissionCacheSize(methodPermissionCache.size());
        return stats;
    }
    
    /**
     * 缓存统计信息内部类
     */
    public static class CacheStatistics {
        private int whitelistSize;
        private int permissionCacheSize;
        
        public int getWhitelistSize() {
            return whitelistSize;
        }
        
        public void setWhitelistSize(int whitelistSize) {
            this.whitelistSize = whitelistSize;
        }
        
        public int getPermissionCacheSize() {
            return permissionCacheSize;
        }
        
        public void setPermissionCacheSize(int permissionCacheSize) {
            this.permissionCacheSize = permissionCacheSize;
        }
    }
}