package com.hejiexmu.rpc.auth.exception;

/**
 * 验证异常
 * 
 * @author hejiexmu
 */
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private String field;
    private Object rejectedValue;
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
    
    public ValidationException(String field, Object rejectedValue, String message) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getField() {
        return field;
    }
    
    public void setField(String field) {
        this.field = field;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }
}