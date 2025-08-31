package com.hejiexmu.rpc.auth.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * RPC响应对象
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String requestId;
    private Object result;
    private Throwable exception;
    private boolean success;
    private String errorMessage;
    private Map<String, String> headers;
    private long timestamp;
    
    public RpcResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public static RpcResponse success(String requestId, Object result) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setResult(result);
        response.setSuccess(true);
        return response;
    }
    
    public static RpcResponse failure(String requestId, Throwable exception) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setException(exception);
        response.setSuccess(false);
        response.setErrorMessage(exception.getMessage());
        return response;
    }
    
    public static RpcResponse failure(String requestId, String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Object getResult() {
        return result;
    }
    
    public void setResult(Object result) {
        this.result = result;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public void setException(Throwable exception) {
        this.exception = exception;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "requestId='" + requestId + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}