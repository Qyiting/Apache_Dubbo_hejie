package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.dto.SessionDTO;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话服务接口
 * 
 * @author hejiexmu
 */
public interface SessionService {
    
    /**
     * 创建用户会话
     */
    UserSession createSession(User user, String ipAddress, String userAgent, boolean rememberMe);
    
    /**
     * 根据会话ID查找会话
     */
    Optional<UserSession> findBySessionId(String sessionId);
    
    /**
     * 根据会话ID查找有效会话
     */
    Optional<UserSession> findValidSession(String sessionId);
    
    /**
     * 验证会话
     */
    boolean validateSession(String sessionId);
    
    /**
     * 验证会话并更新最后访问时间
     */
    boolean validateAndUpdateSession(String sessionId);
    
    /**
     * 更新会话最后访问时间
     */
    boolean updateLastAccessTime(String sessionId);
    
    /**
     * 延长会话过期时间
     */
    boolean extendSession(String sessionId, int extensionMinutes);
    
    /**
     * 使会话过期
     */
    boolean expireSession(String sessionId);
    
    /**
     * 使用户的所有会话过期
     */
    int expireAllUserSessions(Long userId);
    
    /**
     * 使用户的其他会话过期（保留当前会话）
     */
    int expireOtherUserSessions(Long userId, String currentSessionId);
    
    /**
     * 获取用户的所有会话
     */
    List<UserSession> getUserSessions(Long userId);
    
    /**
     * 获取用户的有效会话
     */
    List<UserSession> getUserValidSessions(Long userId);
    
    /**
     * 获取用户的会话DTO列表
     */
    List<SessionDTO> getUserSessionDTOs(Long userId);
    
    /**
     * 获取用户的有效会话数量
     */
    long getUserValidSessionCount(Long userId);
    
    /**
     * 检查用户是否在线
     */
    boolean isUserOnline(Long userId);
    
    /**
     * 获取会话详细信息
     */
    SessionDTO getSessionDetails(String sessionId);
    
    /**
     * 获取会话剩余时间（秒）
     */
    long getSessionRemainingTime(String sessionId);
    
    /**
     * 分页查询所有会话
     */
    Page<SessionDTO> findAllSessions(Pageable pageable);
    
    /**
     * 分页查询有效会话
     */
    Page<SessionDTO> findValidSessions(Pageable pageable);
    
    /**
     * 分页查询过期会话
     */
    Page<SessionDTO> findExpiredSessions(Pageable pageable);
    
    /**
     * 根据IP地址查询会话
     */
    List<SessionDTO> findSessionsByIpAddress(String ipAddress);
    
    /**
     * 根据用户代理查询会话
     */
    List<SessionDTO> findSessionsByUserAgent(String userAgent);
    
    /**
     * 清理过期会话
     */
    int cleanupExpiredSessions();
    
    /**
     * 清理指定时间之前创建的会话
     */
    int cleanupOldSessions(LocalDateTime before);
    
    /**
     * 获取会话统计信息
     */
    SessionStatistics getSessionStatistics();
    
    /**
     * 获取在线用户列表
     */
    List<Long> getOnlineUserIds();
    
    /**
     * 获取最近活跃的会话
     */
    List<SessionDTO> getRecentActiveSessions(int limit);
    
    /**
     * 强制用户下线
     */
    boolean forceUserOffline(Long userId);
    
    /**
     * 强制会话下线
     */
    boolean forceSessionOffline(String sessionId);
    
    /**
     * 检查会话是否属于指定用户
     */
    boolean isSessionOwnedByUser(String sessionId, Long userId);
    
    /**
     * 获取用户在指定IP的会话数量
     */
    long getUserSessionCountByIp(Long userId, String ipAddress);
    
    /**
     * 限制用户并发会话数量
     */
    boolean limitUserConcurrentSessions(Long userId, int maxSessions);
    
    /**
     * 检查IP地址的会话数量是否超限
     */
    boolean isIpSessionLimitExceeded(String ipAddress, int maxSessions);
    
    /**
     * 获取会话的地理位置信息
     */
    String getSessionLocation(String sessionId);
    
    /**
     * 标记可疑会话
     */
    boolean markSessionAsSuspicious(String sessionId, String reason);
    
    /**
     * 获取可疑会话列表
     */
    List<SessionDTO> getSuspiciousSessions();
    
    /**
     * 会话统计信息内部类
     */
    class SessionStatistics {
        private long totalSessions;
        private long validSessions;
        private long expiredSessions;
        private long onlineUsers;
        private long todaySessions;
        private long averageSessionDuration;
        private long maxConcurrentSessions;
        
        // Getters and Setters
        public long getTotalSessions() {
            return totalSessions;
        }
        
        public void setTotalSessions(long totalSessions) {
            this.totalSessions = totalSessions;
        }
        
        public long getValidSessions() {
            return validSessions;
        }
        
        public void setValidSessions(long validSessions) {
            this.validSessions = validSessions;
        }
        
        public long getExpiredSessions() {
            return expiredSessions;
        }
        
        public void setExpiredSessions(long expiredSessions) {
            this.expiredSessions = expiredSessions;
        }
        
        public long getOnlineUsers() {
            return onlineUsers;
        }
        
        public void setOnlineUsers(long onlineUsers) {
            this.onlineUsers = onlineUsers;
        }
        
        public long getTodaySessions() {
            return todaySessions;
        }
        
        public void setTodaySessions(long todaySessions) {
            this.todaySessions = todaySessions;
        }
        
        public long getAverageSessionDuration() {
            return averageSessionDuration;
        }
        
        public void setAverageSessionDuration(long averageSessionDuration) {
            this.averageSessionDuration = averageSessionDuration;
        }
        
        public long getMaxConcurrentSessions() {
            return maxConcurrentSessions;
        }
        
        public void setMaxConcurrentSessions(long maxConcurrentSessions) {
            this.maxConcurrentSessions = maxConcurrentSessions;
        }
    }
}