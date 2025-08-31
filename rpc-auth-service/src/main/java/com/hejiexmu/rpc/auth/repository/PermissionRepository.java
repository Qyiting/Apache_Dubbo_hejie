package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 权限数据访问接口
 * 
 * @author hejiexmu
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    /**
     * 根据权限名查找权限
     */
    Optional<Permission> findByPermissionName(String permissionName);
    
    /**
     * 检查权限名是否存在
     */
    boolean existsByPermissionName(String permissionName);
    
    /**
     * 根据资源查找权限
     */
    List<Permission> findByResource(String resource);
    
    /**
     * 根据操作查找权限
     */
    List<Permission> findByAction(String action);
    
    /**
     * 根据资源和操作查找权限
     */
    Optional<Permission> findByResourceAndAction(String resource, String action);
    
    /**
     * 根据权限名列表查找权限
     */
    @Query("SELECT p FROM Permission p WHERE p.permissionName IN :permissionNames")
    List<Permission> findByPermissionNames(@Param("permissionNames") Set<String> permissionNames);
    
    /**
     * 查找角色拥有的权限
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找角色拥有的权限名称
     */
    @Query("SELECT p.permissionName FROM Permission p JOIN p.roles r WHERE r.id = :roleId")
    Set<String> findPermissionNamesByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 查找用户拥有的权限
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId")
    List<Permission> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查找用户拥有的权限名称
     */
    @Query("SELECT DISTINCT p.permissionName FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId")
    Set<String> findPermissionNamesByUserId(@Param("userId") Long userId);
    
    /**
     * 根据资源列表查找权限
     */
    @Query("SELECT p FROM Permission p WHERE p.resource IN :resources")
    List<Permission> findByResources(@Param("resources") Set<String> resources);
    
    /**
     * 统计权限总数
     */
    @Query("SELECT COUNT(p) FROM Permission p")
    long countTotalPermissions();
    
    /**
     * 统计指定资源的权限数
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.resource = :resource")
    long countPermissionsByResource(@Param("resource") String resource);
    
    /**
     * 根据权限名模糊查询
     */
    @Query("SELECT p FROM Permission p WHERE p.permissionName LIKE %:keyword% OR p.description LIKE %:keyword%")
    List<Permission> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 查找所有资源列表
     */
    @Query("SELECT DISTINCT p.resource FROM Permission p ORDER BY p.resource")
    List<String> findAllResources();
    
    /**
     * 查找所有操作列表
     */
    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findAllActions();
}