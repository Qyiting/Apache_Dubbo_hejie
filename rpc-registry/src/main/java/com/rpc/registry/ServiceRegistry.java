package com.rpc.registry;

import com.rpc.core.serviceinfo.ServiceInfo;

import java.util.List;

/**
 * @author 何杰
 * @version 1.0
 */ public interface ServiceRegistry {

    /**
     * 注册服务
     *
     * @param serviceInfo 服务信息
     * @throws Exception 注册异常
     */
     void register(ServiceInfo serviceInfo) throws Exception;

    /**
     * 注销服务
     *
     * @param serviceInfo 服务信息
     * @throws Exception 注销异常
     */
     void unregister(ServiceInfo serviceInfo) throws Exception;

    /**
     * 发现服务
     *
     * @param serviceName 服务名称
     * @return 服务实例列表
     * @throws Exception 发现异常
     */
     List<ServiceInfo> discover(String serviceName) throws Exception;

    /**
     * 发现服务（带版本和分组）
     *
     * @param serviceName 服务名称
     * @param version 服务版本
     * @param group 服务分组
     * @return 服务实例列表
     * @throws Exception 发现异常
     */
     List<ServiceInfo> discover(String serviceName, String version, String group) throws Exception;

    /**
     * 订阅服务变化
     *
     * @param serviceName 服务名称
     * @param listener 变化监听器
     * @throws Exception 订阅异常
     */
     void subscribe(String serviceName, ServiceChangeListener listener) throws Exception;

    /**
     * 取消订阅服务变化
     *
     * @param serviceName 服务名称
     * @param listener 变化监听器
     * @throws Exception 取消订阅异常
     */
     void unsubscribe(String serviceName, ServiceChangeListener listener) throws Exception;

    /**
     * 获取所有服务名称
     *
     * @return 服务名称列表
     * @throws Exception 获取异常
     */
     List<String> getAllServiceNames() throws Exception;

    /**
     * 检查服务是否存在
     *
     * @param serviceName 服务名称
     * @return 是否存在
     * @throws Exception 检查异常
     */
     boolean exists(String serviceName) throws Exception;

    /**
     * 获取服务实例数量
     *
     * @param serviceName 服务名称
     * @return 实例数量
     * @throws Exception 获取异常
     */
    int getServiceInstanceCount(String serviceName) throws Exception;

    /**
     * 销毁注册中心，释放资源
     *
     * @throws Exception 销毁异常
     */
    void destroy() throws Exception;

    /**
     * 检查注册中心是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable() throws Exception;

    /**
     * 服务变化监听器接口
     */
     interface ServiceChangeListener {

         /**
          * 服务实例添加
          *
          * @param serviceName 服务名称
          * @param serviceInfo 服务信息
          */
         void onServiceAdded(String serviceName, ServiceInfo serviceInfo);

         /**
          * 服务实例移除
          *
          * @param serviceName 服务名称
          * @param serviceInfo 服务信息
          */
         void onServiceRemoved(String serviceName, ServiceInfo serviceInfo);

         /**
          * 服务实例更新
          *
          * @param serviceName 服务名称
          * @param oldServiceInfo 旧服务信息
          * @param newServiceInfo 新服务信息
          */
         void onServiceUpdated(String serviceName, ServiceInfo oldServiceInfo, ServiceInfo newServiceInfo);

         /**
          * 服务列表变化
          *
          * @param serviceName 服务名称
          * @param serviceInfos 当前服务实例列表
          */
         void onServiceListChanged(String serviceName, List<ServiceInfo> serviceInfos);
     }
    abstract class AbstractServiceChangeListener implements ServiceChangeListener {
        @Override
        public void onServiceAdded(String serviceName, ServiceInfo serviceInfo) {

        }

        @Override
        public void onServiceRemoved(String serviceName, ServiceInfo serviceInfo) {

        }

        @Override
        public void onServiceUpdated(String serviceName, ServiceInfo oldServiceInfo, ServiceInfo newServiceInfo) {

        }

        @Override
        public void onServiceListChanged(String serviceName, List<ServiceInfo> serviceInfos) {

        }
    }
}
