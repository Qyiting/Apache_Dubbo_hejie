package com.hejiexmu.rpc.auth.enums;

/**
 * 角色状态枚举
 */
public enum RoleStatus {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");
    
    private final Integer code;
    private final String description;
    
    RoleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static RoleStatus fromCode(Integer code) {
        for (RoleStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown role status code: " + code);
    }
}