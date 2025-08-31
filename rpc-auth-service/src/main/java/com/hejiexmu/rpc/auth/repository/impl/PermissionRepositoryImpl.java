package com.hejiexmu.rpc.auth.repository.impl;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;
import com.hejiexmu.rpc.auth.entity.Permission;
import com.hejiexmu.rpc.auth.repository.BaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 权限Repository实现类
 * 实现主从分离的读写操作
 * 
 * @author hejiexmu
 */
@Repository
public class PermissionRepositoryImpl implements BaseRepository<Permission, Long> {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionRepositoryImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public PermissionRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public Permission save(Permission permission) {
        try {
            if (permission.getId() == null) {
                return insert(permission);
            } else {
                update(permission);
                return permission;
            }
        } catch (Exception e) {
            logger.error("保存权限失败: {}", permission, e);
            throw new RuntimeException("保存权限失败", e);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public List<Permission> saveAll(List<Permission> permissions) {
        List<Permission> savedPermissions = new ArrayList<>();
        for (Permission permission : permissions) {
            savedPermissions.add(save(permission));
        }
        return savedPermissions;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Permission> findById(Long id) {
        try {
            String sql = "SELECT * FROM permissions WHERE id = ? AND deleted_at IS NULL";
            List<Permission> permissions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), id);
            return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
        } catch (Exception e) {
            logger.error("根据ID查找权限失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String sql = "SELECT * FROM permissions WHERE id IN (:ids) AND deleted_at IS NULL";
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
            return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(Permission.class));
        } catch (Exception e) {
            logger.error("根据ID列表查找权限失败: ids={}", ids, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findAll() {
        try {
            String sql = "SELECT * FROM permissions WHERE deleted_at IS NULL ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class));
        } catch (Exception e) {
            logger.error("查找所有权限失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<Permission> findAll(int page, int size) {
        try {
            // 查询总数
            String countSql = "SELECT COUNT(*) FROM permissions WHERE deleted_at IS NULL";
            long totalElements = jdbcTemplate.queryForObject(countSql, Long.class);
            
            // 查询数据
            String sql = "SELECT * FROM permissions WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
            int offset = page * size;
            List<Permission> permissions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), size, offset);
            
            return new PageResult<>(permissions, totalElements, page, size);
        } catch (Exception e) {
            logger.error("分页查找权限失败: page={}, size={}", page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean deleteById(Long id) {
        try {
            String sql = "UPDATE permissions SET deleted_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), id);
            return rows > 0;
        } catch (Exception e) {
            logger.error("根据ID删除权限失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean delete(Permission permission) {
        return deleteById(permission.getId());
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int deleteAll(List<Permission> permissions) {
        int deletedCount = 0;
        for (Permission permission : permissions) {
            if (delete(permission)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        try {
            String sql = "UPDATE permissions SET deleted_at = :deletedAt WHERE id IN (:ids)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("deletedAt", LocalDateTime.now());
            params.addValue("ids", ids);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            logger.error("批量删除权限失败: ids={}", ids, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean update(Permission permission) {
        try {
            String sql = "UPDATE permissions SET name = :name, code = :code, description = :description, " +
                        "resource = :resource, action = :action, status = :status, updated_at = :updatedAt WHERE id = :id";
            
            permission.setUpdatedAt(LocalDateTime.now());
            BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(permission);
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新权限失败: {}", permission, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int updateAll(List<Permission> permissions) {
        int updatedCount = 0;
        for (Permission permission : permissions) {
            if (update(permission)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findByCondition(Permission condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM permissions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            sql.append(" ORDER BY created_at DESC");
            
            return namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(Permission.class));
        } catch (Exception e) {
            logger.error("根据条件查找权限失败: {}", condition, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<Permission> findByCondition(Permission condition, int page, int size) {
        try {
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM permissions WHERE deleted_at IS NULL");
            StringBuilder sql = new StringBuilder("SELECT * FROM permissions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(countSql, params, condition);
            buildWhereClause(sql, params, condition);
            
            // 查询总数
            long totalElements = namedParameterJdbcTemplate.queryForObject(countSql.toString(), params, Long.class);
            
            // 查询数据
            sql.append(" ORDER BY created_at DESC LIMIT :size OFFSET :offset");
            params.addValue("size", size);
            params.addValue("offset", page * size);
            
            List<Permission> permissions = namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(Permission.class));
            
            return new PageResult<>(permissions, totalElements, page, size);
        } catch (Exception e) {
            logger.error("根据条件分页查找权限失败: condition={}, page={}, size={}", condition, page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long count() {
        try {
            String sql = "SELECT COUNT(*) FROM permissions WHERE deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            logger.error("统计权限总数失败", e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countByCondition(Permission condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM permissions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            
            return namedParameterJdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        } catch (Exception e) {
            logger.error("根据条件统计权限数量失败: {}", condition, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(*) FROM permissions WHERE id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查权限是否存在失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCondition(Permission condition) {
        return countByCondition(condition) > 0;
    }
    
    // ========== 权限特定方法 ==========
    
    /**
     * 根据权限名称查找权限
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Permission> findByName(String name) {
        try {
            String sql = "SELECT * FROM permissions WHERE name = ? AND deleted_at IS NULL";
            List<Permission> permissions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), name);
            return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
        } catch (Exception e) {
            logger.error("根据权限名称查找权限失败: name={}", name, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据权限代码查找权限
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Permission> findByCode(String code) {
        try {
            String sql = "SELECT * FROM permissions WHERE code = ? AND deleted_at IS NULL";
            List<Permission> permissions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), code);
            return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
        } catch (Exception e) {
            logger.error("根据权限代码查找权限失败: code={}", code, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据资源和操作查找权限
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Permission> findByResourceAndAction(String resource, String action) {
        try {
            String sql = "SELECT * FROM permissions WHERE resource = ? AND action = ? AND deleted_at IS NULL";
            List<Permission> permissions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), resource, action);
            return permissions.isEmpty() ? Optional.empty() : Optional.of(permissions.get(0));
        } catch (Exception e) {
            logger.error("根据资源和操作查找权限失败: resource={}, action={}", resource, action, e);
            return Optional.empty();
        }
    }
    
    /**
     * 检查权限名称是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT COUNT(*) FROM permissions WHERE name = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, name);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查权限名称是否存在失败: name={}", name, e);
            return false;
        }
    }
    
    /**
     * 检查权限代码是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCode(String code) {
        try {
            String sql = "SELECT COUNT(*) FROM permissions WHERE code = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, code);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查权限代码是否存在失败: code={}", code, e);
            return false;
        }
    }
    
    /**
     * 根据用户ID查找权限列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findByUserId(Long userId) {
        try {
            String sql = "SELECT DISTINCT p.* FROM permissions p " +
                        "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                        "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.deleted_at IS NULL " +
                        "AND rp.deleted_at IS NULL AND ur.deleted_at IS NULL";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), userId);
        } catch (Exception e) {
            logger.error("根据用户ID查找权限列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据角色ID查找权限列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findByRoleId(Long roleId) {
        try {
            String sql = "SELECT p.* FROM permissions p " +
                        "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                        "WHERE rp.role_id = ? AND p.deleted_at IS NULL AND rp.deleted_at IS NULL";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), roleId);
        } catch (Exception e) {
            logger.error("根据角色ID查找权限列表失败: roleId={}", roleId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据资源查找权限列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Permission> findByResource(String resource) {
        try {
            String sql = "SELECT * FROM permissions WHERE resource = ? AND deleted_at IS NULL";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Permission.class), resource);
        } catch (Exception e) {
            logger.error("根据资源查找权限列表失败: resource={}", resource, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查用户是否有指定权限
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean hasPermission(Long userId, String resource, String action) {
        try {
            String sql = "SELECT COUNT(*) FROM permissions p " +
                        "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                        "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.resource = ? AND p.action = ? " +
                        "AND p.deleted_at IS NULL AND rp.deleted_at IS NULL AND ur.deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, resource, action);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查用户权限失败: userId={}, resource={}, action={}", userId, resource, action, e);
            return false;
        }
    }
    
    /**
     * 检查用户是否有指定权限代码
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean hasPermissionByCode(Long userId, String permissionCode) {
        try {
            String sql = "SELECT COUNT(*) FROM permissions p " +
                        "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                        "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.code = ? " +
                        "AND p.deleted_at IS NULL AND rp.deleted_at IS NULL AND ur.deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, permissionCode);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查用户权限代码失败: userId={}, permissionCode={}", userId, permissionCode, e);
            return false;
        }
    }
    
    /**
     * 获取用户权限代码列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<String> getUserPermissionCodes(Long userId) {
        try {
            String sql = "SELECT DISTINCT p.code FROM permissions p " +
                        "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                        "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                        "WHERE ur.user_id = ? AND p.deleted_at IS NULL " +
                        "AND rp.deleted_at IS NULL AND ur.deleted_at IS NULL";
            return jdbcTemplate.queryForList(sql, String.class, userId);
        } catch (Exception e) {
            logger.error("获取用户权限代码列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 插入新权限
     */
    private Permission insert(Permission permission) {
        String sql = "INSERT INTO permissions (name, code, description, resource, action, status, created_at, updated_at) VALUES " +
                    "(:name, :code, :description, :resource, :action, :status, :createdAt, :updatedAt)";
        
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(permission);
        
        namedParameterJdbcTemplate.update(sql, params, keyHolder);
        
        Number key = keyHolder.getKey();
        if (key != null) {
            permission.setId(key.longValue());
        }
        
        return permission;
    }
    
    /**
     * 构建WHERE子句
     */
    private void buildWhereClause(StringBuilder sql, MapSqlParameterSource params, Permission condition) {
        if (condition == null) {
            return;
        }
        
        if (StringUtils.hasText(condition.getPermissionName())) {
            sql.append(" AND permission_name = :permissionName");
            params.addValue("permissionName", condition.getPermissionName());
        }
        
        if (StringUtils.hasText(condition.getResource())) {
            sql.append(" AND resource = :resource");
            params.addValue("resource", condition.getResource());
        }
        
        if (StringUtils.hasText(condition.getResource())) {
            sql.append(" AND resource = :resource");
            params.addValue("resource", condition.getResource());
        }
        
        if (StringUtils.hasText(condition.getAction())) {
            sql.append(" AND action = :action");
            params.addValue("action", condition.getAction());
        }
        
        if (condition.getStatus() != null) {
            sql.append(" AND status = :status");
            params.addValue("status", condition.getStatus());
        }
    }
}