package com.hejiexmu.rpc.auth.controller;

import com.hejiexmu.rpc.auth.dto.*;
import com.hejiexmu.rpc.auth.service.UserService;
import com.hejiexmu.rpc.auth.util.SecurityContextUtil;
import com.hejiexmu.rpc.auth.exception.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author hejiexmu
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        
        try {
            // 验证请求参数
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("参数验证失败");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(errorMessage));
            }
            
            // 设置客户端信息
            request.setIpAddress(getClientIpAddress(httpRequest));
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            // 执行登录
            AuthResponse authResponse = authService.login(request);
            
            logger.info("用户登录成功: username={}, ip={}", request.getUsername(), request.getIpAddress());
            
            return ResponseEntity.ok(ApiResponse.success("登录成功", authResponse));
            
        } catch (Exception e) {
            logger.error("用户登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.authError(e.getMessage()));
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest) {
        
        try {
            // 验证请求参数
            if (bindingResult.hasErrors()) {
                String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("参数验证失败");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(errorMessage));
            }
            
            // 设置客户端信息
            request.setClientIp(getClientIpAddress(httpRequest));
            request.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            // 执行注册
            AuthResponse authResponse = authService.register(request);
            
            if (!authResponse.isSuccess()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError(authResponse.getMessage()));
            }
            
            // 从AuthResponse中提取用户信息转换为UserDTO
            UserDTO userDTO = new UserDTO();
            userDTO.setId(authResponse.getUserInfo().getUserId());
            userDTO.setUsername(authResponse.getUserInfo().getUsername());
            userDTO.setEmail(authResponse.getUserInfo().getEmail());
            userDTO.setRealName(authResponse.getUserInfo().getRealName());
            userDTO.setPhone(authResponse.getUserInfo().getPhone());
            userDTO.setCreatedAt(authResponse.getUserInfo().getCreatedAt());
            userDTO.setLastLoginAt(authResponse.getUserInfo().getLastLoginAt());
            userDTO.setStatus(authResponse.getUserInfo().getStatus());
            userDTO.setRoles(authResponse.getRoles());
            userDTO.setPermissions(authResponse.getPermissions());
            
            logger.info("用户注册成功: username={}, email={}", request.getUsername(), request.getEmail());
            
            return ResponseEntity.ok(ApiResponse.success("注册成功", userDTO));
            
        } catch (Exception e) {
            logger.error("用户注册失败: username={}, error={}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String refreshToken = request.get("refreshToken");
            
            if (!StringUtils.hasText(refreshToken)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("刷新令牌不能为空"));
            }
            
            // 执行令牌刷新
            AuthResponse authResponse = authService.refreshToken(refreshToken);
            
            logger.info("令牌刷新成功");
            
            return ResponseEntity.ok(ApiResponse.success("令牌刷新成功", authResponse));
            
        } catch (Exception e) {
            logger.error("令牌刷新失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.authError(e.getMessage()));
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        try {
            String username = SecurityContextUtil.getCurrentUsername();
            
            // 获取当前会话ID（这里需要从SecurityContext或请求中获取）
            String sessionId = SecurityContextUtil.getCurrentSessionId();
            if (sessionId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("无效的会话"));
            }
            
            // 执行登出
            boolean success = authService.logout(sessionId);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("登出失败"));
            }
            
            logger.info("用户登出成功: username={}, sessionId={}", username, sessionId);
            
            return ResponseEntity.ok(ApiResponse.<Void>success("登出成功", null));
            
        } catch (Exception e) {
            logger.error("用户登出失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> request) {
        
        try {
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            
            if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("原密码和新密码不能为空"));
            }
            
            // 获取当前用户ID
            Long userId = SecurityContextUtil.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("无法获取当前用户信息"));
            }
            
            // 执行密码修改
            boolean success = authService.changePassword(userId, oldPassword, newPassword);
            if (!success) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("密码修改失败，请检查原密码是否正确"));
            }
            
            String username = SecurityContextUtil.getCurrentUsername();
            logger.info("密码修改成功: username={}", username);
            
            return ResponseEntity.ok(ApiResponse.<Void>success("密码修改成功", null));
            
        } catch (Exception e) {
            logger.error("密码修改失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            Long userId = SecurityContextUtil.getCurrentUserId();
            String username = SecurityContextUtil.getCurrentUsername();
            String email = SecurityContextUtil.getCurrentUserEmail();
            String realName = SecurityContextUtil.getCurrentUserFullName();
            
            UserDTO userDTO = new UserDTO();
            userDTO.setId(userId);
            userDTO.setUsername(username);
            userDTO.setEmail(email);
            userDTO.setRealName(realName);
            
            return ResponseEntity.ok(ApiResponse.success(userDTO));
            
        } catch (Exception e) {
            logger.error("获取当前用户信息失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 验证令牌有效性
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken() {
        try {
            boolean isAuthenticated = SecurityContextUtil.isAuthenticated();
            
            if (isAuthenticated) {
                Map<String, Object> result = Map.of(
                    "valid", true,
                    "userId", SecurityContextUtil.getCurrentUserId(),
                    "username", SecurityContextUtil.getCurrentUsername(),
                    "sessionId", SecurityContextUtil.getCurrentSessionId()
                );
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                Map<String, Object> result = Map.of("valid", false);
                return ResponseEntity.ok(ApiResponse.success(result));
            }
            
        } catch (Exception e) {
            logger.error("令牌验证失败: error={}", e.getMessage());
            Map<String, Object> result = Map.of("valid", false, "error", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(result));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        Map<String, String> status = Map.of(
            "status", "UP",
            "service", "auth-service",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}