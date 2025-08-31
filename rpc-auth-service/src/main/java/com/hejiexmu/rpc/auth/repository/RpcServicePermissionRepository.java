package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.entity.RpcServicePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RPC服务权限数据访问接口
 * 
 * @author hejiexmu
 */
@Repository
public interface RpcServicePermissionRepository extends JpaRepository<RpcServicePermission, Long> {
    
    /**
     * 根据服务名查找权限配置
     */
    List<RpcServicePermission> findByServiceName(String serviceName);
    
    /**
     * 根据服务名和方法名查找权限配置
     */
    Optional<RpcServicePermission> findByServiceNameAndMethodName(String serviceName, String methodName);
    
    /**
     * 根据方法名查找权限配置
     */
    List<RpcServicePermission> findByMethodName(String methodName);
    
    /**
     * 根据所需权限查找服务配置
     */
    List<RpcServicePermission> findByRequiredPermission(String requiredPermission);
    
    /**
     * 检查服务方法是否存在权限配置
     */
    boolean existsByServiceNameAndMethodName(String serviceName, String methodName);
    
    /**
     * 查找所有服务名
     */
    @Query("SELECT DISTINCT r.serviceName FROM RpcServicePermission r ORDER BY r.serviceName")
    List<String> findAllServiceNames();
    
    /**
     * 查找指定服务的所有方法名
     */
    @Query("SELECT r.methodName FROM RpcServicePermission r WHERE r.serviceName = :serviceName ORDER BY r.methodName")
    List<String> findMethodNamesByServiceName(@Param("serviceName") String serviceName);
    
    /**
     * 查找所有所需权限
     */
    @Query("SELECT DISTINCT r.requiredPermission FROM RpcServicePermission r ORDER BY r.requiredPermission")
    List<String> findAllRequiredPermissions();
    
    /**
     * 根据服务名列表查找权限配置
     */
    @Query("SELECT r FROM RpcServicePermission r WHERE r.serviceName IN :serviceNames")
    List<RpcServicePermission> findByServiceNames(@Param("serviceNames") Set<String> serviceNames);
    
    /**
     * 根据权限列表查找服务配置
     */
    @Query("SELECT r FROM RpcServicePermission r WHERE r.requiredPermission IN :permissions")
    List<RpcServicePermission> findByRequiredPermissions(@Param("permissions") Set<String> permissions);
    
    /**
     * 统计服务总数
     */
    @Query("SELECT COUNT(DISTINCT r.serviceName) FROM RpcServicePermission r")
    long countTotalServices();
    
    /**
     * 统计方法总数
     */
    @Query("SELECT COUNT(r) FROM RpcServicePermission r")
    long countTotalMethods();
    
    /**
     * 统计指定服务的方法数
     */
    @Query("SELECT COUNT(r) FROM RpcServicePermission r WHERE r.serviceName = :serviceName")
    long countMethodsByServiceName(@Param("serviceName") String serviceName);
    
    /**
     * 统计需要指定权限的方法数
     */
    @Query("SELECT COUNT(r) FROM RpcServicePermission r WHERE r.requiredPermission = :permission")
    long countMethodsByRequiredPermission(@Param("permission") String permission);
    
    /**
     * 根据关键字模糊查询
     */
    @Query("SELECT r FROM RpcServicePermission r WHERE r.serviceName LIKE %:keyword% OR r.methodName LIKE %:keyword% OR r.description LIKE %:keyword%")
    List<RpcServicePermission> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 查找用户有权限访问的RPC服务方法
     */
    @Query("SELECT r FROM RpcServicePermission r WHERE r.requiredPermission IN (" +
           "SELECT DISTINCT p.permissionName FROM Permission p JOIN p.roles role JOIN role.users u WHERE u.id = :userId)")
    List<RpcServicePermission> findAccessibleServicesByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否有权限访问指定的RPC服务方法
     */
    @Query("SELECT COUNT(r) > 0 FROM RpcServicePermission r WHERE r.serviceName = :serviceName AND r.methodName = :methodName " +
           "AND r.requiredPermission IN (" +
           "SELECT DISTINCT p.permissionName FROM Permission p JOIN p.roles role JOIN role.users u WHERE u.id = :userId)")
    boolean hasUserAccessToServiceMethod(@Param("userId") Long userId, 
                                        @Param("serviceName") String serviceName, 
                                        @Param("methodName") String methodName);
    
    /**
     * 查找用户可访问的服务名列表
     */
    @Query("SELECT DISTINCT r.serviceName FROM RpcServicePermission r WHERE r.requiredPermission IN (" +
           "SELECT DISTINCT p.permissionName FROM Permission p JOIN p.roles role JOIN role.users u WHERE u.id = :userId) " +
           "ORDER BY r.serviceName")
    List<String> findAccessibleServiceNamesByUserId(@Param("userId") Long userId);
}