package com.hejiexmu.rpc.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejiexmu.rpc.auth.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 访问拒绝处理器实现
 * 处理已认证但权限不足的用户访问
 * 
 * @author hejiexmu
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessDeniedHandlerImpl.class);
    
    private final ObjectMapper objectMapper;
    
    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        // 获取当前认证用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymous";
        
        logger.warn("Access denied for user: {} to resource: {} from IP: {}, User-Agent: {}, Exception: {}",
                   username,
                   request.getRequestURI(),
                   getClientIpAddress(request),
                   request.getHeader("User-Agent"),
                   accessDeniedException.getMessage());
        
        // 设置响应状态码
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        // 设置响应内容类型
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 设置CORS头（如果需要）
        setCorsHeaders(response);
        
        // 构建错误响应
        ApiResponse<Object> errorResponse = buildErrorResponse(request, username, accessDeniedException);
        
        // 添加额外的错误信息
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setPath(request.getRequestURI());
        
        // 写入响应
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Error writing access denied response", e);
            response.getWriter().write("{\"error\":\"ACCESS_DENIED\",\"message\":\"Access denied\"}");
        }
    }
    
    /**
     * 构建错误响应
     */
    private ApiResponse<Object> buildErrorResponse(HttpServletRequest request, 
                                                  String username, 
                                                  AccessDeniedException accessDeniedException) {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 根据请求路径和方法返回不同的错误信息
        if (requestURI.startsWith("/api/admin/")) {
            return ApiResponse.error(
                "ADMIN_ACCESS_REQUIRED",
                "Administrator privileges required to access this resource.",
                createErrorDetails(username, requestURI, method, "ADMIN")
            );
        } else if (requestURI.startsWith("/api/users/")) {
            return ApiResponse.error(
                "USER_MANAGEMENT_ACCESS_REQUIRED",
                "User management privileges required to access this resource.",
                createErrorDetails(username, requestURI, method, "USER_MANAGER")
            );
        } else if (requestURI.startsWith("/api/roles/")) {
            return ApiResponse.error(
                "ROLE_MANAGEMENT_ACCESS_REQUIRED",
                "Role management privileges required to access this resource.",
                createErrorDetails(username, requestURI, method, "ROLE_MANAGER")
            );
        } else if (requestURI.startsWith("/api/permissions/")) {
            return ApiResponse.error(
                "PERMISSION_MANAGEMENT_ACCESS_REQUIRED",
                "Permission management privileges required to access this resource.",
                createErrorDetails(username, requestURI, method, "ADMIN")
            );
        } else if (requestURI.startsWith("/api/sessions/")) {
            return ApiResponse.error(
                "SESSION_MANAGEMENT_ACCESS_REQUIRED",
                "Session management privileges required to access this resource.",
                createErrorDetails(username, requestURI, method, "SESSION_MANAGER")
            );
        } else if (requestURI.startsWith("/rpc/")) {
            return ApiResponse.error(
                "RPC_ACCESS_DENIED",
                "RPC client access denied. Invalid or insufficient permissions.",
                createErrorDetails(username, requestURI, method, "RPC_CLIENT")
            );
        } else {
            return ApiResponse.error(
                "ACCESS_DENIED",
                "You do not have sufficient permissions to access this resource.",
                createErrorDetails(username, requestURI, method, "UNKNOWN")
            );
        }
    }
    
    /**
     * 创建错误详情
     */
    private Object createErrorDetails(String username, String requestURI, String method, String requiredRole) {
        return new ErrorDetails(
            username,
            requestURI,
            method,
            requiredRole,
            System.currentTimeMillis()
        );
    }
    
    /**
     * 设置CORS响应头
     */
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", 
            "Authorization, Content-Type, X-Requested-With, Accept, Origin, X-Session-Id, X-Client-IP");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        String xClientIp = request.getHeader("X-Client-IP");
        if (xClientIp != null && !xClientIp.isEmpty()) {
            return xClientIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 错误详情内部类
     */
    public static class ErrorDetails {
        private final String username;
        private final String requestURI;
        private final String method;
        private final String requiredRole;
        private final long timestamp;
        
        public ErrorDetails(String username, String requestURI, String method, String requiredRole, long timestamp) {
            this.username = username;
            this.requestURI = requestURI;
            this.method = method;
            this.requiredRole = requiredRole;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getUsername() {
            return username;
        }
        
        public String getRequestURI() {
            return requestURI;
        }
        
        public String getMethod() {
            return method;
        }
        
        public String getRequiredRole() {
            return requiredRole;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}