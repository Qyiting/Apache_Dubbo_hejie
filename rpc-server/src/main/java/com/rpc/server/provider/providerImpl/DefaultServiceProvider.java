package com.rpc.server.provider.providerImpl;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.server.provider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class DefaultServiceProvider implements ServiceProvider {
    /** 服务实例映射：服务键 -> 服务实例 */
    private final Map<String, Object> serviceInstances = new ConcurrentHashMap<>();
    /** 服务信息映射：服务键 -> 服务信息 */
    private final Map<String, ServiceInfo> serviceInfos = new ConcurrentHashMap<>();
    /** 服务提供者是否可用 */
    private final AtomicBoolean available = new AtomicBoolean(true);
    /** 默认服务版本 */
    private static final String DEFAULT_VERSION = "1.0.0.";
    /** 默认服务分组 */
    private static final String DEFAULT_GROUP = "default";

    @Override
    public void registerService(ServiceInfo serviceInfo, Object serviceInstance) {
        if(serviceInfo == null) {
            throw new IllegalArgumentException("服务信息不能为null");
        }
        if(serviceInstance == null) {
            throw new IllegalArgumentException("服务实例不能为null");
        }
        if(!available.get()) {
            throw new RuntimeException("服务提供者已销毁，无法注册服务");
        }
        String serviceKey = serviceInfo.getServiceKey();
        // 检查服务是否已注册
        if(serviceInstances.containsKey(serviceKey)) {
            log.warn("服务已存在，将覆盖已有服务：{}", serviceKey);
        }
        // 注册服务
        serviceInstances.put(serviceKey, serviceInstance);
        serviceInfos.put(serviceKey, serviceInfo);
        log.info("成功注册服务：{} -> {}", serviceKey, serviceInstance.getClass().getName());
    }

    @Override
    public void registerService(Class<?> interfaceClass, Object serviceInstance) {
        registerService(interfaceClass, serviceInstance, DEFAULT_VERSION, DEFAULT_GROUP);
    }

    @Override
    public void registerService(Class<?> interfaceClass, Object serviceInstance, String version, String group) {
        if(interfaceClass == null) {
            throw new IllegalArgumentException("接口类不能为null");
        }
        if(serviceInstance == null) {
            throw new IllegalArgumentException("服务实例不能为null");
        }
        if(!interfaceClass.isAssignableFrom(serviceInstance.getClass())) {
            throw new IllegalArgumentException("serviceInstance 必须是 " + interfaceClass.getName() + " 的实现或子类");
        }
        if(!interfaceClass.isAssignableFrom(serviceInstance.getClass())) {
            throw new IllegalArgumentException("服务实例必须实现指定的接口：" + interfaceClass.getName());
        }
        // 创建服务信息
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceName(interfaceClass.getName());
        serviceInfo.setVersion(version != null?version:DEFAULT_VERSION);
        serviceInfo.setGroup(group!= null?group:DEFAULT_GROUP);
        serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
        serviceInfo.setRegisterTime(System.currentTimeMillis());
        serviceInfo.setLastUpdateTime(System.currentTimeMillis());
        registerService(serviceInfo, serviceInstance);
    }

    @Override
    public Object getService(String serviceName) {
        return getService(serviceName, DEFAULT_VERSION, DEFAULT_GROUP);
    }

    @Override
    public Object getService(String serviceName, String version, String group) {
        if(serviceName == null || serviceName.trim().isEmpty()) {
            return null;
        }
        String serviceKey = ServiceInfo.buildServiceKey(serviceName, version != null ? version : DEFAULT_VERSION,
                group != null ? group : DEFAULT_GROUP);
        return getServiceByKey(serviceKey);
    }

    @Override
    public Object getServiceByKey(String serviceKey) {
        if(serviceKey == null || serviceKey.trim().isEmpty()) {
            return null;
        }
        if(!available.get()) {
            log.warn("服务提供者已销毁，无法获取服务：{}", serviceKey);
            return null;
        }
        Object serviceInstance = serviceInstances.get(serviceKey);
        if(serviceInstance != null) {
            log.debug("找到服务实例：{} -> {}", serviceKey, serviceInstance.getClass().getName());
        } else {
            log.debug("未找到服务实例：{}", serviceKey);
        }
        return serviceInstance;
    }

    @Override
    public boolean unregisterService(String serviceName) {
        return unregisterService(serviceName, DEFAULT_VERSION, DEFAULT_GROUP);
    }

    @Override
    public boolean unregisterService(String serviceName, String version, String group) {
        if(serviceName == null || serviceName.trim().isEmpty()) {
            return false;
        }
        String serviceKey = ServiceInfo.buildServiceKey(serviceName, version != null ? version : DEFAULT_VERSION,
                group != null ? group : DEFAULT_GROUP);
        return unregisterServiceByKey(serviceKey);
    }

    @Override
    public boolean unregisterServiceByKey(String serviceKey) {
        if(serviceKey == null || serviceKey.trim().isEmpty()) {
            return false;
        }
        Object removedInstance = serviceInstances.remove(serviceKey);
        ServiceInfo removedInfo = serviceInfos.remove(serviceKey);
        boolean success = removedInstance != null && removedInfo != null;
        if(success) {
            log.info("成功注销服务：{} -> {}", serviceKey, removedInstance.getClass().getName());
        } else {
            log.debug("服务不存在，无法注销：{}", serviceKey);
        }
        return success;
    }

    @Override
    public boolean containsService(String serviceName) {
        return containsService(serviceName, DEFAULT_VERSION, DEFAULT_GROUP);
    }

    @Override
    public boolean containsService(String serviceName, String version, String group) {
        if(serviceName == null || serviceName.trim().isEmpty()) {
            return false;
        }
        String serviceKey = ServiceInfo.buildServiceKey(serviceName, version != null ? version : DEFAULT_VERSION,
                group != null ? group : DEFAULT_GROUP);
        return containsServiceByKey(serviceKey);
    }

    @Override
    public boolean containsServiceByKey(String serviceKey) {
        if(serviceKey == null || serviceKey.trim().isEmpty()) {
            return false;
        }
        return serviceInstances.containsKey(serviceKey);
    }

    @Override
    public List<ServiceInfo> getAllServices() {
        return new ArrayList<>(serviceInfos.values());
    }

    @Override
    public Map<String, Object> getAllServiceInstances() {
        return new HashMap<>(serviceInstances);
    }

    @Override
    public int getServiceCount() {
        return serviceInstances.size();
    }

    @Override
    public void clear() {
        int count = serviceInstances.size();
        serviceInfos.clear();
        serviceInstances.clear();
        log.info("已清空所有服务，共清空 {} 个服务", count);
    }

    @Override
    public void destroy() {
        if(available.compareAndSet(true, false)) {
            int count = serviceInstances.size();
            clear();
            log.info("服务提供者已销毁，共清理 {} 个服务", count);
        }
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    /**
     * 获取服务统计信息
     *
     * @return 服务统计信息字符串
     */
    public String getStatistics() {
        if (!available.get()) {
            return "服务提供者已销毁";
        }

        Map<String, Integer> groupCount = new HashMap<>();
        Map<String, Integer> versionCount = new HashMap<>();

        for (ServiceInfo serviceInfo : serviceInfos.values()) {
            // 统计分组
            String group = serviceInfo.getGroup();
            groupCount.put(group, groupCount.getOrDefault(group, 0) + 1);

            // 统计版本
            String version = serviceInfo.getVersion();
            versionCount.put(version, versionCount.getOrDefault(version, 0) + 1);
        }

        return String.format("服务总数: %d, 分组统计: %s, 版本统计: %s",
                getServiceCount(), groupCount, versionCount);
    }

    @Override
    public String toString() {
        return "DefaultServiceProvider{" +
                "serviceCount=" + getServiceCount() +
                ", available=" + available.get() +
                '}';
    }
}
