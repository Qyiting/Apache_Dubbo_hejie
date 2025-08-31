package com.hejiexmu.rpc.auth.controller;

import com.hejiexmu.rpc.auth.dto.*;
import com.hejiexmu.rpc.auth.service.UserService;
import com.hejiexmu.rpc.auth.util.SecurityContextUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户管理控制器
 * 
 * @author hejiexmu
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取用户列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'read')")
    public ResponseEntity<ApiResponse<PageResponse<UserDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean enabled) {
        
        try {
            PageResponse<UserDTO> users = userService.findUsers(page, size, username, email, enabled);
            
            logger.info("获取用户列表成功: page={}, size={}, total={}", page, size, users.getTotalElements());
            
            return ResponseEntity.ok(ApiResponse.success(users));
            
        } catch (Exception e) {
            logger.error("获取用户列表失败: error={}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'read') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserDetails(id);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.notFoundError("用户不存在"));
            }
            
            logger.info("获取用户信息成功: userId={}", id);
            
            return ResponseEntity.ok(ApiResponse.success(user));
            
        } catch (Exception e) {
            logger.error("获取用户信息失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'create')")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {
        
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
            
            UserDTO user = userService.createUser(request);
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("创建用户成功: username={}, createdBy={}", user.getUsername(), currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("用户创建成功", user));
            
        } catch (Exception e) {
            logger.error("创建用户失败: username={}, error={}", request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO,
            BindingResult bindingResult) {
        
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
            
            userDTO.setId(id);
            UserDTO updatedUser = userService.updateUser(userDTO);
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("更新用户成功: userId={}, updatedBy={}", id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("用户更新成功", updatedUser));
            
        } catch (Exception e) {
            logger.error("更新用户失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            // 不能删除自己
            Long currentUserId = SecurityContextUtil.getCurrentUserId();
            if (id.equals(currentUserId)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("不能删除自己的账户"));
            }
            
            boolean deleted = userService.deleteUser(id);
            if (!deleted) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("用户删除失败"));
            }
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("删除用户成功: userId={}, deletedBy={}", id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success());
            
        } catch (Exception e) {
            logger.error("删除用户失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 启用/禁用用户
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update')")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        
        try {
            Boolean enabled = request.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("enabled参数不能为空"));
            }
            
            // 不能禁用自己
            Long currentUserId = SecurityContextUtil.getCurrentUserId();
            if (id.equals(currentUserId) && !enabled) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("不能禁用自己的账户"));
            }
            
            if (enabled) {
                userService.enableUser(id);
            } else {
                userService.disableUser(id);
            }
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            String action = enabled ? "启用" : "禁用";
            logger.info("{}用户成功: userId={}, operatedBy={}", action, id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success(action + "用户成功", null));
            
        } catch (Exception e) {
            logger.error("更新用户状态失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 锁定/解锁用户
     */
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update')")
    public ResponseEntity<ApiResponse<Void>> updateUserLockStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        
        try {
            Boolean locked = request.get("locked");
            if (locked == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("locked参数不能为空"));
            }
            
            // 不能锁定自己
            Long currentUserId = SecurityContextUtil.getCurrentUserId();
            if (id.equals(currentUserId) && locked) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.businessError("不能锁定自己的账户"));
            }
            
            if (locked) {
                userService.lockUser(id);
            } else {
                userService.unlockUser(id);
            }
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            String action = locked ? "锁定" : "解锁";
            logger.info("{}用户成功: userId={}, operatedBy={}", action, id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success(action + "用户成功", null));
            
        } catch (Exception e) {
            logger.error("更新用户锁定状态失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update')")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@PathVariable Long id) {
        try {
            String newPassword = userService.resetPassword(id);
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("重置用户密码成功: userId={}, operatedBy={}", id, currentUser);
            
            Map<String, String> result = Map.of("newPassword", newPassword);
            return ResponseEntity.ok(ApiResponse.success("密码重置成功", result));
            
        } catch (Exception e) {
            logger.error("重置用户密码失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 获取用户角色
     */
    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'read') or #id == authentication.principal.id")
    public ResponseEntity<ApiResponse<Set<String>>> getUserRoles(@PathVariable Long id) {
        try {
            Set<String> roles = userService.getUserRoles(id);
            
            logger.info("获取用户角色成功: userId={}, roleCount={}", id, roles.size());
            
            return ResponseEntity.ok(ApiResponse.success(roles));
            
        } catch (Exception e) {
            logger.error("获取用户角色失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 分配用户角色
     */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update')")
    public ResponseEntity<ApiResponse<Void>> assignRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<String>> request) {
        
        try {
            List<String> roleNames = request.get("roleIds");
            if (roleNames == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError("roleIds参数不能为空"));
            }
            
            userService.assignRoles(id, new HashSet<>(roleNames));
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("分配用户角色成功: userId={}, roleIds={}, operatedBy={}", id, roleNames, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success("角色分配成功", null));
            
        } catch (Exception e) {
            logger.error("分配用户角色失败: userId={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
    
    /**
     * 移除用户角色
     */
    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN') or hasPermission('user', 'update')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable Long id,
            @PathVariable String roleId) {
        
        try {
            userService.removeRole(id, roleId);
            
            String currentUser = SecurityContextUtil.getCurrentUsername();
            logger.info("移除用户角色成功: userId={}, roleId={}, operatedBy={}", id, roleId, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success("角色移除成功", null));
            
        } catch (Exception e) {
            logger.error("移除用户角色失败: userId={}, roleId={}, error={}", id, roleId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.businessError(e.getMessage()));
        }
    }
}