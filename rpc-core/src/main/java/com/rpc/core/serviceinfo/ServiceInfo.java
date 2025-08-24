package com.rpc.core.serviceinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 何杰
 * @version 1.0
 */
@Data
public class ServiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    public enum ServiceStatus {
        ACTIVE("活跃"), // 活跃
        INACTIVE("非活跃"), // 非活跃
        DISABLE("禁用");
        private final String description;

        ServiceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
        // 警用
    }
    private String serviceName; // 服务名称
    private String address; // 服务地址
    private int port; // 服务端口
    private String version = "1.0.0"; // 服务版本
    private String group = "default"; // 服务分组
    private int weight = 1; // 权重
    private ServiceStatus status = ServiceStatus.ACTIVE; // 服务状态
    private long registerTime; // 注册时间
    private long lastUpdateTime; // 最后更新时间

    /**
     * 默认构造函数
     */
    public ServiceInfo() {
        this.registerTime = System.currentTimeMillis();
        this.lastUpdateTime = this.registerTime;
    }

    public ServiceInfo(String serviceName, String address, int port) {
        this();
        this.serviceName = serviceName;
        this.address = address;
        this.port = port;
    }

    public ServiceInfo(String serviceName, String address, int port, String version, String group) {
        this(serviceName, address, port);
        this.version = version;
        this.group = group;
    }

    // 获取完整服务地址
    public String getFullAddress() {
        return address + ":" + port;
    }

    public String getServiceKey() {
        return serviceName + ":" + version + ":" + group;
    }

    // 构建服务唯一标识
    public static String buildServiceKey(String serviceName, String version, String group) {
        return serviceName + ":" + version + ":" + group;
    }

    public String getServiceInstanceKey() {
        return getServiceKey() + "@" + getFullAddress();
    }

    /**
     * 更新最后更新时间
     */
    public void updateLastUpdateTime() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 判断服务是否活跃
     *
     * @return true表示活跃，false表示非活跃
     */
    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }

    /**
     * 判断服务是否可用
     *
     * @return true表示可用，false表示不可用
     */
    public boolean isAvailable() {
        return status == ServiceStatus.ACTIVE;
    }
}
