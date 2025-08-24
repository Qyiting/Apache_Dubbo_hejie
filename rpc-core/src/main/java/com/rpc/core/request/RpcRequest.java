package com.rpc.core.request;


import lombok.Data;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 何杰
 * @version 1.0
 * RpcRequest RPC请求协议对象
 */
@Data
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 请求ID生成器，确保每个请求都有唯一标识
     */
    private static final AtomicLong REQUEST_ID_GENERATOR = new AtomicLong(0);
    private Long requestId; // 请求唯一标识
    private String interfaceName; //接口名称
    private String methodName; // 方法名称
    private Class<?>[] parameterTypes; // 参数类型数组
    private Object[] parameters; // 参数值数组
    private String version = "1.0.0"; // 服务版本，用于服务版本控制
    private String group = "default"; // 服务分组，用于服务分组管理
    private long timeout = 5000L; // 超时时间（毫秒）

    /**
     * 默认构造函数
     */
    public RpcRequest() {
        this.requestId = REQUEST_ID_GENERATOR.getAndIncrement();
    }

    public RpcRequest(String interfaceName, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        this();
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }


    // 获取服务唯一标识
    public String getServiceKey() {
        return interfaceName + ":" + version + ":" + group;
    }
}
