package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.cache.RedisCacheService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * JWT令牌服务
 * 处理JWT的生成、验证和管理
 * 
 * @author hejiexmu
 */
@Service
public class JwtTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);
    
    // JWT配置
    @Value("${security.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${security.jwt.expiration:86400000}")
    private long jwtExpiration; // 24小时
    
    @Value("${security.jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 7天
    
    @Value("${security.jwt.header:Authorization}")
    private String jwtHeader;
    
    @Value("${security.jwt.prefix:Bearer }")
    private String jwtPrefix;
    
    // Redis缓存服务
    private final RedisCacheService redisCacheService;
    
    // 缓存键前缀
    private static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final String SESSION_PREFIX = "auth:session:";
    private static final String USER_SESSIONS_PREFIX = "auth:user:sessions:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    
    public JwtTokenService(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), jwtExpiration);
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        String refreshToken = createToken(claims, userDetails.getUsername(), refreshExpiration);
        
        // 将刷新令牌存储到Redis
        String key = REFRESH_TOKEN_PREFIX + userDetails.getUsername();
        redisCacheService.set(key, refreshToken, refreshExpiration / 1000, TimeUnit.SECONDS);
        
        return refreshToken;
    }
    
    /**
     * 生成会话令牌
     */
    public TokenPair generateTokenPair(UserDetails userDetails, String sessionId) {
        String accessToken = generateAccessToken(userDetails);
        String refreshToken = generateRefreshToken(userDetails);
        
        // 存储会话信息
        storeSessionInfo(sessionId, userDetails.getUsername(), accessToken);
        
        return new TokenPair(accessToken, refreshToken, jwtExpiration, refreshExpiration);
    }
    
    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object userId = claims.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return null;
        });
    }
    
    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * 从令牌中获取JWT ID
     */
    public String getJwtIdFromToken(String token) {
        return getClaimFromToken(token, Claims::getId);
    }
    
    /**
     * 从令牌中获取指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 从令牌中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.warn("Failed to parse JWT token: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            // 检查令牌格式和签名
            getAllClaimsFromToken(token);
            
            // 检查是否在黑名单中
            if (isTokenBlacklisted(token)) {
                logger.warn("Token is blacklisted");
                return false;
            }
            
            // 检查是否过期
            if (isTokenExpired(token)) {
                logger.warn("Token is expired");
                return false;
            }
            
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("JWT token validation failed: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 验证令牌与用户详情
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && validateToken(token);
        } catch (Exception e) {
            logger.warn("Token validation failed for user: {}", userDetails.getUsername(), e);
            return false;
        }
    }
    
    /**
     * 刷新令牌
     */
    public String refreshToken(String refreshToken) {
        try {
            if (!validateRefreshToken(refreshToken)) {
                throw new IllegalArgumentException("Invalid refresh token");
            }
            
            final String username = getUsernameFromToken(refreshToken);
            
            // 检查刷新令牌是否存在于Redis中
            String key = REFRESH_TOKEN_PREFIX + username;
            String storedToken = (String) redisCacheService.get(key);
            
            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new IllegalArgumentException("Refresh token not found or invalid");
            }
            
            // 生成新的访问令牌
            Map<String, Object> claims = new HashMap<>();
            return createToken(claims, username, jwtExpiration);
            
        } catch (Exception e) {
            logger.error("Failed to refresh token", e);
            throw new IllegalArgumentException("Token refresh failed", e);
        }
    }
    
    /**
     * 验证刷新令牌
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            Claims claims = getAllClaimsFromToken(refreshToken);
            String type = (String) claims.get("type");
            return "refresh".equals(type) && !isTokenExpired(refreshToken);
        } catch (Exception e) {
            logger.warn("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 将令牌加入黑名单
     */
    public void blacklistToken(String token) {
        try {
            String jwtId = getJwtIdFromToken(token);
            Date expiration = getExpirationDateFromToken(token);
            
            // 计算剩余过期时间
            long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (ttl > 0) {
                String key = TOKEN_BLACKLIST_PREFIX + jwtId;
                redisCacheService.set(key, "blacklisted", ttl, TimeUnit.SECONDS);
                logger.info("Token blacklisted: {}", jwtId);
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token", e);
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String jwtId = getJwtIdFromToken(token);
            String key = TOKEN_BLACKLIST_PREFIX + jwtId;
            return redisCacheService.exists(key);
        } catch (Exception e) {
            logger.warn("Failed to check token blacklist status", e);
            return false;
        }
    }
    
    /**
     * 存储会话信息
     */
    public void storeSessionInfo(String sessionId, String username, String accessToken) {
        try {
            SessionInfo sessionInfo = new SessionInfo(
                sessionId,
                username,
                accessToken,
                LocalDateTime.now(),
                LocalDateTime.now().plusSeconds(jwtExpiration / 1000)
            );
            
            // 存储会话信息
            String sessionKey = SESSION_PREFIX + sessionId;
            redisCacheService.set(sessionKey, sessionInfo, jwtExpiration / 1000, TimeUnit.SECONDS);
            
            // 添加到用户会话列表
            String userSessionsKey = USER_SESSIONS_PREFIX + username;
            redisCacheService.sAdd(userSessionsKey, sessionId);
            redisCacheService.expire(userSessionsKey, jwtExpiration / 1000, TimeUnit.SECONDS);
            
            logger.debug("Session info stored for user: {}, sessionId: {}", username, sessionId);
        } catch (Exception e) {
            logger.error("Failed to store session info", e);
        }
    }
    
    /**
     * 验证会话是否有效
     */
    public boolean isSessionValid(String sessionId, String username) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            SessionInfo sessionInfo = redisCacheService.get(sessionKey, SessionInfo.class);
            
            return sessionInfo != null && 
                   username.equals(sessionInfo.getUsername()) &&
                   sessionInfo.getExpiresAt().isAfter(LocalDateTime.now());
        } catch (Exception e) {
            logger.warn("Failed to validate session: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 删除会话
     */
    public void removeSession(String sessionId, String username) {
        try {
            // 删除会话信息
            String sessionKey = SESSION_PREFIX + sessionId;
            redisCacheService.delete(sessionKey);
            
            // 从用户会话列表中移除
            String userSessionsKey = USER_SESSIONS_PREFIX + username;
            redisCacheService.sRemove(userSessionsKey, sessionId);
            
            logger.info("Session removed for user: {}, sessionId: {}", username, sessionId);
        } catch (Exception e) {
            logger.error("Failed to remove session", e);
        }
    }
    
    /**
     * 删除用户的所有会话
     */
    public void removeAllUserSessions(String username) {
        try {
            String userSessionsKey = USER_SESSIONS_PREFIX + username;
            
            // 获取用户的所有会话ID
            Set<Object> sessionIds = redisCacheService.sMembers(userSessionsKey);
            
            // 删除所有会话
            for (Object sessionId : sessionIds) {
                String sessionKey = SESSION_PREFIX + sessionId.toString();
                redisCacheService.delete(sessionKey);
            }
            
            // 删除用户会话列表
            redisCacheService.delete(userSessionsKey);
            
            // 删除刷新令牌
            String refreshTokenKey = REFRESH_TOKEN_PREFIX + username;
            redisCacheService.delete(refreshTokenKey);
            
            logger.info("All sessions removed for user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to remove all user sessions", e);
        }
    }
    
    /**
     * 令牌对类
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final long accessTokenExpiration;
        private final long refreshTokenExpiration;
        
        public TokenPair(String accessToken, String refreshToken, 
                        long accessTokenExpiration, long refreshTokenExpiration) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessTokenExpiration = accessTokenExpiration;
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
        
        // Getters
        public String getAccessToken() {
            return accessToken;
        }
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public long getAccessTokenExpiration() {
            return accessTokenExpiration;
        }
        
        public long getRefreshTokenExpiration() {
            return refreshTokenExpiration;
        }
    }
    
    /**
     * 会话信息类
     */
    public static class SessionInfo {
        private String sessionId;
        private String username;
        private String accessToken;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        
        public SessionInfo() {}
        
        public SessionInfo(String sessionId, String username, String accessToken,
                          LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.accessToken = accessToken;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
        
        // Getters and Setters
        public String getSessionId() {
            return sessionId;
        }
        
        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}