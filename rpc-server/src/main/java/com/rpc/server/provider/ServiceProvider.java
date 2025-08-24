package com.rpc.server.provider;

import com.rpc.core.serviceinfo.ServiceInfo;

import java.util.List;
import java.util.Map;

/**
 * @author 何杰
 * @version 1.0
 */
public interface ServiceProvider {
    /**
     * 注册服务实例
     *
     * @param serviceInfo 服务信息
     * @param serviceInstance 服务实例对象
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果注册失败
     */
    void registerService(ServiceInfo serviceInfo, Object serviceInstance);
    /**
     * 注册服务实例（使用默认配置）
     *
     * @param interfaceClass 服务接口类
     * @param serviceInstance 服务实例对象
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果注册失败
     */
    void registerService(Class<?> interfaceClass, Object serviceInstance);
    /**
     * 注册服务实例（指定版本和分组）
     *
     * @param interfaceClass 服务接口类
     * @param serviceInstance 服务实例对象
     * @param version 服务版本
     * @param group 服务分组
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException 如果注册失败
     */
    void registerService(Class<?> interfaceClass, Object serviceInstance, String version, String group);
    /**
     * 获取服务实例
     *
     * @param serviceName 服务名称（接口全限定名）
     * @return 服务实例对象，如果不存在则返回null
     */
    Object getService(String serviceName);
    /**
     * 获取服务实例（指定版本和分组）
     *
     * @param serviceName 服务名称（接口全限定名）
     * @param version 服务版本
     * @param group 服务分组
     * @return 服务实例对象，如果不存在则返回null
     */
    Object getService(String serviceName, String version, String group);
    /**
     * 获取服务实例（使用服务唯一标识）
     *
     * @param serviceKey 服务唯一标识
     * @return 服务实例对象，如果不存在则返回null
     */
    Object getServiceByKey(String serviceKey);
    /**
     * 注销服务实例
     *
     * @param serviceName 服务名称（接口全限定名）
     * @return 是否注销成功
     */
    boolean unregisterService(String serviceName);
    /**
     * 注销服务实例（指定版本和分组）
     *
     * @param serviceName 服务名称（接口全限定名）
     * @param version 服务版本
     * @param group 服务分组
     * @return 是否注销成功
     */
    boolean unregisterService(String serviceName, String version, String group);
    /**
     * 注销服务实例（使用服务唯一标识）
     *
     * @param serviceKey 服务唯一标识
     * @return 是否注销成功
     */
    boolean unregisterServiceByKey(String serviceKey);
    /**
     * 检查服务是否存在
     *
     * @param serviceName 服务名称（接口全限定名）
     * @return 是否存在
     */
    boolean containsService(String serviceName);
    /**
     * 检查服务是否存在（指定版本和分组）
     *
     * @param serviceName 服务名称（接口全限定名）
     * @param version 服务版本
     * @param group 服务分组
     * @return 是否存在
     */
    boolean containsService(String serviceName, String version, String group);
    /**
     * 检查服务是否存在（使用服务唯一标识）
     *
     * @param serviceKey 服务唯一标识
     * @return 是否存在
     */
    boolean containsServiceByKey(String serviceKey);
    /**
     * 获取所有已注册的服务信息
     *
     * @return 服务信息列表
     */
    List<ServiceInfo> getAllServices();
    /**
     * 获取所有已注册的服务实例映射
     *
     * @return 服务键到服务实例的映射
     */
    Map<String, Object> getAllServiceInstances();
    /**
     * 获取已注册服务的数量
     *
     * @return 服务数量
     */
    int getServiceCount();
    /**
     * 清空所有已注册的服务
     */
    void clear();
    /**
     * 销毁服务提供者，释放资源
     */
    void destroy();
    /**
     * 检查服务提供者是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
