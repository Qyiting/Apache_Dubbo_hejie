package com.hejiexmu.rpc.auth.repository.impl;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.repository.BaseRepository;
import com.hejiexmu.rpc.auth.service.UserService;
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
 * 用户Repository实现类
 * 实现主从分离的读写操作
 * 
 * @author hejiexmu
 */
@Repository
public class UserRepositoryImpl implements BaseRepository<User, Long> {
    
    private static final Logger logger = LoggerFactory.getLogger(UserRepositoryImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public UserRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public User save(User user) {
        try {
            if (user.getId() == null) {
                return insert(user);
            } else {
                update(user);
                return user;
            }
        } catch (Exception e) {
            logger.error("保存用户失败: {}", user, e);
            throw new RuntimeException("保存用户失败", e);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public List<User> saveAll(List<User> users) {
        List<User> savedUsers = new ArrayList<>();
        for (User user : users) {
            savedUsers.add(save(user));
        }
        return savedUsers;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<User> findById(Long id) {
        try {
            String sql = "SELECT * FROM users WHERE id = ? AND deleted_at IS NULL";
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), id);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
        } catch (Exception e) {
            logger.error("根据ID查找用户失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<User> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String sql = "SELECT * FROM users WHERE id IN (:ids) AND deleted_at IS NULL";
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
            return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(User.class));
        } catch (Exception e) {
            logger.error("根据ID列表查找用户失败: ids={}", ids, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<User> findAll() {
        try {
            String sql = "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
        } catch (Exception e) {
            logger.error("查找所有用户失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<User> findAll(int page, int size) {
        try {
            // 查询总数
            String countSql = "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL";
            long totalElements = jdbcTemplate.queryForObject(countSql, Long.class);
            
            // 查询数据
            String sql = "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
            int offset = page * size;
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), size, offset);
            
            return new PageResult<>(users, totalElements, page, size);
        } catch (Exception e) {
            logger.error("分页查找用户失败: page={}, size={}", page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean deleteById(Long id) {
        try {
            String sql = "UPDATE users SET deleted_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), id);
            return rows > 0;
        } catch (Exception e) {
            logger.error("根据ID删除用户失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean delete(User user) {
        return deleteById(user.getId());
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int deleteAll(List<User> users) {
        int deletedCount = 0;
        for (User user : users) {
            if (delete(user)) {
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
            String sql = "UPDATE users SET deleted_at = :deletedAt WHERE id IN (:ids)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("deletedAt", LocalDateTime.now());
            params.addValue("ids", ids);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            logger.error("批量删除用户失败: ids={}", ids, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean update(User user) {
        try {
            String sql = "UPDATE users SET username = :username, email = :email, phone = :phone, " +
                        "password_hash = :passwordHash, status = :status, last_login_at = :lastLoginAt, " +
                        "last_login_ip = :lastLoginIp, failed_login_attempts = :failedLoginAttempts, " +
                        "updated_at = :updatedAt WHERE id = :id";
            
            user.setUpdatedAt(LocalDateTime.now());
            BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(user);
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新用户失败: {}", user, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int updateAll(List<User> users) {
        int updatedCount = 0;
        for (User user : users) {
            if (update(user)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<User> findByCondition(User condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            sql.append(" ORDER BY created_at DESC");
            
            return namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(User.class));
        } catch (Exception e) {
            logger.error("根据条件查找用户失败: {}", condition, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<User> findByCondition(User condition, int page, int size) {
        try {
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
            StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(countSql, params, condition);
            buildWhereClause(sql, params, condition);
            
            // 查询总数
            long totalElements = namedParameterJdbcTemplate.queryForObject(countSql.toString(), params, Long.class);
            
            // 查询数据
            sql.append(" ORDER BY created_at DESC LIMIT :size OFFSET :offset");
            params.addValue("size", size);
            params.addValue("offset", page * size);
            
            List<User> users = namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(User.class));
            
            return new PageResult<>(users, totalElements, page, size);
        } catch (Exception e) {
            logger.error("根据条件分页查找用户失败: condition={}, page={}, size={}", condition, page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long count() {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            logger.error("统计用户总数失败", e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countByCondition(User condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            
            return namedParameterJdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        } catch (Exception e) {
            logger.error("根据条件统计用户数量失败: {}", condition, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查用户是否存在失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCondition(User condition) {
        return countByCondition(condition) > 0;
    }
    
    // ========== 用户特定方法 ==========
    
    /**
     * 根据用户名查找用户
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<User> findByUsername(String username) {
        try {
            String sql = "SELECT * FROM users WHERE username = ? AND deleted_at IS NULL";
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), username);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
        } catch (Exception e) {
            logger.error("根据用户名查找用户失败: username={}", username, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据邮箱查找用户
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<User> findByEmail(String email) {
        try {
            String sql = "SELECT * FROM users WHERE email = ? AND deleted_at IS NULL";
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), email);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
        } catch (Exception e) {
            logger.error("根据邮箱查找用户失败: email={}", email, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据手机号查找用户
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<User> findByPhone(String phone) {
        try {
            String sql = "SELECT * FROM users WHERE phone = ? AND deleted_at IS NULL";
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), phone);
            return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
        } catch (Exception e) {
            logger.error("根据手机号查找用户失败: phone={}", phone, e);
            return Optional.empty();
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByUsername(String username) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, username);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查用户名是否存在失败: username={}", username, e);
            return false;
        }
    }
    
    /**
     * 检查邮箱是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByEmail(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, email);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查邮箱是否存在失败: email={}", email, e);
            return false;
        }
    }
    
    /**
     * 检查手机号是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByPhone(String phone) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE phone = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, phone);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查手机号是否存在失败: phone={}", phone, e);
            return false;
        }
    }
    
    /**
     * 更新用户最后登录时间
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updateLastLoginAt(Long userId, LocalDateTime lastLoginAt) {
        try {
            String sql = "UPDATE users SET last_login_at = ?, updated_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, lastLoginAt, LocalDateTime.now(), userId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新用户最后登录时间失败: userId={}, lastLoginAt={}", userId, lastLoginAt, e);
            return false;
        }
    }
    
    /**
     * 更新用户最后登录IP
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updateLastLoginIp(Long userId, String lastLoginIp) {
        try {
            String sql = "UPDATE users SET last_login_ip = ?, updated_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, lastLoginIp, LocalDateTime.now(), userId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新用户最后登录IP失败: userId={}, lastLoginIp={}", userId, lastLoginIp, e);
            return false;
        }
    }
    
    /**
     * 更新用户密码
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updatePassword(Long userId, String newPassword) {
        try {
            String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, newPassword, LocalDateTime.now(), userId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新用户密码失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 更新用户账户状态
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updateAccountStatus(Long userId, UserService.AccountStatus status) {
        try {
            String sql = "UPDATE users SET status = ?, updated_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, status.ordinal(), LocalDateTime.now(), userId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新用户账户状态失败: userId={}, status={}", userId, status, e);
            return false;
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 插入新用户
     */
    private User insert(User user) {
        String sql = "INSERT INTO users (username, email, phone, password_hash, status, " +
                    "failed_login_attempts, created_at, updated_at) VALUES " +
                    "(:username, :email, :phone, :passwordHash, :status, :failedLoginAttempts, " +
                    ":createdAt, :updatedAt)";
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(user);
        
        namedParameterJdbcTemplate.update(sql, params, keyHolder);
        
        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(Long.valueOf(key.longValue()));
        }
        
        return user;
    }
    
    /**
     * 构建WHERE子句
     */
    private void buildWhereClause(StringBuilder sql, MapSqlParameterSource params, User condition) {
        if (condition == null) {
            return;
        }
        
        if (StringUtils.hasText(condition.getUsername())) {
            sql.append(" AND username = :username");
            params.addValue("username", condition.getUsername());
        }
        
        if (StringUtils.hasText(condition.getEmail())) {
            sql.append(" AND email = :email");
            params.addValue("email", condition.getEmail());
        }
        
        if (StringUtils.hasText(condition.getPhone())) {
            sql.append(" AND phone = :phone");
            params.addValue("phone", condition.getPhone());
        }
        
        if (condition.getStatus() != null) {
            sql.append(" AND status = :status");
            params.addValue("status", condition.getStatus());
        }
    }
}