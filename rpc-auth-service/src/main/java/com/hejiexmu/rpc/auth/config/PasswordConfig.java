package com.hejiexmu.rpc.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器配置
 * 用于密码的加密和验证
 * 
 * @author hejiexmu
 */
@Configuration
public class PasswordConfig {
    
    /**
     * 配置BCrypt密码编码器
     * BCrypt是一种安全的哈希算法，适合用于密码存储
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用强度为12的BCrypt编码器
        // 强度越高，计算时间越长，安全性越高
        // 12是一个平衡安全性和性能的好选择
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * 密码验证工具类
     */
    @Bean
    public PasswordValidator passwordValidator() {
        return new PasswordValidator();
    }
    
    /**
     * 密码验证器
     */
    public static class PasswordValidator {
        
        // 密码策略配置
        private static final int MIN_LENGTH = 8;
        private static final int MAX_LENGTH = 128;
        private static final boolean REQUIRE_UPPERCASE = true;
        private static final boolean REQUIRE_LOWERCASE = true;
        private static final boolean REQUIRE_DIGIT = true;
        private static final boolean REQUIRE_SPECIAL_CHAR = true;
        
        // 特殊字符集合
        private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        
        /**
         * 验证密码强度
         */
        public PasswordValidationResult validatePassword(String password) {
            if (password == null) {
                return new PasswordValidationResult(false, "密码不能为空");
            }
            
            // 检查长度
            if (password.length() < MIN_LENGTH) {
                return new PasswordValidationResult(false, 
                    String.format("密码长度不能少于%d个字符", MIN_LENGTH));
            }
            
            if (password.length() > MAX_LENGTH) {
                return new PasswordValidationResult(false, 
                    String.format("密码长度不能超过%d个字符", MAX_LENGTH));
            }
            
            // 检查是否包含大写字母
            if (REQUIRE_UPPERCASE && !containsUppercase(password)) {
                return new PasswordValidationResult(false, "密码必须包含至少一个大写字母");
            }
            
            // 检查是否包含小写字母
            if (REQUIRE_LOWERCASE && !containsLowercase(password)) {
                return new PasswordValidationResult(false, "密码必须包含至少一个小写字母");
            }
            
            // 检查是否包含数字
            if (REQUIRE_DIGIT && !containsDigit(password)) {
                return new PasswordValidationResult(false, "密码必须包含至少一个数字");
            }
            
            // 检查是否包含特殊字符
            if (REQUIRE_SPECIAL_CHAR && !containsSpecialChar(password)) {
                return new PasswordValidationResult(false, 
                    String.format("密码必须包含至少一个特殊字符：%s", SPECIAL_CHARS));
            }
            
            // 检查是否包含常见弱密码模式
            if (isCommonWeakPassword(password)) {
                return new PasswordValidationResult(false, "密码过于简单，请使用更复杂的密码");
            }
            
            return new PasswordValidationResult(true, "密码强度符合要求");
        }
        
        /**
         * 检查是否包含大写字母
         */
        private boolean containsUppercase(String password) {
            return password.chars().anyMatch(Character::isUpperCase);
        }
        
        /**
         * 检查是否包含小写字母
         */
        private boolean containsLowercase(String password) {
            return password.chars().anyMatch(Character::isLowerCase);
        }
        
        /**
         * 检查是否包含数字
         */
        private boolean containsDigit(String password) {
            return password.chars().anyMatch(Character::isDigit);
        }
        
        /**
         * 检查是否包含特殊字符
         */
        private boolean containsSpecialChar(String password) {
            return password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0);
        }
        
        /**
         * 检查是否是常见的弱密码
         */
        private boolean isCommonWeakPassword(String password) {
            String lowerPassword = password.toLowerCase();
            
            // 常见弱密码列表
            String[] weakPasswords = {
                "password", "123456", "12345678", "qwerty", "abc123",
                "password123", "admin", "letmein", "welcome", "monkey",
                "1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"
            };
            
            for (String weak : weakPasswords) {
                if (lowerPassword.contains(weak)) {
                    return true;
                }
            }
            
            // 检查是否是连续字符
            if (isSequentialChars(password)) {
                return true;
            }
            
            // 检查是否是重复字符
            if (isRepeatingChars(password)) {
                return true;
            }
            
            return false;
        }
        
        /**
         * 检查是否是连续字符
         */
        private boolean isSequentialChars(String password) {
            if (password.length() < 3) {
                return false;
            }
            
            for (int i = 0; i < password.length() - 2; i++) {
                char c1 = password.charAt(i);
                char c2 = password.charAt(i + 1);
                char c3 = password.charAt(i + 2);
                
                // 检查连续递增或递减
                if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * 检查是否是重复字符
         */
        private boolean isRepeatingChars(String password) {
            if (password.length() < 3) {
                return false;
            }
            
            for (int i = 0; i < password.length() - 2; i++) {
                char c = password.charAt(i);
                if (password.charAt(i + 1) == c && password.charAt(i + 2) == c) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * 生成密码强度评分
         */
        public PasswordStrength calculatePasswordStrength(String password) {
            if (password == null || password.isEmpty()) {
                return PasswordStrength.VERY_WEAK;
            }
            
            int score = 0;
            
            // 长度评分
            if (password.length() >= 8) score += 1;
            if (password.length() >= 12) score += 1;
            if (password.length() >= 16) score += 1;
            
            // 字符类型评分
            if (containsLowercase(password)) score += 1;
            if (containsUppercase(password)) score += 1;
            if (containsDigit(password)) score += 1;
            if (containsSpecialChar(password)) score += 1;
            
            // 复杂度评分
            if (!isCommonWeakPassword(password)) score += 1;
            
            // 根据评分返回强度等级
            if (score <= 2) return PasswordStrength.VERY_WEAK;
            if (score <= 4) return PasswordStrength.WEAK;
            if (score <= 6) return PasswordStrength.MEDIUM;
            if (score <= 7) return PasswordStrength.STRONG;
            return PasswordStrength.VERY_STRONG;
        }
    }
    
    /**
     * 密码验证结果
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final String message;
        
        public PasswordValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        VERY_WEAK("非常弱"),
        WEAK("弱"),
        MEDIUM("中等"),
        STRONG("强"),
        VERY_STRONG("非常强");
        
        private final String description;
        
        PasswordStrength(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}