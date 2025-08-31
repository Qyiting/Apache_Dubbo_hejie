package com.hejiexmu.rpc.auth.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejiexmu.rpc.auth.dto.SessionDTO;
import com.hejiexmu.rpc.auth.entity.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis会话管理器
 * 实现分布式会话存储和缓存策略
 * 
 * @author hejiexmu
 */
@Component
public class RedisSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisSessionManager.class);
    
    private static final String SESSION_PREFIX = "rpc:auth:session:";
    private static final String USER_SESSIONS_PREFIX = "rpc:auth:user:sessions:";
    private static final String SESSION_INDEX_PREFIX = "rpc:auth:session:index:";
    private static final String ONLINE_USERS_KEY = "rpc:auth:online:users";
    private static final String SESSION_STATS_KEY = "rpc:auth:session:stats";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Lua脚本：原子性操作
    private final DefaultRedisScript<Long> createSessionScript;
    private final DefaultRedisScript<Boolean> validateSessionScript;
    private final DefaultRedisScript<Boolean> expireSessionScript;
    private final DefaultRedisScript<Long> cleanupExpiredScript;
    
    public RedisSessionManager(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        
        // 初始化Lua脚本
        this.createSessionScript = createSessionScript();
        this.validateSessionScript = validateSessionScript();
        this.expireSessionScript = expireSessionScript();
        this.cleanupExpiredScript = cleanupExpiredScript();
    }
    
    /**
     * 创建会话
     */
    public boolean createSession(UserSession session) {
        try {
            String sessionKey = SESSION_PREFIX + session.getSessionId();
            String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
            String sessionIndexKey = SESSION_INDEX_PREFIX + session.getSessionId();
            
            // 序列化会话数据
            String sessionData = objectMapper.writeValueAsString(session);
            
            // 计算过期时间（秒）
            long expirationSeconds = session.getExpiresAt().toEpochSecond(ZoneOffset.UTC) - 
                                   LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
            
            if (expirationSeconds <= 0) {
                logger.warn("会话已过期，无法创建: sessionId={}", session.getSessionId());
                return false;
            }
            
            // 使用Lua脚本原子性创建会话
            List<String> keys = Arrays.asList(sessionKey, userSessionsKey, sessionIndexKey, ONLINE_USERS_KEY);
            List<Object> args = Arrays.asList(
                sessionData,
                session.getUserId().toString(),
                session.getSessionId(),
                expirationSeconds,
                System.currentTimeMillis()
            );
            
            Long result = redisTemplate.execute(createSessionScript, keys, args.toArray());
            
            if (result != null && result > 0) {
                logger.info("会话创建成功: sessionId={}, userId={}, expiresIn={}s", 
                           session.getSessionId(), session.getUserId(), expirationSeconds);
                
                // 更新统计信息
                updateSessionStats("create", 1);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("创建会话失败: sessionId={}", session.getSessionId(), e);
            return false;
        }
    }
    
    /**
     * 获取会话
     */
    public Optional<UserSession> getSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionData = (String) redisTemplate.opsForValue().get(sessionKey);
            
            if (StringUtils.hasText(sessionData)) {
                UserSession session = objectMapper.readValue(sessionData, UserSession.class);
                
                // 检查会话是否过期
                if (session.getExpiresAt().isAfter(LocalDateTime.now())) {
                    return Optional.of(session);
                } else {
                    // 会话已过期，删除
                    expireSession(sessionId);
                }
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("获取会话失败: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 验证会话
     */
    public boolean validateSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionIndexKey = SESSION_INDEX_PREFIX + sessionId;
            
            List<String> keys = Arrays.asList(sessionKey, sessionIndexKey);
            List<Object> args = Arrays.asList(System.currentTimeMillis());
            
            Boolean result = redisTemplate.execute(validateSessionScript, keys, args.toArray());
            return Boolean.TRUE.equals(result);
            
        } catch (Exception e) {
            logger.error("验证会话失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 更新会话最后访问时间
     */
    public boolean updateLastAccessTime(String sessionId) {
        try {
            Optional<UserSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.setLastAccessedAt(LocalDateTime.now());
                
                String sessionKey = SESSION_PREFIX + sessionId;
                String sessionData = objectMapper.writeValueAsString(session);
                
                // 更新会话数据，保持原有的TTL
                Long ttl = redisTemplate.getExpire(sessionKey, TimeUnit.SECONDS);
                if (ttl != null && ttl > 0) {
                    redisTemplate.opsForValue().set(sessionKey, sessionData, ttl, TimeUnit.SECONDS);
                    return true;
                }
            }
            return false;
            
        } catch (Exception e) {
            logger.error("更新会话访问时间失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 延长会话过期时间
     */
    public boolean extendSession(String sessionId, int extensionMinutes) {
        try {
            Optional<UserSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.setExpiresAt(session.getExpiresAt().plusMinutes(extensionMinutes));
                
                String sessionKey = SESSION_PREFIX + sessionId;
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                String sessionIndexKey = SESSION_INDEX_PREFIX + sessionId;
                
                String sessionData = objectMapper.writeValueAsString(session);
                long newExpirationSeconds = session.getExpiresAt().toEpochSecond(ZoneOffset.UTC) - 
                                          LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                
                if (newExpirationSeconds > 0) {
                    redisTemplate.opsForValue().set(sessionKey, sessionData, newExpirationSeconds, TimeUnit.SECONDS);
                    redisTemplate.expire(sessionIndexKey, newExpirationSeconds, TimeUnit.SECONDS);
                    redisTemplate.opsForSet().add(userSessionsKey, sessionId);
                    redisTemplate.expire(userSessionsKey, newExpirationSeconds, TimeUnit.SECONDS);
                    
                    logger.info("会话延长成功: sessionId={}, extensionMinutes={}", sessionId, extensionMinutes);
                    return true;
                }
            }
            return false;
            
        } catch (Exception e) {
            logger.error("延长会话失败: sessionId={}, extensionMinutes={}", sessionId, extensionMinutes, e);
            return false;
        }
    }
    
    /**
     * 使会话过期
     */
    public boolean expireSession(String sessionId) {
        try {
            Optional<UserSession> sessionOpt = getSession(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                
                String sessionKey = SESSION_PREFIX + sessionId;
                String userSessionsKey = USER_SESSIONS_PREFIX + session.getUserId();
                String sessionIndexKey = SESSION_INDEX_PREFIX + sessionId;
                
                List<String> keys = Arrays.asList(sessionKey, userSessionsKey, sessionIndexKey, ONLINE_USERS_KEY);
                List<Object> args = Arrays.asList(sessionId, session.getUserId().toString());
                
                Boolean result = redisTemplate.execute(expireSessionScript, keys, args.toArray());
                
                if (Boolean.TRUE.equals(result)) {
                    logger.info("会话过期成功: sessionId={}, userId={}", sessionId, session.getUserId());
                    updateSessionStats("expire", 1);
                    return true;
                }
            }
            return false;
            
        } catch (Exception e) {
            logger.error("使会话过期失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 使用户的所有会话过期
     */
    public int expireAllUserSessions(Long userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return 0;
            }
            
            int expiredCount = 0;
            for (Object sessionIdObj : sessionIds) {
                String sessionId = (String) sessionIdObj;
                if (expireSession(sessionId)) {
                    expiredCount++;
                }
            }
            
            // 清理用户会话集合
            redisTemplate.delete(userSessionsKey);
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
            
            logger.info("用户所有会话过期完成: userId={}, expiredCount={}", userId, expiredCount);
            return expiredCount;
            
        } catch (Exception e) {
            logger.error("使用户所有会话过期失败: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * 使用户的其他会话过期（保留当前会话）
     */
    public int expireOtherUserSessions(Long userId, String currentSessionId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return 0;
            }
            
            int expiredCount = 0;
            for (Object sessionIdObj : sessionIds) {
                String sessionId = (String) sessionIdObj;
                if (!sessionId.equals(currentSessionId) && expireSession(sessionId)) {
                    expiredCount++;
                }
            }
            
            logger.info("用户其他会话过期完成: userId={}, currentSessionId={}, expiredCount={}", 
                       userId, currentSessionId, expiredCount);
            return expiredCount;
            
        } catch (Exception e) {
            logger.error("使用户其他会话过期失败: userId={}, currentSessionId={}", userId, currentSessionId, e);
            return 0;
        }
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<UserSession> getUserSessions(Long userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
            
            if (sessionIds == null || sessionIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<UserSession> sessions = new ArrayList<>();
            for (Object sessionIdObj : sessionIds) {
                String sessionId = (String) sessionIdObj;
                Optional<UserSession> sessionOpt = getSession(sessionId);
                sessionOpt.ifPresent(sessions::add);
            }
            
            return sessions;
            
        } catch (Exception e) {
            logger.error("获取用户会话失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取用户的有效会话数量
     */
    public long getUserValidSessionCount(Long userId) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            Long count = redisTemplate.opsForSet().size(userSessionsKey);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("获取用户有效会话数量失败: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString()));
        } catch (Exception e) {
            logger.error("检查用户在线状态失败: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 获取在线用户列表
     */
    public List<Long> getOnlineUserIds() {
        try {
            Set<Object> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
            if (userIds == null || userIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            return userIds.stream()
                    .map(obj -> Long.parseLong((String) obj))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("获取在线用户列表失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 清理过期会话
     */
    public int cleanupExpiredSessions() {
        try {
            List<String> keys = Arrays.asList(SESSION_PREFIX + "*", USER_SESSIONS_PREFIX + "*", 
                                             SESSION_INDEX_PREFIX + "*", ONLINE_USERS_KEY);
            List<Object> args = Arrays.asList(System.currentTimeMillis());
            
            Long result = redisTemplate.execute(cleanupExpiredScript, keys, args.toArray());
            int cleanedCount = result != null ? result.intValue() : 0;
            
            if (cleanedCount > 0) {
                logger.info("清理过期会话完成: cleanedCount={}", cleanedCount);
                updateSessionStats("cleanup", cleanedCount);
            }
            
            return cleanedCount;
            
        } catch (Exception e) {
            logger.error("清理过期会话失败", e);
            return 0;
        }
    }
    
    /**
     * 获取会话统计信息
     */
    public SessionStatistics getSessionStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(SESSION_STATS_KEY);
            
            SessionStatistics statistics = new SessionStatistics();
            statistics.setTotalSessions(getLongValue(stats, "total", 0L));
            statistics.setActiveSessions(getLongValue(stats, "active", 0L));
            statistics.setExpiredSessions(getLongValue(stats, "expired", 0L));
            statistics.setOnlineUsers(redisTemplate.opsForSet().size(ONLINE_USERS_KEY));
            statistics.setTodaySessions(getLongValue(stats, "today", 0L));
            
            return statistics;
            
        } catch (Exception e) {
            logger.error("获取会话统计信息失败", e);
            return new SessionStatistics();
        }
    }
    
    /**
     * 更新会话统计信息
     */
    private void updateSessionStats(String operation, long count) {
        try {
            redisTemplate.opsForHash().increment(SESSION_STATS_KEY, operation, count);
            redisTemplate.opsForHash().increment(SESSION_STATS_KEY, "total", operation.equals("create") ? count : 0);
            redisTemplate.expire(SESSION_STATS_KEY, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.error("更新会话统计信息失败: operation={}, count={}", operation, count, e);
        }
    }
    
    /**
     * 获取Long值
     */
    private Long getLongValue(Map<Object, Object> map, String key, Long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    /**
     * 创建会话的Lua脚本
     */
    private DefaultRedisScript<Long> createSessionScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local sessionKey = KEYS[1]\n" +
            "local userSessionsKey = KEYS[2]\n" +
            "local sessionIndexKey = KEYS[3]\n" +
            "local onlineUsersKey = KEYS[4]\n" +
            "local sessionData = ARGV[1]\n" +
            "local userId = ARGV[2]\n" +
            "local sessionId = ARGV[3]\n" +
            "local expirationSeconds = tonumber(ARGV[4])\n" +
            "local timestamp = ARGV[5]\n" +
            "\n" +
            "redis.call('SET', sessionKey, sessionData, 'EX', expirationSeconds)\n" +
            "redis.call('SADD', userSessionsKey, sessionId)\n" +
            "redis.call('EXPIRE', userSessionsKey, expirationSeconds)\n" +
            "redis.call('SET', sessionIndexKey, timestamp, 'EX', expirationSeconds)\n" +
            "redis.call('SADD', onlineUsersKey, userId)\n" +
            "return 1"
        );
        script.setResultType(Long.class);
        return script;
    }
    
    /**
     * 验证会话的Lua脚本
     */
    private DefaultRedisScript<Boolean> validateSessionScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local sessionKey = KEYS[1]\n" +
            "local sessionIndexKey = KEYS[2]\n" +
            "local timestamp = ARGV[1]\n" +
            "\n" +
            "if redis.call('EXISTS', sessionKey) == 1 then\n" +
            "    redis.call('SET', sessionIndexKey, timestamp)\n" +
            "    return true\n" +
            "else\n" +
            "    return false\n" +
            "end"
        );
        script.setResultType(Boolean.class);
        return script;
    }
    
    /**
     * 使会话过期的Lua脚本
     */
    private DefaultRedisScript<Boolean> expireSessionScript() {
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local sessionKey = KEYS[1]\n" +
            "local userSessionsKey = KEYS[2]\n" +
            "local sessionIndexKey = KEYS[3]\n" +
            "local onlineUsersKey = KEYS[4]\n" +
            "local sessionId = ARGV[1]\n" +
            "local userId = ARGV[2]\n" +
            "\n" +
            "redis.call('DEL', sessionKey)\n" +
            "redis.call('SREM', userSessionsKey, sessionId)\n" +
            "redis.call('DEL', sessionIndexKey)\n" +
            "\n" +
            "local remainingSessions = redis.call('SCARD', userSessionsKey)\n" +
            "if remainingSessions == 0 then\n" +
            "    redis.call('SREM', onlineUsersKey, userId)\n" +
            "end\n" +
            "\n" +
            "return true"
        );
        script.setResultType(Boolean.class);
        return script;
    }
    
    /**
     * 清理过期会话的Lua脚本
     */
    private DefaultRedisScript<Long> cleanupExpiredScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local sessionPattern = ARGV[1]\n" +
            "local userSessionsPattern = ARGV[2]\n" +
            "local sessionIndexPattern = ARGV[3]\n" +
            "local onlineUsersKey = ARGV[4]\n" +
            "local currentTime = tonumber(ARGV[5])\n" +
            "\n" +
            "local cleanedCount = 0\n" +
            "\n" +
            "-- 这里应该实现具体的清理逻辑\n" +
            "-- 由于Lua脚本的复杂性，建议在Java代码中实现\n" +
            "\n" +
            "return cleanedCount"
        );
        script.setResultType(Long.class);
        return script;
    }
    
    /**
     * 会话统计信息类
     */
    public static class SessionStatistics {
        private Long totalSessions = 0L;
        private Long activeSessions = 0L;
        private Long expiredSessions = 0L;
        private Long onlineUsers = 0L;
        private Long todaySessions = 0L;
        
        // Getters and Setters
        public Long getTotalSessions() {
            return totalSessions;
        }
        
        public void setTotalSessions(Long totalSessions) {
            this.totalSessions = totalSessions;
        }
        
        public Long getActiveSessions() {
            return activeSessions;
        }
        
        public void setActiveSessions(Long activeSessions) {
            this.activeSessions = activeSessions;
        }
        
        public Long getExpiredSessions() {
            return expiredSessions;
        }
        
        public void setExpiredSessions(Long expiredSessions) {
            this.expiredSessions = expiredSessions;
        }
        
        public Long getOnlineUsers() {
            return onlineUsers;
        }
        
        public void setOnlineUsers(Long onlineUsers) {
            this.onlineUsers = onlineUsers;
        }
        
        public Long getTodaySessions() {
            return todaySessions;
        }
        
        public void setTodaySessions(Long todaySessions) {
            this.todaySessions = todaySessions;
        }
    }
}