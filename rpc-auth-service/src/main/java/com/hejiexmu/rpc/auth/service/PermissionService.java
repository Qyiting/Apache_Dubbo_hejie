package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.dto.PermissionCheckRequest;
import com.hejiexmu.rpc.auth.dto.PermissionCheckResponse;
import com.hejiexmu.rpc.auth.entity.Permission;
import com.hejiexmu.rpc.auth.entity.Role;
import com.hejiexmu.rpc.auth.entity.RpcServicePermission;
import com.hejiexmu.rpc.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 权限服务接口
 * 
 * @author hejiexmu
 */
public interface PermissionService {
    
    /**
     * 检查用户权限
     */
    PermissionCheckResponse checkPermission(PermissionCheckRequest request);
    
    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(Long userId, String resource, String action);
    
    /**
     * 检查用户是否有指定权限（通过权限名称）
     */
    boolean hasPermission(Long userId, String permissionName);
    
    /**
     * 检查用户是否有指定权限（通过权限代码）
     */
    boolean hasPermissionByCode(Long userId, String permissionCode);
    
    /**
     * 检查用户是否有任意一个权限
     */
    boolean hasAnyPermission(Long userId, Set<String> permissions);
    
    /**
     * 检查用户是否有所有权限
     */
    boolean hasAllPermissions(Long userId, Set<String> permissions);
    
    /**
     * 检查用户是否可以访问RPC服务
     */
    boolean canAccessRpcService(Long userId, String serviceName, String methodName);
    
    /**
     * 批量检查用户权限
     */
    Map<String, Boolean> batchCheckPermissions(Long userId, Set<String> permissions);
    
    /**
     * 批量检查RPC服务权限
     */
    Map<String, Boolean> batchCheckRpcPermissions(Long userId, Map<String, String> serviceMethodMap);
    
    /**
     * 获取用户所有权限
     */
    Set<String> getUserPermissions(Long userId);
    
    /**
     * 获取用户权限代码列表
     */
    List<String> getUserPermissionCodes(Long userId);
    
    /**
     * 获取用户所有角色
     */
    Set<String> getUserRoles(Long userId);
    
    /**
     * 获取用户权限详细信息
     */
    List<Permission> getUserPermissionDetails(Long userId);
    
    /**
     * 获取用户角色详细信息
     */
    List<Role> getUserRoleDetails(Long userId);
    
    /**
     * 获取用户可访问的RPC服务
     */
    List<RpcServicePermission> getUserAccessibleRpcServices(Long userId);
    
    /**
     * 创建权限
     */
    Permission createPermission(String name, String resource, String action, String description);
    
    /**
     * 更新权限
     */
    Permission updatePermission(Long permissionId, String name, String resource, String action, String description);
    
    /**
     * 删除权限
     */
    boolean deletePermission(Long permissionId);
    
    /**
     * 根据ID查找权限
     */
    Optional<Permission> findPermissionById(Long permissionId);
    
    /**
     * 根据名称查找权限
     */
    Optional<Permission> findPermissionByName(String name);
    
    /**
     * 根据资源和动作查找权限
     */
    Optional<Permission> findPermissionByResourceAndAction(String resource, String action);
    
    /**
     * 分页查询所有权限
     */
    Page<Permission> findAllPermissions(Pageable pageable);
    
    /**
     * 根据关键字搜索权限
     */
    Page<Permission> searchPermissions(String keyword, Pageable pageable);
    
    /**
     * 获取所有资源列表
     */
    List<String> getAllResources();
    
    /**
     * 获取所有动作列表
     */
    List<String> getAllActions();
    
    /**
     * 获取指定资源的所有动作
     */
    List<String> getResourceActions(String resource);
    
    /**
     * 创建角色
     */
    Role createRole(String name, String description);
    
    /**
     * 更新角色
     */
    Role updateRole(Long roleId, String name, String description);
    
    /**
     * 删除角色
     */
    boolean deleteRole(Long roleId);
    
    /**
     * 根据ID查找角色
     */
    Optional<Role> findRoleById(Long roleId);
    
    /**
     * 根据名称查找角色
     */
    Optional<Role> findRoleByName(String name);
    
    /**
     * 分页查询所有角色
     */
    Page<Role> findAllRoles(Pageable pageable);
    
    /**
     * 根据关键字搜索角色
     */
    Page<Role> searchRoles(String keyword, Pageable pageable);
    
    /**
     * 为角色分配权限
     */
    boolean assignPermissionToRole(Long roleId, Long permissionId);
    
    /**
     * 为角色批量分配权限
     */
    boolean assignPermissionsToRole(Long roleId, Set<Long> permissionIds);
    
    /**
     * 从角色移除权限
     */
    boolean removePermissionFromRole(Long roleId, Long permissionId);
    
    /**
     * 从角色批量移除权限
     */
    boolean removePermissionsFromRole(Long roleId, Set<Long> permissionIds);
    
    /**
     * 获取角色的所有权限
     */
    List<Permission> getRolePermissions(Long roleId);
    
    /**
     * 为用户分配角色
     */
    boolean assignRoleToUser(Long userId, Long roleId);
    
