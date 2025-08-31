package com.hejiexmu.rpc.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * 密码工具类
 * 
 * @author hejiexmu
 */
@Component
public class PasswordUtil {
    
    private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    
    // 密码强度正则表达式
    private static final Pattern WEAK_PASSWORD = Pattern.compile("^.{1,5}$");
    private static final Pattern MEDIUM_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z]).{6,11}$|^(?=.*[a-z])(?=.*\\d).{6,11}$|^(?=.*[A-Z])(?=.*\\d).{6,11}$");
    private static final Pattern STRONG_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$");
    private static final Pattern VERY_STRONG_PASSWORD = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{12,}$");
    
    // 常见弱密码列表
    private static final String[] COMMON_WEAK_PASSWORDS = {
        "123456", "password", "123456789", "12345678", "12345", "1234567", "1234567890",
        "qwerty", "abc123", "111111", "123123", "admin", "letmein", "welcome",
        "monkey", "1234", "dragon", "pass", "master", "hello", "freedom", "whatever",
        "qazwsx", "trustno1", "jordan", "harley", "1234qwer", "sunshine", "iloveyou"
    };
    
    public PasswordUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder(12); // 使用强度12的BCrypt
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * 加密密码
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        
        try {
            return passwordEncoder.encode(rawPassword);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败", e);
        }
    }
    
    /**
     * 加密密码（别名方法）
     */
    public String encode(String rawPassword) {
        return encodePassword(rawPassword);
    }
    
    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }
    
    /**
     * 检查密码强度
     */
    public PasswordStrength checkPasswordStrength(String password) {
        if (password == null || password.trim().isEmpty()) {
            return PasswordStrength.INVALID;
        }
        
        // 检查是否为常见弱密码
        if (isCommonWeakPassword(password)) {
            return PasswordStrength.VERY_WEAK;
        }
        
        // 检查密码长度和复杂度
        if (WEAK_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.VERY_WEAK;
        }
        
        if (VERY_STRONG_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.VERY_STRONG;
        }
        
        if (STRONG_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.STRONG;
        }
        
        if (MEDIUM_PASSWORD.matcher(password).matches()) {
            return PasswordStrength.MEDIUM;
        }
        
        return PasswordStrength.WEAK;
    }
    
    /**
     * 检查是否为常见弱密码
     */
    private boolean isCommonWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        for (String weakPassword : COMMON_WEAK_PASSWORDS) {
            if (weakPassword.equals(lowerPassword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 验证密码格式
     */
    public PasswordValidationResult validatePassword(String password) {
        PasswordValidationResult result = new PasswordValidationResult();
        
        if (password == null || password.trim().isEmpty()) {
            result.setValid(false);
            result.addError("密码不能为空");
            return result;
        }
        
        // 长度检查
        if (password.length() < 6) {
            result.addError("密码长度不能少于6个字符");
        }
        
        if (password.length() > 100) {
            result.addError("密码长度不能超过100个字符");
        }
        
        // 复杂度检查
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[@$!%*?&].*");
        
        if (!hasLower) {
            result.addError("密码必须包含至少一个小写字母");
        }
        
        if (!hasUpper) {
            result.addError("密码必须包含至少一个大写字母");
        }
        
        if (!hasDigit) {
            result.addError("密码必须包含至少一个数字");
        }
        
        // 强度检查
        PasswordStrength strength = checkPasswordStrength(password);
        result.setStrength(strength);
        
        if (strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK) {
            result.addWarning("密码强度较弱，建议使用更复杂的密码");
        }
        
        // 常见弱密码检查
        if (isCommonWeakPassword(password)) {
            result.addError("不能使用常见的弱密码");
        }
        
        result.setValid(result.getErrors().isEmpty());
        return result;
    }
    
    /**
     * 生成随机密码
     */
    public String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8;
        }
        
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String special = "@$!%*?&";
        String allChars = lowercase + uppercase + digits + special;
        
        StringBuilder password = new StringBuilder();
        
        // 确保至少包含每种类型的字符
        password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
        password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        password.append(digits.charAt(secureRandom.nextInt(digits.length())));
        password.append(special.charAt(secureRandom.nextInt(special.length())));
        
        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(secureRandom.nextInt(allChars.length())));
        }
        
        // 打乱字符顺序
        return shuffleString(password.toString());
    }
    
    /**
     * 打乱字符串
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
    
    /**
     * 生成盐值
     */
    public String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return java.util.Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        INVALID("无效", 0),
        VERY_WEAK("非常弱", 1),
        WEAK("弱", 2),
        MEDIUM("中等", 3),
        STRONG("强", 4),
        VERY_STRONG("非常强", 5);
        
        private final String description;
        private final int level;
        
        PasswordStrength(String description, int level) {
            this.description = description;
            this.level = level;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * 密码验证结果
     */
    public static class PasswordValidationResult {
        private boolean valid;
        private PasswordStrength strength;
        private java.util.List<String> errors = new java.util.ArrayList<>();
        private java.util.List<String> warnings = new java.util.ArrayList<>();
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public PasswordStrength getStrength() {
            return strength;
        }
        
        public void setStrength(PasswordStrength strength) {
            this.strength = strength;
        }
        
        public java.util.List<String> getErrors() {
            return errors;
        }
        
        public java.util.List<String> getWarnings() {
            return warnings;
        }
        
        public void addError(String error) {
            this.errors.add(error);
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }
}