package com.hejiexmu.rpc.auth.repository.impl;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;
import com.hejiexmu.rpc.auth.entity.Role;
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
 * 角色Repository实现类
 * 实现主从分离的读写操作
 * 
 * @author hejiexmu
 */
@Repository
public class RoleRepositoryImpl implements BaseRepository<Role, Long> {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleRepositoryImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public RoleRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public Role save(Role role) {
        try {
            if (role.getId() == null) {
                return insert(role);
            } else {
                update(role);
                return role;
            }
        } catch (Exception e) {
            logger.error("保存角色失败: {}", role, e);
            throw new RuntimeException("保存角色失败", e);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public List<Role> saveAll(List<Role> roles) {
        List<Role> savedRoles = new ArrayList<>();
        for (Role role : roles) {
            savedRoles.add(save(role));
        }
        return savedRoles;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Role> findById(Long id) {
        try {
            String sql = "SELECT * FROM roles WHERE id = ? AND deleted_at IS NULL";
            List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), id);
            return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
        } catch (Exception e) {
            logger.error("根据ID查找角色失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Role> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String sql = "SELECT * FROM roles WHERE id IN (:ids) AND deleted_at IS NULL";
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
            return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(Role.class));
        } catch (Exception e) {
            logger.error("根据ID列表查找角色失败: ids={}", ids, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Role> findAll() {
        try {
            String sql = "SELECT * FROM roles WHERE deleted_at IS NULL ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class));
        } catch (Exception e) {
            logger.error("查找所有角色失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<Role> findAll(int page, int size) {
        try {
            // 查询总数
            String countSql = "SELECT COUNT(*) FROM roles WHERE deleted_at IS NULL";
            long totalElements = jdbcTemplate.queryForObject(countSql, Long.class);
            
            // 查询数据
            String sql = "SELECT * FROM roles WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
            int offset = page * size;
            List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), size, offset);
            
            return new PageResult<>(roles, totalElements, page, size);
        } catch (Exception e) {
            logger.error("分页查找角色失败: page={}, size={}", page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean deleteById(Long id) {
        try {
            String sql = "UPDATE roles SET deleted_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), id);
            return rows > 0;
        } catch (Exception e) {
            logger.error("根据ID删除角色失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean delete(Role role) {
        return deleteById(role.getId());
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int deleteAll(List<Role> roles) {
        int deletedCount = 0;
        for (Role role : roles) {
            if (delete(role)) {
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
            String sql = "UPDATE roles SET deleted_at = :deletedAt WHERE id IN (:ids)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("deletedAt", LocalDateTime.now());
            params.addValue("ids", ids);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            logger.error("批量删除角色失败: ids={}", ids, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean update(Role role) {
        try {
            String sql = "UPDATE roles SET name = :name, code = :code, description = :description, " +
                        "status = :status, updated_at = :updatedAt WHERE id = :id";
            
            role.setUpdatedAt(LocalDateTime.now());
            BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(role);
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新角色失败: {}", role, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int updateAll(List<Role> roles) {
        int updatedCount = 0;
        for (Role role : roles) {
            if (update(role)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Role> findByCondition(Role condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM roles WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            sql.append(" ORDER BY created_at DESC");
            
            return namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(Role.class));
        } catch (Exception e) {
            logger.error("根据条件查找角色失败: {}", condition, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<Role> findByCondition(Role condition, int page, int size) {
        try {
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM roles WHERE deleted_at IS NULL");
            StringBuilder sql = new StringBuilder("SELECT * FROM roles WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(countSql, params, condition);
            buildWhereClause(sql, params, condition);
            
            // 查询总数
            long totalElements = namedParameterJdbcTemplate.queryForObject(countSql.toString(), params, Long.class);
            
            // 查询数据
            sql.append(" ORDER BY created_at DESC LIMIT :size OFFSET :offset");
            params.addValue("size", size);
            params.addValue("offset", page * size);
            
            List<Role> roles = namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(Role.class));
            
            return new PageResult<>(roles, totalElements, page, size);
        } catch (Exception e) {
            logger.error("根据条件分页查找角色失败: condition={}, page={}, size={}", condition, page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long count() {
        try {
            String sql = "SELECT COUNT(*) FROM roles WHERE deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            logger.error("统计角色总数失败", e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countByCondition(Role condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM roles WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            
            return namedParameterJdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        } catch (Exception e) {
            logger.error("根据条件统计角色数量失败: {}", condition, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(*) FROM roles WHERE id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查角色是否存在失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCondition(Role condition) {
        return countByCondition(condition) > 0;
    }
    
    // ========== 角色特定方法 ==========
    
    /**
     * 根据角色名称查找角色
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Role> findByName(String name) {
        try {
            String sql = "SELECT * FROM roles WHERE name = ? AND deleted_at IS NULL";
            List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), name);
            return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
        } catch (Exception e) {
            logger.error("根据角色名称查找角色失败: name={}", name, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据角色代码查找角色
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<Role> findByCode(String code) {
        try {
            String sql = "SELECT * FROM roles WHERE code = ? AND deleted_at IS NULL";
            List<Role> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), code);
            return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
        } catch (Exception e) {
            logger.error("根据角色代码查找角色失败: code={}", code, e);
            return Optional.empty();
        }
    }
    
    /**
     * 检查角色名称是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByName(String name) {
        try {
            String sql = "SELECT COUNT(*) FROM roles WHERE name = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, name);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查角色名称是否存在失败: name={}", name, e);
            return false;
        }
    }
    
    /**
     * 检查角色代码是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCode(String code) {
        try {
            String sql = "SELECT COUNT(*) FROM roles WHERE code = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, code);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查角色代码是否存在失败: code={}", code, e);
            return false;
        }
    }
    
    /**
     * 根据用户ID查找角色列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Role> findByUserId(Long userId) {
        try {
            String sql = "SELECT r.* FROM roles r " +
                        "INNER JOIN user_roles ur ON r.id = ur.role_id " +
                        "WHERE ur.user_id = ? AND r.deleted_at IS NULL AND ur.deleted_at IS NULL";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), userId);
        } catch (Exception e) {
            logger.error("根据用户ID查找角色列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据权限ID查找角色列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Role> findByPermissionId(Long permissionId) {
        try {
            String sql = "SELECT r.* FROM roles r " +
                        "INNER JOIN role_permissions rp ON r.id = rp.role_id " +
                        "WHERE rp.permission_id = ? AND r.deleted_at IS NULL AND rp.deleted_at IS NULL";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Role.class), permissionId);
        } catch (Exception e) {
            logger.error("根据权限ID查找角色列表失败: permissionId={}", permissionId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 为用户分配角色
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean assignRoleToUser(Long userId, Long roleId) {
        try {
            // 检查是否已经分配
            String checkSql = "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(checkSql, Long.class, userId, roleId);
            if (count != null && count > 0) {
                return true; // 已经分配过了
            }
            
            String sql = "INSERT INTO user_roles (user_id, role_id, created_at, updated_at) VALUES (?, ?, ?, ?)";
            LocalDateTime now = LocalDateTime.now();
            int rows = jdbcTemplate.update(sql, userId, roleId, now, now);
            return rows > 0;
        } catch (Exception e) {
            logger.error("为用户分配角色失败: userId={}, roleId={}", userId, roleId, e);
            return false;
        }
    }
    
    /**
     * 移除用户角色
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean removeRoleFromUser(Long userId, Long roleId) {
        try {
            String sql = "UPDATE user_roles SET deleted_at = ? WHERE user_id = ? AND role_id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), userId, roleId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("移除用户角色失败: userId={}, roleId={}", userId, roleId, e);
            return false;
        }
    }
    
    /**
     * 为角色分配权限
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean assignPermissionToRole(Long roleId, Long permissionId) {
        try {
            // 检查是否已经分配
            String checkSql = "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(checkSql, Long.class, roleId, permissionId);
            if (count != null && count > 0) {
                return true; // 已经分配过了
            }
            
            String sql = "INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at) VALUES (?, ?, ?, ?)";
            LocalDateTime now = LocalDateTime.now();
            int rows = jdbcTemplate.update(sql, roleId, permissionId, now, now);
            return rows > 0;
        } catch (Exception e) {
            logger.error("为角色分配权限失败: roleId={}, permissionId={}", roleId, permissionId, e);
            return false;
        }
    }
    
    /**
     * 移除角色权限
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean removePermissionFromRole(Long roleId, Long permissionId) {
        try {
            String sql = "UPDATE role_permissions SET deleted_at = ? WHERE role_id = ? AND permission_id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), roleId, permissionId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("移除角色权限失败: roleId={}, permissionId={}", roleId, permissionId, e);
            return false;
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 插入新角色
     */
    private Role insert(Role role) {
        String sql = "INSERT INTO roles (name, code, description, status, created_at, updated_at) VALUES " +
                    "(:name, :code, :description, :status, :createdAt, :updatedAt)";
        
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(role);
        
        namedParameterJdbcTemplate.update(sql, params, keyHolder);
        
        Number key = keyHolder.getKey();
        if (key != null) {
            role.setId(key.longValue());
        }
        
        return role;
    }
    
    /**
     * 构建WHERE子句
     */
    private void buildWhereClause(StringBuilder sql, MapSqlParameterSource params, Role condition) {
        if (condition == null) {
            return;
        }
        
        if (StringUtils.hasText(condition.getRoleName())) {
            sql.append(" AND role_name = :roleName");
            params.addValue("roleName", condition.getRoleName());
        }
        
        if (StringUtils.hasText(condition.getRoleName())) {
            sql.append(" AND role_name = :roleName");
            params.addValue("roleName", condition.getRoleName());
        }
        
        if (condition.getStatus() != null) {
            sql.append(" AND status = :status");
            params.addValue("status", condition.getStatus());
        }
    }
}