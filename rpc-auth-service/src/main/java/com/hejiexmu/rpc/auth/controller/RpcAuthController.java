package com.hejiexmu.rpc.auth.controller;

import com.hejiexmu.rpc.auth.dto.*;
import com.hejiexmu.rpc.auth.service.AuthService;
import com.hejiexmu.rpc.auth.service.PermissionService;
import com.hejiexmu.rpc.auth.service.JwtTokenService;
import com.hejiexmu.rpc.auth.util.SecurityContextUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RPC认证控制器
 * 用于处理内部RPC调用的认证和权限验证
 * 
 * @author hejiexmu
 */
@RestController
@RequestMapping("/rpc/auth")
public class RpcAuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcAuthController.class);
    
    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private AuthService authService;
    
    /**
     * 验证JWT令牌
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("valid", false, "reason", "Token is empty")));
            }
            
            // 验证令牌
            boolean isValid = jwtTokenService.validateToken(token);
            
            if (isValid) {
                // 获取用户信息
                Long userId = jwtTokenService.getUserIdFromToken(token);
                String username = jwtTokenService.getUsernameFromToken(token);
                
                Map<String, Object> result = Map.of(
                    "valid", true,
                    "userId", userId,
                    "username", username,
                    "tokenType", "Bearer"
                );
                
                logger.debug("令牌验证成功: userId={}, username={}", userId, username);
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                logger.debug("令牌验证失败: token无效或已过期");
                return ResponseEntity.ok(ApiResponse.success(Map.of("valid", false, "reason", "Token is invalid or expired")));
            }
            
        } catch (Exception e) {
            logger.error("令牌验证异常: error={}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(Map.of("valid", false, "reason", "Token validation error: " + e.getMessage())));
        }
    }
    
    /**
     * 检查用户权限
     */
    @PostMapping("/check-permission")
    public ResponseEntity<ApiResponse<PermissionCheckResponse>> checkPermission(
            @RequestBody PermissionCheckRequest request) {
        
        try {
            boolean hasPermission = false;
            String reason = "";
            
            // 检查权限
            if (request.getPermissionCode() != null) {
                hasPermission = permissionService.hasPermissionByCode(request.getUserId(), request.getPermissionCode());
                reason = hasPermission ? "Permission granted" : "Permission denied by code";
            } else if (request.getResource() != null && request.getAction() != null) {
                hasPermission = permissionService.hasPermission(request.getUserId(), request.getResource(), request.getAction());
                reason = hasPermission ? "Permission granted" : "Permission denied by resource and action";
            } else {
                reason = "Invalid permission check request";
            }
            
            PermissionCheckResponse response = new PermissionCheckResponse();
            response.setUserId(request.getUserId());
            response.setHasPermission(hasPermission);
            response.setReason(reason);
            response.setTimestamp(System.currentTimeMillis());
            
            logger.debug("权限检查完成: userId={}, hasPermission={}, reason={}", 
                request.getUserId(), hasPermission, reason);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            logger.error("权限检查异常: userId={}, error={}", request.getUserId(), e.getMessage());
            
            PermissionCheckResponse response = new PermissionCheckResponse();
            response.setUserId(request.getUserId());
            response.setHasPermission(false);
            response.setReason("Permission check error: " + e.getMessage());
            response.setTimestamp(System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }
    
    /**
     * 获取用户权限列表
     */
    @GetMapping("/user/{userId}/permissions")
    public ResponseEntity<ApiResponse<List<String>>> getUserPermissions(@PathVariable Long userId) {
        try {
            List<String> permissions = permissionService.getUserPermissionCodes(userId);
            
            logger.debug("获取用户权限成功: userId={}, permissionCount={}", userId, permissions.size());
            
            return ResponseEntity.ok(ApiResponse.success(permissions));
            
        } catch (Exception e) {
            logger.error("获取用户权限失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 获取用户角色列表
     */
    @GetMapping("/user/{userId}/roles")
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles(@PathVariable Long userId) {
        try {
            List<String> roles = new ArrayList<>(permissionService.getUserRoles(userId));
            
            logger.debug("获取用户角色成功: userId={}, roleCount={}", userId, roles.size());
            
            return ResponseEntity.ok(ApiResponse.success(roles));
            
        } catch (Exception e) {
            logger.error("获取用户角色失败: userId={}, error={}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 批量验证令牌
     */
    @PostMapping("/batch-validate")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> batchValidateTokens(
            @RequestBody Map<String, List<String>> request) {
        
        try {
            List<String> tokens = request.get("tokens");
            if (tokens == null || tokens.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("tokens参数不能为空"));
            }
            
            Map<String, Map<String, Object>> results = new java.util.HashMap<>();
            
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                String key = "token_" + i;
                
                try {
                    boolean isValid = jwtTokenService.validateToken(token);
                    
                    if (isValid) {
                        Long userId = jwtTokenService.getUserIdFromToken(token);
                        String username = jwtTokenService.getUsernameFromToken(token);
                        
                        results.put(key, Map.of(
                            "valid", true,
                            "userId", userId,
                            "username", username
                        ));
                    } else {
                        results.put(key, Map.of("valid", false, "reason", "Invalid token"));
                    }
                } catch (Exception e) {
                    results.put(key, Map.of("valid", false, "reason", "Validation error: " + e.getMessage()));
                }
            }
            
            logger.debug("批量令牌验证完成: tokenCount={}", tokens.size());
            
            return ResponseEntity.ok(ApiResponse.success(results));
            
        } catch (Exception e) {
            logger.error("批量令牌验证失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 使令牌失效
     */
    @PostMapping("/invalidate-token")
    public ResponseEntity<ApiResponse<Void>> invalidateToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String token = request.get("token");
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("token参数不能为空"));
            }
            
            // 将令牌加入黑名单
            jwtTokenService.blacklistToken(token);
            
            logger.info("令牌已失效");
            
            return ResponseEntity.ok(ApiResponse.<Void>success("令牌已失效", null));
            
        } catch (Exception e) {
            logger.error("令牌失效失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 获取在线用户数量
     */
    @GetMapping("/online-users/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOnlineUserCount() {
        try {
            // 这里可以通过Redis或数据库查询在线用户数量
            // 暂时返回模拟数据
            Map<String, Object> result = Map.of(
                "onlineCount", 0,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("获取在线用户数量失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> status = Map.of(
            "status", "UP",
            "service", "rpc-auth-service",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}