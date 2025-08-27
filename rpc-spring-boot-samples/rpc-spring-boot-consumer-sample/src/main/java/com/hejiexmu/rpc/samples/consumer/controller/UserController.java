package com.hejiexmu.rpc.samples.consumer.controller;

import com.hejiexmu.rpc.samples.api.entity.User;
import com.hejiexmu.rpc.samples.api.service.UserService;
import com.hejiexmu.rpc.spring.boot.annotation.RpcReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器 - 演示RPC服务消费
 * 
 * @author hejiexmu
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    

    
    // 使用@RpcReference注解注入RPC服务
    @RpcReference(interfaceClass = UserService.class, version = "1.0.0", group = "default")
    private UserService userService;
    
    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("REST请求：获取用户ID={}", id);
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取用户失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        log.info("REST请求：获取用户名={}", username);
        try {
            User user = userService.getUserByUsername(username);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取用户失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("REST请求：获取所有用户");
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("获取用户列表失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据年龄范围获取用户
     */
    @GetMapping("/age-range")
    public ResponseEntity<List<User>> getUsersByAgeRange(
            @RequestParam int minAge, 
            @RequestParam int maxAge) {
        log.info("REST请求：获取年龄范围用户 {}-{}", minAge, maxAge);
        try {
            List<User> users = userService.getUsersByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("获取用户列表失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        log.info("REST请求：创建用户={}", user.getUsername());
        try {
            Long userId = userService.createUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("message", "用户创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建用户失败：{}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id, 
            @RequestBody User user) {
        log.info("REST请求：更新用户ID={}", id);
        try {
            user.setId(id);
            boolean success = userService.updateUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "用户更新成功" : "用户不存在");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("更新用户失败：{}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        log.info("REST请求：删除用户ID={}", id);
        try {
            boolean success = userService.deleteUser(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "用户删除成功" : "用户不存在");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("删除用户失败：{}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    @GetMapping("/exists/{username}")
    public ResponseEntity<Map<String, Object>> existsByUsername(@PathVariable String username) {
        log.info("REST请求：检查用户名是否存在={}", username);
        try {
            boolean exists = userService.existsByUsername(username);
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("username", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查用户名失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取用户总数
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        log.info("REST请求：获取用户总数");
        try {
            long count = userService.getUserCount();
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户总数失败：{}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量创建用户
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchCreateUsers(@RequestBody List<User> users) {
        log.info("REST请求：批量创建用户，数量={}", users.size());
        try {
            int successCount = userService.batchCreateUsers(users);
            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", users.size());
            response.put("successCount", successCount);
            response.put("failedCount", users.size() - successCount);
            response.put("message", String.format("批量创建完成，成功：%d，失败：%d", 
                    successCount, users.size() - successCount));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量创建用户失败：{}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}