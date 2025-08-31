package com.hejiexmu.rpc.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 会话工具类
 * 
 * @author hejiexmu
 */
@Slf4j
@Component
public class SessionUtil {
    
    private final SecureRandom secureRandom;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{32,64}$");
    
    public SessionUtil() {
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * 生成会话ID
     */
    public String generateSessionId() {
        return generateSessionId(48); // 默认48位长度
    }
    
    /**
     * 生成指定长度的会话ID
     */
    public String generateSessionId(int length) {
        if (length < 32) {
            length = 32; // 最小32位
        }
        if (length > 64) {
            length = 64; // 最大64位
        }
        
        StringBuilder sessionId = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sessionId.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        
        return sessionId.toString();
    }
    
    /**
     * 生成基于UUID的会话ID
     */
    public String generateUuidSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 生成带时间戳的会话ID
     */
    public String generateTimestampSessionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = generateSessionId(32);
        return timestamp + randomPart;
    }
    
    /**
     * 验证会话ID格式
     */
    public boolean isValidSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        return SESSION_ID_PATTERN.matcher(sessionId).matches();
    }
    
    /**
     * 计算会话过期时间
     */
    public LocalDateTime calculateExpirationTime(int expirationMinutes) {
        return LocalDateTime.now().plusMinutes(expirationMinutes);
    }
    
    /**
     * 计算会话过期时间（秒）
     */
    public LocalDateTime calculateExpirationTimeInSeconds(int expirationSeconds) {
        return LocalDateTime.now().plusSeconds(expirationSeconds);
    }
    
    /**
     * 检查会话是否过期
     */
    public boolean isSessionExpired(LocalDateTime expirationTime) {
        return expirationTime != null && expirationTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * 计算会话剩余时间（秒）
     */
    public long calculateRemainingTime(LocalDateTime expirationTime) {
        if (expirationTime == null) {
            return 0L;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (expirationTime.isBefore(now)) {
            return 0L;
        }
        
        return java.time.Duration.between(now, expirationTime).getSeconds();
    }
    
    /**
     * 延长会话过期时间
     */
    public LocalDateTime extendSessionExpiration(LocalDateTime currentExpiration, int extensionMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpiration = now.plusMinutes(extensionMinutes);
        
        // 如果当前过期时间还没到，则在当前过期时间基础上延长
        if (currentExpiration != null && currentExpiration.isAfter(now)) {
            newExpiration = currentExpiration.plusMinutes(extensionMinutes);
        }
        
        return newExpiration;
    }
    
    /**
     * 解析用户代理信息
     */
    public UserAgentInfo parseUserAgent(String userAgent) {
        UserAgentInfo info = new UserAgentInfo();
        
        if (userAgent == null || userAgent.trim().isEmpty()) {
            info.setBrowser("Unknown");
            info.setOperatingSystem("Unknown");
            info.setDeviceType("Unknown");
            return info;
        }
        
        String ua = userAgent.toLowerCase();
        
        // 解析浏览器
        if (ua.contains("chrome")) {
            info.setBrowser("Chrome");
        } else if (ua.contains("firefox")) {
            info.setBrowser("Firefox");
        } else if (ua.contains("safari")) {
            info.setBrowser("Safari");
        } else if (ua.contains("edge")) {
            info.setBrowser("Edge");
        } else if (ua.contains("opera")) {
            info.setBrowser("Opera");
        } else {
            info.setBrowser("Unknown");
        }
        
        // 解析操作系统
        if (ua.contains("windows")) {
            info.setOperatingSystem("Windows");
        } else if (ua.contains("mac")) {
            info.setOperatingSystem("macOS");
        } else if (ua.contains("linux")) {
            info.setOperatingSystem("Linux");
        } else if (ua.contains("android")) {
            info.setOperatingSystem("Android");
        } else if (ua.contains("ios")) {
            info.setOperatingSystem("iOS");
        } else {
            info.setOperatingSystem("Unknown");
        }
        
        // 解析设备类型
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            info.setDeviceType("Mobile");
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            info.setDeviceType("Tablet");
        } else {
            info.setDeviceType("Desktop");
        }
        
        return info;
    }
    
    /**
     * 验证IP地址格式
     */
    public boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }
        
        // IPv4正则表达式
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        
        // IPv6正则表达式（简化版）
        String ipv6Pattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        
        return ipAddress.matches(ipv4Pattern) || ipAddress.matches(ipv6Pattern);
    }
    
    /**
     * 获取IP地址类型
     */
    public String getIpAddressType(String ipAddress) {
        if (!isValidIpAddress(ipAddress)) {
            return "Invalid";
        }
        
        if (ipAddress.contains(":")) {
            return "IPv6";
        } else {
            return "IPv4";
        }
    }
    
    /**
     * 检查是否为内网IP
     */
    public boolean isPrivateIpAddress(String ipAddress) {
        if (!isValidIpAddress(ipAddress) || ipAddress.contains(":")) {
            return false; // 暂不处理IPv6
        }
        
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            
            // 10.0.0.0/8
            if (first == 10) {
                return true;
            }
            
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) {
                return true;
            }
            
            // 192.168.0.0/16
            if (first == 192 && second == 168) {
                return true;
            }
            
            // 127.0.0.0/8 (localhost)
            if (first == 127) {
                return true;
            }
            
        } catch (NumberFormatException e) {
            return false;
        }
        
        return false;
    }
    
    /**
     * 用户代理信息类
     */
    public static class UserAgentInfo {
        private String browser;
        private String operatingSystem;
        private String deviceType;
        
        public String getBrowser() {
            return browser;
        }
        
        public void setBrowser(String browser) {
            this.browser = browser;
        }
        
        public String getOperatingSystem() {
            return operatingSystem;
        }
        
        public void setOperatingSystem(String operatingSystem) {
            this.operatingSystem = operatingSystem;
        }
        
        public String getDeviceType() {
            return deviceType;
        }
        
        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }
        
        @Override
        public String toString() {
            return String.format("%s on %s (%s)", browser, operatingSystem, deviceType);
        }
    }
}