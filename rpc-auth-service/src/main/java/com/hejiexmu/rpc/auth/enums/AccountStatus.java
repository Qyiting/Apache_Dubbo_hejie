package com.hejiexmu.rpc.auth.enums;

/**
 * 账户状态枚举
 */
public enum AccountStatus {
    ACTIVE("ACTIVE", "激活"),
    INACTIVE("INACTIVE", "未激活"),
    LOCKED("LOCKED", "锁定"),
    SUSPENDED("SUSPENDED", "暂停"),
    EXPIRED("EXPIRED", "已过期"),
    DELETED("DELETED", "已删除");
    
    private final String code;
    private final String description;
    
    AccountStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown account status code: " + code);
    }
}