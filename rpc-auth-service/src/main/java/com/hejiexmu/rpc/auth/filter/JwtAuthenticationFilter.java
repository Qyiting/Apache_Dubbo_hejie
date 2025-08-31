package com.hejiexmu.rpc.auth.filter;

import com.hejiexmu.rpc.auth.service.JwtTokenService;
import com.hejiexmu.rpc.auth.service.UserDetailsServiceImpl;
import com.hejiexmu.rpc.auth.util.SecurityContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 处理JWT令牌的验证和用户认证
 * 
 * @author hejiexmu
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SESSION_ID_HEADER = "X-Session-Id";
    
    private final JwtTokenService jwtTokenService;
    private final UserDetailsServiceImpl userDetailsService;
    
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                 UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 提取JWT令牌
            String jwt = extractJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenService.validateToken(jwt)) {
                // 从JWT中获取用户名
                String username = jwtTokenService.getUsernameFromToken(jwt);
                
                // 检查当前安全上下文中是否已有认证信息
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // 加载用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // 验证令牌是否有效
                    if (jwtTokenService.validateToken(jwt, userDetails)) {
                        
                        // 检查会话是否有效
                        String sessionId = extractSessionIdFromRequest(request);
                        if (sessionId != null && !jwtTokenService.isSessionValid(sessionId, username)) {
                            logger.warn("Invalid session for user: {}, sessionId: {}", username, sessionId);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        
                        // 创建认证令牌
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        
                        // 设置认证详情
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        // 设置用户上下文信息
                        SecurityContextUtil.setCurrentUser(username);
                        SecurityContextUtil.setCurrentSessionId(sessionId);
                        SecurityContextUtil.setCurrentClientIp(getClientIpAddress(request));
                        SecurityContextUtil.setCurrentUserAgent(request.getHeader("User-Agent"));
                        
                        logger.debug("Successfully authenticated user: {}", username);
                    } else {
                        logger.warn("Invalid JWT token for user: {}", username);
                    }
                } else if (username == null) {
                    logger.warn("Username is null from JWT token");
                }
            } else if (StringUtils.hasText(jwt)) {
                logger.warn("Invalid JWT token format or expired");
            }
            
        } catch (Exception ex) {
            logger.error("Cannot set user authentication: {}", ex.getMessage(), ex);
            // 清除安全上下文
            SecurityContextHolder.clearContext();
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理线程本地变量
            SecurityContextUtil.clear();
        }
    }
    
    /**
     * 从请求中提取JWT令牌
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        // 也可以从查询参数中获取令牌（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * 从请求中提取会话ID
     */
    private String extractSessionIdFromRequest(HttpServletRequest request) {
        // 首先从请求头中获取
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        
        // 也可以从查询参数中获取
        sessionId = request.getParameter("sessionId");
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        
        return null;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            // X-Forwarded-For可能包含多个IP，取第一个
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        String xClientIp = request.getHeader("X-Client-IP");
        if (StringUtils.hasText(xClientIp)) {
            return xClientIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 判断是否应该跳过过滤器
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // 跳过公开接口
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/auth/captcha") ||
               path.startsWith("/api/auth/forgot-password") ||
               path.startsWith("/api/auth/reset-password") ||
               path.startsWith("/api/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico") ||
               path.equals("/error");
    }
}