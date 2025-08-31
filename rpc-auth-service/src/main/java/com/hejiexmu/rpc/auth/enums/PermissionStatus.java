package com.hejiexmu.rpc.auth.enums;

/**
 * 权限状态枚举
 */
public enum PermissionStatus {
    ACTIVE(1, "激活"),
    INACTIVE(0, "未激活");
    
    private final Integer code;
    private final String description;
    
    PermissionStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PermissionStatus fromCode(Integer code) {
        for (PermissionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown permission status code: " + code);
    }
}