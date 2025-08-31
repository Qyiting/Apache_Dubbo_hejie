package com.hejiexmu.rpc.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一API响应格式
 * 
 * @author hejiexmu
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Long timestamp;
    private String path;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "操作成功", data);
    }
    
    /**
     * 成功响应（带消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, "SUCCESS", "操作成功", null);
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
    
    /**
     * 错误响应（带数据）
     */
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }
    
    /**
     * 通用错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, "ERROR", message, null);
    }
    
    /**
     * 参数验证错误
     */
    public static <T> ApiResponse<T> validationError(String message) {
        return new ApiResponse<>(false, "VALIDATION_ERROR", message, null);
    }
    
    /**
     * 认证错误
     */
    public static <T> ApiResponse<T> authError(String message) {
        return new ApiResponse<>(false, "AUTH_ERROR", message, null);
    }
    
    /**
     * 权限错误
     */
    public static <T> ApiResponse<T> permissionError(String message) {
        return new ApiResponse<>(false, "PERMISSION_ERROR", message, null);
    }
    
    /**
     * 资源未找到错误
     */
    public static <T> ApiResponse<T> notFoundError(String message) {
        return new ApiResponse<>(false, "NOT_FOUND", message, null);
    }
    
    /**
     * 服务器内部错误
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(false, "INTERNAL_ERROR", message, null);
    }
    
    /**
     * 业务逻辑错误
     */
    public static <T> ApiResponse<T> businessError(String message) {
        return new ApiResponse<>(false, "BUSINESS_ERROR", message, null);
    }
    
    /**
     * 请求过于频繁错误
     */
    public static <T> ApiResponse<T> rateLimitError(String message) {
        return new ApiResponse<>(false, "RATE_LIMIT_ERROR", message, null);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
               "success=" + success +
               ", code='" + code + '\'' +
               ", message='" + message + '\'' +
               ", data=" + data +
               ", timestamp=" + timestamp +
               ", path='" + path + '\'' +
               '}';
    }
}