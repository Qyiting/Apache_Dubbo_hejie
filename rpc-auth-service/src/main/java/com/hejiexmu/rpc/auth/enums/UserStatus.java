package com.hejiexmu.rpc.auth.enums;

/**
 * 用户状态枚举
 */
public enum UserStatus {
    ACTIVE("ACTIVE", "激活"),
    INACTIVE("INACTIVE", "未激活"),
    LOCKED("LOCKED", "锁定"),
    SUSPENDED("SUSPENDED", "暂停"),
    PENDING("PENDING", "待审核"),
    DELETED("DELETED", "已删除");
    
    private final String code;
    private final String description;
    
    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static UserStatus fromCode(String code) {
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status code: " + code);
    }
}