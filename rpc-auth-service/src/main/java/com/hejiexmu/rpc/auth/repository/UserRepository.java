package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 * 
 * @author hejiexmu
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据用户名或邮箱查找用户
     */
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(Integer status);
    
    /**
     * 查找激活的用户
     */
    @Query("SELECT u FROM User u WHERE u.status = 1")
    List<User> findActiveUsers();
    
    /**
     * 根据角色名查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * 查找拥有指定权限的用户
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r JOIN r.permissions p WHERE p.permissionName = :permissionName")
    List<User> findByPermissionName(@Param("permissionName") String permissionName);
    
    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyLoggedInUsers(@Param("since") LocalDateTime since);
    
    /**
     * 查找长时间未登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :before OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsers(@Param("before") LocalDateTime before);
    
    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();
    
    /**
     * 统计激活用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 1")
    long countActiveUsers();
    
    /**
     * 统计指定时间段内注册的用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countUsersByRegistrationPeriod(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * 根据用户名模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);
    
    /**
     * 根据状态统计用户数
     */
    long countByStatus(UserStatus status);
    
    /**
     * 根据用户名、邮箱或真实姓名模糊查询（分页）
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:username% OR u.email LIKE %:email% OR u.realName LIKE %:realName%")
    Page<User> findByUsernameContainingOrEmailContainingOrRealNameContaining(
            @Param("username") String username, 
            @Param("email") String email, 
            @Param("realName") String realName, 
            Pageable pageable);
    
    /**
     * 统计指定时间之后创建的用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :after")
    long countByCreatedAtAfter(@Param("after") LocalDateTime after);
}