package com.hejiexmu.rpc.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JWT工具类
 * 
 * @author hejiexmu
 */
@Component
public class JwtUtil {
    
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${rpc.auth.jwt.secret:rpc-auth-secret-key-for-jwt-token-generation-and-validation}")
    private String jwtSecret;
    
    @Value("${rpc.auth.jwt.access-token-expiration:3600}")
    private Long accessTokenExpiration;
    
    @Value("${rpc.auth.jwt.refresh-token-expiration:604800}")
    private Long refreshTokenExpiration;
    
    @Value("${rpc.auth.jwt.issuer:rpc-auth-service}")
    private String issuer;
    
    private SecretKey secretKey;
    
    @PostConstruct
    public void init() {
        // 确保密钥长度足够
        if (jwtSecret.length() < 32) {
            jwtSecret = jwtSecret + "0123456789abcdef".repeat((32 - jwtSecret.length()) / 16 + 1);
            jwtSecret = jwtSecret.substring(0, 32);
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, String username, String sessionId, Set<String> roles, Set<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("sessionId", sessionId);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("tokenType", "access");
        
        return createToken(claims, accessTokenExpiration);
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username, String sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("sessionId", sessionId);
        claims.put("tokenType", "refresh");
        
        return createToken(claims, refreshTokenExpiration);
    }
    
    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("userId", Long.class) : null;
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }
    
    /**
     * 从令牌中获取会话ID
     */
    public String getSessionIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("sessionId", String.class) : null;
    }
    
    /**
     * 从令牌中获取角色
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object roles = claims.get("roles");
            if (roles instanceof Set) {
                return (Set<String>) roles;
            }
        }
        return null;
    }
    
    /**
     * 从令牌中获取权限
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object permissions = claims.get("permissions");
            if (permissions instanceof Set) {
                return (Set<String>) permissions;
            }
        }
        return null;
    }
    
    /**
     * 从令牌中获取令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get("tokenType", String.class) : null;
    }
    
    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }
    
    /**
     * 获取令牌签发时间
     */
    public Date getIssuedAtFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getIssuedAt() : null;
    }
    
    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.warn("检查令牌过期时间失败: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证令牌并检查类型
     */
    public boolean validateToken(String token, String expectedType) {
        if (!validateToken(token)) {
            return false;
        }
        
        String tokenType = getTokenTypeFromToken(token);
        return expectedType.equals(tokenType);
    }
    
    /**
     * 从令牌中获取Claims
     */
    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("解析令牌Claims失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取令牌剩余有效时间（秒）
     */
    public Long getRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            if (expiration == null) {
                return 0L;
            }
            
            long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0L, remaining);
        } catch (Exception e) {
            log.warn("获取令牌剩余时间失败: {}", e.getMessage());
            return 0L;
        }
    }
    
    /**
     * 刷新令牌（生成新的访问令牌）
     */
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken, "refresh")) {
            return null;
        }
        
        Long userId = getUserIdFromToken(refreshToken);
        String username = getUsernameFromToken(refreshToken);
        String sessionId = getSessionIdFromToken(refreshToken);
        
        if (userId == null || username == null || sessionId == null) {
            return null;
        }
        
        // 注意：这里需要重新获取用户的角色和权限，因为可能已经发生变化
        // 在实际使用中，应该调用服务层方法获取最新的角色和权限
        return generateAccessToken(userId, username, sessionId, null, null);
    }
    
    /**
     * 将LocalDateTime转换为Date
     */
    public Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
    
    /**
     * 将Date转换为LocalDateTime
     */
    public LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}