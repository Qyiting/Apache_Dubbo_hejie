package com.hejiexmu.rpc.auth.repository;

import com.hejiexmu.rpc.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户会话数据访问接口
 * 
 * @author hejiexmu
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    /**
     * 根据会话ID查找会话
     */
    Optional<UserSession> findBySessionId(String sessionId);
    
    /**
     * 根据用户ID查找所有会话
     */
    List<UserSession> findByUserId(Long userId);
    
    /**
     * 根据用户ID查找有效会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId AND s.status = 1 AND s.expiresAt > :now")
    List<UserSession> findValidSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * 根据会话ID查找有效会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId AND s.status = 1 AND s.expiresAt > :now")
    Optional<UserSession> findValidSessionBySessionId(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    /**
     * 根据会话ID和有效性查找会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId AND (:valid = false OR (s.status = 1 AND s.expiresAt > CURRENT_TIMESTAMP))")
    Optional<UserSession> findBySessionIdAndValid(@Param("sessionId") String sessionId, @Param("valid") boolean valid);
    
    /**
     * 查找过期的会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt <= :now OR s.status = 0")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * 查找指定时间之前创建的会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.createdAt < :before")
    List<UserSession> findSessionsCreatedBefore(@Param("before") LocalDateTime before);
    
    /**
     * 查找指定时间之后最后访问的会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.lastAccessedAt > :after")
    List<UserSession> findSessionsLastAccessedAfter(@Param("after") LocalDateTime after);
    
    /**
     * 统计有效且未过期的会话数量
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = :valid AND s.expiresAt > :now")
    long countByValidAndExpiresAtAfter(@Param("valid") boolean valid, @Param("now") LocalDateTime now);
    
    /**
     * 统计指定用户的有效且未过期的会话数量
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user.id = :userId AND s.status = :valid AND s.expiresAt > :now")
    long countByUserIdAndValidAndExpiresAtAfter(@Param("userId") Long userId, @Param("valid") boolean valid, @Param("now") LocalDateTime now);
    
    /**
     * 根据IP地址查找会话
     */
    List<UserSession> findByIpAddress(String ipAddress);
    
    /**
     * 根据用户代理查找会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.userAgent LIKE %:userAgent%")
    List<UserSession> findByUserAgentContaining(@Param("userAgent") String userAgent);
    
    /**
     * 统计用户的有效会话数
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.user.id = :userId AND s.status = 1 AND s.expiresAt > :now")
    long countValidSessionsByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    
    /**
     * 统计总会话数
     */
    @Query("SELECT COUNT(s) FROM UserSession s")
    long countTotalSessions();
    
    /**
     * 统计有效会话数
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.status = 1 AND s.expiresAt > :now")
    long countValidSessions(@Param("now") LocalDateTime now);
    
    /**
     * 统计过期会话数
     */
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.expiresAt <= :now OR s.status = 0")
    long countExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * 删除过期的会话
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt <= :now OR s.status = 0")
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * 删除指定时间之前创建的会话
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.createdAt < :before")
    int deleteSessionsCreatedBefore(@Param("before") LocalDateTime before);
    
    /**
     * 使用户的所有会话过期
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.status = 0 WHERE s.user.id = :userId")
    int expireAllUserSessions(@Param("userId") Long userId);
    
    /**
     * 使指定会话过期
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.status = 0 WHERE s.sessionId = :sessionId")
    int expireSession(@Param("sessionId") String sessionId);
    
    /**
     * 更新会话的最后访问时间
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :now WHERE s.sessionId = :sessionId")
    int updateLastAccessTime(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);
    
    /**
     * 延长会话过期时间
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.expiresAt = :newExpiresAt WHERE s.sessionId = :sessionId")
    int extendSessionExpiration(@Param("sessionId") String sessionId, @Param("newExpiresAt") LocalDateTime newExpiresAt);
}