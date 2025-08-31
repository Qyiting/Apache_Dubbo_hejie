package com.hejiexmu.rpc.auth.filter;

import com.hejiexmu.rpc.auth.interceptor.RpcAuthInterceptor;
import com.hejiexmu.rpc.auth.dto.RpcRequest;
import com.hejiexmu.rpc.auth.dto.RpcResponse;
import com.hejiexmu.rpc.auth.exception.BusinessException;
import com.hejiexmu.rpc.auth.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC认证过滤器
 * 集成到RPC框架中，对所有RPC请求进行认证和权限检查
 * 
 * @author hejiexmu
 */
@Component
@Order(1) // 确保认证过滤器优先执行
public class RpcAuthFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcAuthFilter.class);
    
    private final RpcAuthInterceptor authInterceptor;
    
    // 请求上下文存储
    private final ThreadLocal<RequestContext> requestContextHolder = new ThreadLocal<>();
    
    // 静态实例，用于静态方法访问
    private static RpcAuthFilter instance;
    
    public RpcAuthFilter(RpcAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
        RpcAuthFilter.instance = this;
    }
    
    /**
     * 过滤RPC请求
     */
    public RpcResponse filter(RpcRequest request, Map<String, String> headers, FilterChain chain) {
        RequestContext context = new RequestContext(request, headers);
        requestContextHolder.set(context);
        
        try {
            // 前置认证处理
            if (!authInterceptor.preHandle(request, headers)) {
                logger.warn("RPC认证失败: service={}, method={}", request.getServiceName(), request.getMethodName());
                return createErrorResponse("UNAUTHORIZED", "认证失败");
            }
            
            // 执行RPC调用
            RpcResponse response = chain.doFilter(request, headers);
            
            // 后置处理
            authInterceptor.postHandle(request, response, headers);
            
            return response;
            
        } catch (RpcException e) {
            logger.warn("RPC认证异常: service={}, method={}, error={}", 
                       request.getServiceName(), request.getMethodName(), e.getMessage());
            
            RpcResponse errorResponse = createErrorResponse(e.getCode(), e.getMessage());
            authInterceptor.afterCompletion(request, errorResponse, e, headers);
            
            return errorResponse;
            
        } catch (Exception e) {
            logger.error("RPC过滤器异常: service={}, method={}", 
                        request.getServiceName(), request.getMethodName(), e);
            
            RpcResponse errorResponse = createErrorResponse("INTERNAL_ERROR", "服务内部错误");
            authInterceptor.afterCompletion(request, errorResponse, e, headers);
            
            return errorResponse;
            
        } finally {
            // 清理上下文
            requestContextHolder.remove();
        }
    }
    
    /**
     * 获取当前请求上下文
     */
    public RequestContext getCurrentContext() {
        return requestContextHolder.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        if (instance == null) return null;
        RequestContext context = instance.getCurrentContext();
        if (context != null && context.getRequest().getHeaders() != null) {
            String userIdStr = context.getRequest().getHeaders().get("userId");
            if (userIdStr != null) {
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
     * 获取当前会话ID
     */
    public static String getCurrentSessionId() {
        if (instance == null) return null;
        RequestContext context = instance.getCurrentContext();
        if (context != null && context.getRequest().getHeaders() != null) {
            return context.getRequest().getHeaders().get("sessionId");
        }
        return null;
    }
    
    /**
     * 获取当前客户端IP
     */
    public static String getCurrentClientIp() {
        if (instance == null) return null;
        RequestContext context = instance.getCurrentContext();
        if (context != null) {
            Map<String, String> headers = context.getHeaders();
            String clientIp = headers.get("X-Client-IP");
            if (clientIp == null) {
                clientIp = headers.get("X-Forwarded-For");
            }
            if (clientIp == null) {
                clientIp = headers.get("X-Real-IP");
            }
            return clientIp;
        }
        return null;
    }
    
    /**
     * 创建错误响应
     */
    private RpcResponse createErrorResponse(String code, String message) {
        RpcResponse response = new RpcResponse();
        response.setException(new BusinessException(message));
        response.setSuccess(false);
        response.setErrorMessage(message);
        return response;
    }
    
    /**
     * 过滤器链接口
     */
    public interface FilterChain {
        RpcResponse doFilter(RpcRequest request, Map<String, String> headers) throws Exception;
    }
    
    /**
     * 请求上下文类
     */
    public static class RequestContext {
        private final RpcRequest request;
        private final Map<String, String> headers;
        private final long startTime;
        private final Map<String, Object> attributes;
        
        public RequestContext(RpcRequest request, Map<String, String> headers) {
            this.request = request;
            this.headers = headers;
            this.startTime = System.currentTimeMillis();
            this.attributes = new ConcurrentHashMap<>();
        }
        
        public RpcRequest getRequest() {
            return request;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
        
        public Object getAttribute(String key) {
            return attributes.get(key);
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String key, Class<T> type) {
            Object value = attributes.get(key);
            if (value != null && type.isInstance(value)) {
                return (T) value;
            }
            return null;
        }
    }
}