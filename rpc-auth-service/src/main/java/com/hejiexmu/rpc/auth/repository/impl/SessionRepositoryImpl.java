package com.hejiexmu.rpc.auth.repository.impl;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;
import com.hejiexmu.rpc.auth.entity.UserSession;
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
 * 会话Repository实现类
 * 实现主从分离的读写操作
 * 
 * @author hejiexmu
 */
@Repository
public class SessionRepositoryImpl implements BaseRepository<UserSession, Long> {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionRepositoryImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    
    public SessionRepositoryImpl(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public UserSession save(UserSession session) {
        try {
            if (session.getId() == null) {
                return insert(session);
            } else {
                update(session);
                return session;
            }
        } catch (Exception e) {
            logger.error("保存会话失败: {}", session, e);
            throw new RuntimeException("保存会话失败", e);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public List<UserSession> saveAll(List<UserSession> sessions) {
        List<UserSession> savedSessions = new ArrayList<>();
        for (UserSession session : sessions) {
            savedSessions.add(save(session));
        }
        return savedSessions;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<UserSession> findById(Long id) {
        try {
            String sql = "SELECT * FROM sessions WHERE id = ? AND deleted_at IS NULL";
            List<UserSession> sessions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), id);
            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
        } catch (Exception e) {
            logger.error("根据ID查找会话失败: id={}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<UserSession> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            String sql = "SELECT * FROM sessions WHERE id IN (:ids) AND deleted_at IS NULL";
            MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
            return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(UserSession.class));
        } catch (Exception e) {
            logger.error("根据ID列表查找会话失败: ids={}", ids, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<UserSession> findAll() {
        try {
            String sql = "SELECT * FROM sessions WHERE deleted_at IS NULL ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class));
        } catch (Exception e) {
            logger.error("查找所有会话失败", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<UserSession> findAll(int page, int size) {
        try {
            // 查询总数
            String countSql = "SELECT COUNT(*) FROM sessions WHERE deleted_at IS NULL";
            long totalElements = jdbcTemplate.queryForObject(countSql, Long.class);
            
            // 查询数据
            String sql = "SELECT * FROM sessions WHERE deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
            int offset = page * size;
            List<UserSession> sessions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), size, offset);
            
            return new PageResult<>(sessions, totalElements, page, size);
        } catch (Exception e) {
            logger.error("分页查找会话失败: page={}, size={}", page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean deleteById(Long id) {
        try {
            String sql = "UPDATE sessions SET deleted_at = ? WHERE id = ?";
            int rows = jdbcTemplate.update(sql, LocalDateTime.now(), id);
            return rows > 0;
        } catch (Exception e) {
            logger.error("根据ID删除会话失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean delete(UserSession session) {
        return deleteById(session.getId());
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int deleteAll(List<UserSession> sessions) {
        int deletedCount = 0;
        for (UserSession session : sessions) {
            if (delete(session)) {
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
            String sql = "UPDATE sessions SET deleted_at = :deletedAt WHERE id IN (:ids)";
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("deletedAt", LocalDateTime.now());
            params.addValue("ids", ids);
            return namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            logger.error("批量删除会话失败: ids={}", ids, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean update(UserSession session) {
        try {
            String sql = "UPDATE sessions SET user_id = :userId, session_id = :sessionId, " +
                        "access_token = :accessToken, refresh_token = :refreshToken, " +
                        "expires_at = :expiresAt, last_access_at = :lastAccessAt, " +
                        "client_ip = :clientIp, user_agent = :userAgent, status = :status, " +
                        "updated_at = :updatedAt WHERE id = :id";
            
            session.setUpdatedAt(LocalDateTime.now());
            BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(session);
            int rows = namedParameterJdbcTemplate.update(sql, params);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新会话失败: {}", session, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int updateAll(List<UserSession> sessions) {
        int updatedCount = 0;
        for (UserSession session : sessions) {
            if (update(session)) {
                updatedCount++;
            }
        }
        return updatedCount;
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<UserSession> findByCondition(UserSession condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM sessions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            sql.append(" ORDER BY created_at DESC");
            
            return namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(UserSession.class));
        } catch (Exception e) {
            logger.error("根据条件查找会话失败: {}", condition, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public PageResult<UserSession> findByCondition(UserSession condition, int page, int size) {
        try {
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM sessions WHERE deleted_at IS NULL");
            StringBuilder sql = new StringBuilder("SELECT * FROM sessions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(countSql, params, condition);
            buildWhereClause(sql, params, condition);
            
            // 查询总数
            long totalElements = namedParameterJdbcTemplate.queryForObject(countSql.toString(), params, Long.class);
            
            // 查询数据
            sql.append(" ORDER BY created_at DESC LIMIT :size OFFSET :offset");
            params.addValue("size", size);
            params.addValue("offset", page * size);
            
            List<UserSession> sessions = namedParameterJdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(UserSession.class));
            
            return new PageResult<>(sessions, totalElements, page, size);
        } catch (Exception e) {
            logger.error("根据条件分页查找会话失败: condition={}, page={}, size={}", condition, page, size, e);
            return new PageResult<>(new ArrayList<>(), 0, page, size);
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long count() {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            logger.error("统计会话总数失败", e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countByCondition(UserSession condition) {
        try {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sessions WHERE deleted_at IS NULL");
            MapSqlParameterSource params = new MapSqlParameterSource();
            
            buildWhereClause(sql, params, condition);
            
            return namedParameterJdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        } catch (Exception e) {
            logger.error("根据条件统计会话数量失败: {}", condition, e);
            return 0;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsById(Long id) {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, id);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查会话是否存在失败: id={}", id, e);
            return false;
        }
    }
    
    @Override
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByCondition(UserSession condition) {
        return countByCondition(condition) > 0;
    }
    
    // ========== 会话特定方法 ==========
    
    /**
     * 根据会话ID查找会话
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<UserSession> findBySessionId(String sessionId) {
        try {
            String sql = "SELECT * FROM sessions WHERE session_id = ? AND deleted_at IS NULL";
            List<UserSession> sessions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), sessionId);
            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
        } catch (Exception e) {
            logger.error("根据会话ID查找会话失败: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据访问令牌查找会话
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<UserSession> findByAccessToken(String accessToken) {
        try {
            String sql = "SELECT * FROM sessions WHERE access_token = ? AND deleted_at IS NULL";
            List<UserSession> sessions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), accessToken);
            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
        } catch (Exception e) {
            logger.error("根据访问令牌查找会话失败: accessToken={}", accessToken, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据刷新令牌查找会话
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        try {
            String sql = "SELECT * FROM sessions WHERE refresh_token = ? AND deleted_at IS NULL";
            List<UserSession> sessions = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), refreshToken);
            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
        } catch (Exception e) {
            logger.error("根据刷新令牌查找会话失败: refreshToken={}", refreshToken, e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据用户ID查找活跃会话列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<UserSession> findActiveSessionsByUserId(Long userId) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_id = ? AND status = 1 " +
                        "AND expires_at > ? AND deleted_at IS NULL ORDER BY last_access_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), userId, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("根据用户ID查找活跃会话列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据用户ID查找所有会话列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<UserSession> findByUserId(Long userId) {
        try {
            String sql = "SELECT * FROM sessions WHERE user_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
            return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserSession.class), userId);
        } catch (Exception e) {
            logger.error("根据用户ID查找会话列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查会话ID是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsBySessionId(String sessionId) {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE session_id = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, sessionId);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查会话ID是否存在失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 检查访问令牌是否存在
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public boolean existsByAccessToken(String accessToken) {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE access_token = ? AND deleted_at IS NULL";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, accessToken);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("检查访问令牌是否存在失败: accessToken={}", accessToken, e);
            return false;
        }
    }
    
    /**
     * 更新会话最后访问时间
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updateLastAccessAt(String sessionId, LocalDateTime lastAccessAt) {
        try {
            String sql = "UPDATE sessions SET last_access_at = ?, updated_at = ? WHERE session_id = ?";
            int rows = jdbcTemplate.update(sql, lastAccessAt, LocalDateTime.now(), sessionId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新会话最后访问时间失败: sessionId={}, lastAccessAt={}", sessionId, lastAccessAt, e);
            return false;
        }
    }
    
    /**
     * 更新会话状态
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean updateSessionStatus(String sessionId, Integer status) {
        try {
            String sql = "UPDATE sessions SET status = ?, updated_at = ? WHERE session_id = ?";
            int rows = jdbcTemplate.update(sql, status, LocalDateTime.now(), sessionId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("更新会话状态失败: sessionId={}, status={}", sessionId, status, e);
            return false;
        }
    }
    
    /**
     * 使会话过期
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public boolean expireSession(String sessionId) {
        try {
            String sql = "UPDATE sessions SET status = 0, expires_at = ?, updated_at = ? WHERE session_id = ?";
            LocalDateTime now = LocalDateTime.now();
            int rows = jdbcTemplate.update(sql, now, now, sessionId);
            return rows > 0;
        } catch (Exception e) {
            logger.error("使会话过期失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 使用户所有会话过期
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int expireUserSessions(Long userId) {
        try {
            String sql = "UPDATE sessions SET status = 0, expires_at = ?, updated_at = ? WHERE user_id = ? AND status = 1";
            LocalDateTime now = LocalDateTime.now();
            return jdbcTemplate.update(sql, now, now, userId);
        } catch (Exception e) {
            logger.error("使用户所有会话过期失败: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * 使用户其他会话过期（除了当前会话）
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int expireOtherUserSessions(Long userId, String currentSessionId) {
        try {
            String sql = "UPDATE sessions SET status = 0, expires_at = ?, updated_at = ? " +
                        "WHERE user_id = ? AND session_id != ? AND status = 1";
            LocalDateTime now = LocalDateTime.now();
            return jdbcTemplate.update(sql, now, now, userId, currentSessionId);
        } catch (Exception e) {
            logger.error("使用户其他会话过期失败: userId={}, currentSessionId={}", userId, currentSessionId, e);
            return 0;
        }
    }
    
    /**
     * 清理过期会话
     */
    @DataSource(DataSourceConfig.DataSourceType.MASTER)
    public int cleanupExpiredSessions() {
        try {
            String sql = "UPDATE sessions SET deleted_at = ? WHERE expires_at < ? AND deleted_at IS NULL";
            LocalDateTime now = LocalDateTime.now();
            return jdbcTemplate.update(sql, now, now);
        } catch (Exception e) {
            logger.error("清理过期会话失败", e);
            return 0;
        }
    }
    
    /**
     * 统计活跃会话数量
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countActiveSessions() {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE status = 1 AND expires_at > ? AND deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("统计活跃会话数量失败", e);
            return 0;
        }
    }
    
    /**
     * 统计用户活跃会话数量
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public long countActiveSessionsByUserId(Long userId) {
        try {
            String sql = "SELECT COUNT(*) FROM sessions WHERE user_id = ? AND status = 1 " +
                        "AND expires_at > ? AND deleted_at IS NULL";
            return jdbcTemplate.queryForObject(sql, Long.class, userId, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("统计用户活跃会话数量失败: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * 获取在线用户列表
     */
    @DataSource(DataSourceConfig.DataSourceType.SLAVE)
    public List<Long> getOnlineUserIds() {
        try {
            String sql = "SELECT DISTINCT user_id FROM sessions WHERE status = 1 " +
                        "AND expires_at > ? AND deleted_at IS NULL";
            return jdbcTemplate.queryForList(sql, Long.class, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("获取在线用户列表失败", e);
            return new ArrayList<>();
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 插入新会话
     */
    private UserSession insert(UserSession session) {
        String sql = "INSERT INTO sessions (user_id, session_id, access_token, refresh_token, " +
                    "expires_at, last_access_at, client_ip, user_agent, status, created_at, updated_at) VALUES " +
                    "(:userId, :sessionId, :accessToken, :refreshToken, :expiresAt, :lastAccessAt, " +
                    ":clientIp, :userAgent, :status, :createdAt, :updatedAt)";
        
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(session);
        
        namedParameterJdbcTemplate.update(sql, params, keyHolder);
        
        Number key = keyHolder.getKey();
        if (key != null) {
            session.setId(key.longValue());
        }
        
        return session;
    }
    
    /**
     * 构建WHERE子句
     */
    private void buildWhereClause(StringBuilder sql, MapSqlParameterSource params, UserSession condition) {
        if (condition == null) {
            return;
        }
        
        if (condition.getUserId() != null) {
            sql.append(" AND user_id = :userId");
            params.addValue("userId", condition.getUserId());
        }
        
        if (StringUtils.hasText(condition.getSessionId())) {
            sql.append(" AND session_id = :sessionId");
            params.addValue("sessionId", condition.getSessionId());
        }
        
        if (StringUtils.hasText(condition.getClientIp())) {
            sql.append(" AND client_ip = :clientIp");
            params.addValue("clientIp", condition.getClientIp());
        }
        
        if (condition.getStatus() != null) {
            sql.append(" AND status = :status");
            params.addValue("status", condition.getStatus());
        }
        
        if (condition.getExpiresAt() != null) {
            sql.append(" AND expires_at > :expiresAt");
            params.addValue("expiresAt", condition.getExpiresAt());
        }
    }
}