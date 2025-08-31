package com.hejiexmu.rpc.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejiexmu.rpc.auth.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证入口点实现
 * 处理未认证用户的访问
 * 
 * @author hejiexmu
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationEntryPointImpl.class);
    
    private final ObjectMapper objectMapper;
    
    public AuthenticationEntryPointImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("Unauthorized access attempt to: {} from IP: {}, User-Agent: {}, Exception: {}",
                   request.getRequestURI(),
                   getClientIpAddress(request),
                   request.getHeader("User-Agent"),
                   authException.getMessage());
        
        // 设置响应状态码
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // 设置响应内容类型
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        // 设置CORS头（如果需要）
        setCorsHeaders(response);
        
        // 构建错误响应
        ApiResponse<Object> errorResponse = ApiResponse.error(
            "UNAUTHORIZED",
            "Authentication required. Please provide valid credentials.",
            null
        );
        
        // 根据请求类型返回不同的错误信息
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/")) {
            // API请求
            if (requestURI.startsWith("/api/admin/")) {
                errorResponse = ApiResponse.error(
                    "ADMIN_ACCESS_REQUIRED",
                    "Administrator access required for this resource.",
                    null
                );
            } else if (requestURI.startsWith("/rpc/")) {
                errorResponse = ApiResponse.error(
                    "RPC_AUTH_REQUIRED",
                    "RPC client authentication required.",
                    null
                );
            }
        }
        
        // 添加额外的错误信息
        errorResponse.setTimestamp(System.currentTimeMillis());
        errorResponse.setPath(requestURI);
        
        // 写入响应
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Error writing authentication error response", e);
            response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}");
        }
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
}