    /**
     * 为用户批量分配角色
     */
    boolean assignRolesToUser(Long userId, Set<Long> roleIds);
    
    /**
     * 从用户移除角色
     */
    boolean removeRoleFromUser(Long userId, Long roleId);
    
    /**
     * 从用户批量移除角色
     */
    boolean removeRolesFromUser(Long userId, Set<Long> roleIds);
    
    /**
     * 为用户直接分配权限
     */
    boolean assignPermissionToUser(Long userId, Long permissionId);
    
    /**
     * 为用户批量分配权限
     */
    boolean assignPermissionsToUser(Long userId, Set<Long> permissionIds);
    
    /**
     * 从用户移除直接权限
     */
    boolean removePermissionFromUser(Long userId, Long permissionId);
    
    /**
     * 从用户批量移除直接权限
     */
    boolean removePermissionsFromUser(Long userId, Set<Long> permissionIds);
    
    /**
     * 获取用户的直接权限（不包括角色权限）
     */
    List<Permission> getUserDirectPermissions(Long userId);
    
    /**
     * 创建RPC服务权限
     */
    RpcServicePermission createRpcServicePermission(String serviceName, String methodName, String requiredPermission, String description);
    
    /**
     * 更新RPC服务权限
     */
    RpcServicePermission updateRpcServicePermission(Long id, String serviceName, String methodName, String requiredPermission, String description);
    
    /**
     * 删除RPC服务权限
     */
    boolean deleteRpcServicePermission(Long id);
    
    /**
     * 根据服务名和方法名查找RPC服务权限
     */
    Optional<RpcServicePermission> findRpcServicePermission(String serviceName, String methodName);
    
    /**
     * 分页查询所有RPC服务权限
     */
    Page<RpcServicePermission> findAllRpcServicePermissions(Pageable pageable);
    
    /**
     * 根据服务名查询RPC服务权限
     */
    List<RpcServicePermission> findRpcServicePermissionsByService(String serviceName);
    
    /**
     * 获取所有RPC服务名称
     */
    List<String> getAllRpcServiceNames();
    
    /**
     * 获取指定服务的所有方法名称
     */
    List<String> getRpcServiceMethods(String serviceName);
    
    /**
     * 检查权限是否存在
     */
    boolean permissionExists(String name);
    
    /**
     * 检查权限是否存在（通过资源和动作）
     */
    boolean permissionExists(String resource, String action);
    
    /**
     * 检查角色是否存在
     */
    boolean roleExists(String name);
    
    /**
     * 检查RPC服务权限是否存在
     */
    boolean rpcServicePermissionExists(String serviceName, String methodName);
    
    /**
     * 获取权限统计信息
     */
    PermissionStatistics getPermissionStatistics();
    
    /**
     * 清理未使用的权限
     */
    int cleanupUnusedPermissions();
    
    /**
     * 清理未使用的角色
     */
    int cleanupUnusedRoles();
    
    /**
     * 验证权限配置
     */
    List<String> validatePermissionConfiguration();
    
    /**
     * 导出权限配置
     */
    String exportPermissionConfiguration();
    
    /**
     * 导入权限配置
     */
    boolean importPermissionConfiguration(String configuration);
    
    /**
     * 权限统计信息内部类
     */
    class PermissionStatistics {
        private long totalPermissions;
        private long totalRoles;
        private long totalRpcServicePermissions;
        private long usersWithRoles;
        private long usersWithDirectPermissions;
        private long averagePermissionsPerUser;
        private long averageRolesPerUser;
        
        // Getters and Setters
        public long getTotalPermissions() {
            return totalPermissions;
        }
        
        public void setTotalPermissions(long totalPermissions) {
            this.totalPermissions = totalPermissions;
        }
        
        public long getTotalRoles() {
            return totalRoles;
        }
        
        public void setTotalRoles(long totalRoles) {
            this.totalRoles = totalRoles;
        }
        
        public long getTotalRpcServicePermissions() {
            return totalRpcServicePermissions;
        }
        
        public void setTotalRpcServicePermissions(long totalRpcServicePermissions) {
            this.totalRpcServicePermissions = totalRpcServicePermissions;
        }
        
        public long getUsersWithRoles() {
            return usersWithRoles;
        }
        
        public void setUsersWithRoles(long usersWithRoles) {
            this.usersWithRoles = usersWithRoles;
        }
        
        public long getUsersWithDirectPermissions() {
            return usersWithDirectPermissions;
        }
        
        public void setUsersWithDirectPermissions(long usersWithDirectPermissions) {
            this.usersWithDirectPermissions = usersWithDirectPermissions;
        }
        
        public long getAveragePermissionsPerUser() {
            return averagePermissionsPerUser;
        }
        
        public void setAveragePermissionsPerUser(long averagePermissionsPerUser) {
            this.averagePermissionsPerUser = averagePermissionsPerUser;
        }
        
        public long getAverageRolesPerUser() {
            return averageRolesPerUser;
        }
        
        public void setAverageRolesPerUser(long averageRolesPerUser) {
            this.averageRolesPerUser = averageRolesPerUser;
        }
    }
}