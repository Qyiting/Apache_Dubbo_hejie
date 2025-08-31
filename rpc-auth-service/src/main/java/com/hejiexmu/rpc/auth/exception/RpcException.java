package com.hejiexmu.rpc.auth.exception;

/**
 * RPC异常类
 * 用于RPC调用过程中的异常处理
 * 
 * @author hejiexmu
 */
public class RpcException extends Exception {
    
    private String code;
    
    public RpcException() {
        super();
    }
    
    public RpcException(String message) {
        super(message);
    }
    
    public RpcException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public RpcException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public RpcException(Throwable cause) {
        super(cause);
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
}