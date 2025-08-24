package com.rpc.core.response;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 何杰
 * @version 1.0
 */
@Data
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum StatusCode {
        /** 成功 */
        SUCCESS((byte) 1, "成功"),
        /** 失败 */
        FAIL((byte)2, "失败"),
        /** 服务未找到 */
        NOT_FOUND((byte)3, "服务未找到"),
        /** 请求超时 */
        TIMEOUT((byte)4, "请求超时"),
        /** 服务器内部错误 */
        SERVER_ERROR((byte)5, "服务器内部错误"),
        /** 序列化错误 */
        SERIALIZATION_ERROR((byte)6, "序列化错误"),
        /** 客户端错误 */
        CLIENT_ERROR((byte)7, "客户端错误");
        private final byte code;
        private final String message;

        StatusCode(byte code, String message) {
            this.code = code;
            this.message = message;
        }
        public byte getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
    }
    private StatusCode statusCode; // 响应状态码
    private Long requestId; // 对应的请求ID
    private Object result; // 调用结果
    private Throwable exception; // 异常信息
    private String message; // 响应消息
    private long timestamp; // 响应时间搓

    public RpcResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public RpcResponse(Long requestId) {
        this();
        this.requestId = requestId;
    }

    // 创建成功响应
    public static RpcResponse success(Long requestId, Object result) {
        RpcResponse rpcResponse = new RpcResponse(requestId);
        rpcResponse.setStatusCode(StatusCode.SUCCESS);
        rpcResponse.setResult(result);
        rpcResponse.setMessage("调用成功");
        return rpcResponse;
    }

    // 创建失败响应
    public static RpcResponse fail(Long requestId, Throwable exception) {
        RpcResponse rpcResponse = new RpcResponse(requestId);
        rpcResponse.setStatusCode(StatusCode.FAIL);
        rpcResponse.setException(exception);
        rpcResponse.setMessage(exception != null?exception.getMessage():"调用失败");
        return rpcResponse;
    }

    public static RpcResponse fail(Long requestId, StatusCode statusCode, String message) {
        RpcResponse rpcResponse = new RpcResponse(requestId);
        rpcResponse.setStatusCode(statusCode);
        rpcResponse.setMessage(message);
        return  rpcResponse;
    }

    public static RpcResponse notFound(Long requestId, String message) {
        RpcResponse rpcResponse = new RpcResponse(requestId);
        rpcResponse.setStatusCode(StatusCode.NOT_FOUND);
        rpcResponse.setMessage(message);
        return rpcResponse;
    }

    /**
     * 判断响应是否成功
     *
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return statusCode == StatusCode.SUCCESS;
    }

    /**
     * 判断响应是否有异常
     *
     * @return true表示有异常，false表示无异常
     */
    public boolean hasException() {
        return exception != null;
    }
}
