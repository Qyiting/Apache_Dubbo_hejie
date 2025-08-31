 package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 角色数据访问接口
 * 
 * @author hejiexmu
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    /**
     * 根据角色名查找角色
     */
    Optional<Role> findByRoleName(String roleName);
    
    /**
     * 检查角色名是否存在
     */
    boolean existsByRoleName(String roleName);
    
    /**
     * 根据状态查找角色
     */
    List<Role> findByStatus(Integer status);
    
    /**
     * 查找激活的角色
     */
    @Query("SELECT r FROM Role r WHERE r.status = 1")
    List<Role> findActiveRoles();
    
    /**
     * 根据角色名列表查找角色
     */
    @Query("SELECT r FROM Role r WHERE r.roleName IN :roleNames")
    List<Role> findByRoleNames(@Param("roleNames") Set<String> roleNames);
    
    /**
     * 查找拥有指定权限的角色
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.permissionName = :permissionName")
    List<Role> findByPermissionName(@Param("permissionName") String permissionName);
    
    /**
     * 查找用户拥有的角色
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId")
    List<Role> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查找用户拥有的角色名称
     */
    @Query("SELECT r.roleName FROM Role r JOIN r.users u WHERE u.id = :userId")
    Set<String> findRoleNamesByUserId(@Param("userId") Long userId);
    
    /**
     * 统计角色总数
     */
    @Query("SELECT COUNT(r) FROM Role r")
    long countTotalRoles();
    
    /**
     * 统计激活角色数
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.status = 1")
    long countActiveRoles();
    
    /**
     * 统计角色下的用户数
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.id = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据角色名模糊查询
     */
    @Query("SELECT r FROM Role r WHERE r.roleName LIKE %:keyword% OR r.description LIKE %:keyword%")
    List<Role> searchByKeyword(@Param("keyword") String keyword);
}