package com.rpc.registry.zookeeper;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.registry.ServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry {
    /** Zookeeper根路径 */
    private static final String ZK_ROOT_PATH = "/rpc";
    /** 服务路径前缀 */
    private static final String SERVICE_PATH_PREFIX = ZK_ROOT_PATH + "/services";
    /** Curator客户端 */
    private final CuratorFramework client;
    /** 路径缓存映射 */
    private final Map<String, PathChildrenCache> pathCacheMap = new ConcurrentHashMap<>();
    /** 服务监听器映射 */
    private final Map<String, List<ServiceRegistry.ServiceChangeListener>> listenerMap = new ConcurrentHashMap<>();
    /** 已注册的服务信息 */
    private final Set<String> registeredServices = ConcurrentHashMap.newKeySet();

    /**
     * 构造函数
     *
     * @param connectString Zookeeper连接字符串
     */
    public ZookeeperServiceRegistry(String connectString) {
        this(connectString, 5000, 3);
    }

    /**
     * 构造函数
     *
     * @param connectString Zookeeper连接字符串
     * @param sessionTimeout 会话超时时间
     * @param retryTimes 重试次数
     */
    public ZookeeperServiceRegistry(String connectString, int sessionTimeout, int retryTimes) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, retryTimes);
        // 创建Curator客户端
        this.client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .build();
        // 启动客户端
        this.client.start();
        try {
            boolean connected = client.blockUntilConnected(30, TimeUnit.SECONDS);
            if(!connected) {
                throw new RuntimeException("连接Zookeeper超时");
            }
            // 创建根路径
            createRootPath();
            log.info("Zookeeper服务注册中心初始化成功，连接字符串：{}", connectString);
        } catch (Exception e) {
            log.error("初始化Zookeeper服务注册中心失败");
            throw new RuntimeException("初始化Zookeeper服务注册中心失败", e);
        }
    }

    /**
     * 创建根路径
     */
    private void createRootPath() throws Exception {
        if(client.checkExists().forPath(ZK_ROOT_PATH) == null) {
            client.create().creatingParentsIfNeeded().forPath(ZK_ROOT_PATH);
        }
        if(client.checkExists().forPath(SERVICE_PATH_PREFIX) == null) {
            client.create().creatingParentsIfNeeded().forPath(SERVICE_PATH_PREFIX);
        }
    }

    /**
     * 构建服务路径
     */
    private String buildServicePath(ServiceInfo serviceInfo) {
        return buildServiceTypePath(serviceInfo.getServiceName(), serviceInfo.getVersion(),
                serviceInfo.getGroup()) + "/instance-";
    }

    /**
     * 构建服务类型路径
     */
    private String buildServiceTypePath(String serviceName, String version, String group) {
        StringBuilder pathBuilder = new StringBuilder(SERVICE_PATH_PREFIX)
                .append("/").append(serviceName);
        if(version != null && !version.isEmpty()) {
            pathBuilder.append("/").append(version);
        }
        if(group != null && !group.isEmpty()) {
            pathBuilder.append("/").append(group);
        }
        return pathBuilder.toString();
    }

    /**
     * 构建服务数据
     */
    private String buildServiceData(ServiceInfo serviceInfo) {
        return String.format("%s:%d|%s|%s|%d|%s|%d",
                serviceInfo.getAddress(),
                serviceInfo.getPort(),
                serviceInfo.getVersion() != null?serviceInfo.getVersion():"",
                serviceInfo.getGroup() != null?serviceInfo.getGroup():"",
                serviceInfo.getWeight(),
                serviceInfo.getStatus().getDescription(),
                System.currentTimeMillis());
    }

    /**
     * 解析服务数据
     */
    private ServiceInfo parseServiceData(String data) {
        try {
            String[] parts = data.split("\\|");
            if(parts.length >= 6) {
                String[] addressParts = parts[0].split(":");
                if(addressParts.length == 2) {
                    ServiceInfo serviceInfo = new ServiceInfo();
                    serviceInfo.setAddress(addressParts[0]);
                    serviceInfo.setPort(Integer.parseInt(addressParts[1]));
                    serviceInfo.setVersion(parts[1].isEmpty()?null:parts[1]);
                    serviceInfo.setGroup(parts[2].isEmpty()?null:parts[2]);
                    serviceInfo.setWeight(Integer.parseInt(parts[3]));
                    serviceInfo.setStatus(parseServiceStatus(parts[4]));
                    serviceInfo.setLastUpdateTime(Long.parseLong(parts[5]));
                    return serviceInfo;
                }
            }
        } catch (Exception e) {
            log.warn("解析服务器数据失败：{}", data, e);
        }
        return null;
    }

    /**
     * 解析服务状态
     */
    private ServiceInfo.ServiceStatus parseServiceStatus(String statusDescription) {
        if (statusDescription == null || statusDescription.trim().isEmpty()) {
            return ServiceInfo.ServiceStatus.ACTIVE;
        }
        
        // 根据描述匹配对应的枚举值
        for (ServiceInfo.ServiceStatus status : ServiceInfo.ServiceStatus.values()) {
            if (status.getDescription().equals(statusDescription)) {
                return status;
            }
        }
        
        // 如果没有匹配到，尝试直接解析枚举名称
        try {
            return ServiceInfo.ServiceStatus.valueOf(statusDescription.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知的服务状态描述：{}，使用默认状态ACTIVE", statusDescription);
            return ServiceInfo.ServiceStatus.ACTIVE;
        }
    }

    @Override
    public void register(ServiceInfo serviceInfo) throws Exception {
        String servicePath = buildServicePath(serviceInfo);
        String serviceData = buildServiceData(serviceInfo);
        try {
            // 创建服务路径（如果不存在）
            String serviceTypePath = buildServiceTypePath(serviceInfo.getServiceName(), serviceInfo.getVersion(),
                    serviceInfo.getGroup());
            if(client.checkExists().forPath(serviceTypePath) == null) {
                client.create().creatingParentsIfNeeded().forPath(serviceTypePath);
            }
            // 注册服务实例（临时顺序节点）
            String actualPath = client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(servicePath,
                    serviceData.getBytes());
            registeredServices.add(actualPath);
            log.info("服务注册成功：{} -> {}", actualPath, serviceData);
        } catch (Exception e) {
            log.error("注册服务失败：{}", serviceInfo, e);
            throw e;
        }
    }

    @Override
    public void unregister(ServiceInfo serviceInfo) throws Exception {
        String serviceTypePath = buildServiceTypePath(serviceInfo.getServiceName(), serviceInfo.getVersion(),
                serviceInfo.getGroup());
        try {
            // 查找并删除对应的服务节点
            List<String> children = client.getChildren().forPath(serviceTypePath);
            for(String child: children) {
                String childPath = serviceTypePath + "/" + child;
                byte[] data = client.getData().forPath(childPath);
                if(data != null) {
                    String serviceData = new String(data);
                    if(serviceData.contains(serviceInfo.getAddress() + ":" + serviceInfo.getPort())) {
                        client.delete().forPath(childPath);
                        registeredServices.remove(childPath);
                        log.info("服务注销成功：{}", childPath);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("注销服务失败：{}", serviceInfo, e);
            throw e;
        }
    }

    @Override
    public List<ServiceInfo> discover(String serviceName) throws Exception {
        return discover(serviceName, null, null);
    }

    @Override
    public List<ServiceInfo> discover(String serviceName, String version, String group) throws Exception {
        String serviceTypePath = buildServiceTypePath(serviceName, version, group);
        try {
            if(client.checkExists().forPath(serviceTypePath) == null) {
                return new ArrayList<>();
            }
            List<String> children = client.getChildren().forPath(serviceTypePath);
            ArrayList<ServiceInfo> serviceInfos = new ArrayList<>();
            for(String child: children) {
                String childPath = serviceTypePath + "/" + child;
                byte[] data = client.getData().forPath(childPath);
                if(data != null) {
                    ServiceInfo serviceInfo = parseServiceData(new String(data));
                    if(serviceInfo != null) {
                        serviceInfos.add(serviceInfo);
                    }
                }
            }
            log.debug("发现服务：{} -> {} 个实例", serviceName, serviceInfos.size());
            return serviceInfos;
        } catch (Exception e) {
            log.error("发现服务失败：{}", serviceName, e);
            throw e;
        }
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) throws Exception {
        String serviceTypePath = buildServiceTypePath(serviceName, null, null);
        // 添加监听器到映射
        listenerMap.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(listener);
        // 如果路径缓存不存在，创建并启动
        if(!pathCacheMap.containsKey(serviceName)) {
            PathChildrenCache pathCache = new PathChildrenCache(client, serviceTypePath, true);
            pathCache.getListenable().addListener(new ServicePathChildrenCacheListener(serviceName));
            pathCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            pathCacheMap.put(serviceName, pathCache);
            log.info("开始监听服务变化：{}", serviceName);
        }
    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) throws Exception {
        List<ServiceChangeListener> listeners = listenerMap.get(serviceName);
        if(listeners != null) {
            listeners.remove(listener);
            // 如果没有监听器了，关闭路径缓存
            if(listeners.isEmpty()) {
                PathChildrenCache pathCache = pathCacheMap.remove(serviceName);
                if(pathCache != null) {
                    pathCache.close();
                    log.info("停止监听服务变化：{}", serviceName);
                }
                listenerMap.remove(serviceName);
            }
        }
    }

    @Override
    public List<String> getAllServiceNames() throws Exception {
        try {
            if(client.checkExists().forPath(SERVICE_PATH_PREFIX) == null) {
                return new ArrayList<>();
            }
            return client.getChildren().forPath(SERVICE_PATH_PREFIX);
        } catch (Exception e) {
            log.error("获取所有服务名称失败", e);
            throw e;
        }
    }

    /**
     * 获取所有服务实例（递归遍历所有版本和分组）
     */
    public List<ServiceInfo> getAllServiceInstances() throws Exception {
        List<ServiceInfo> allInstances = new ArrayList<>();
        try {
            if(client.checkExists().forPath(SERVICE_PATH_PREFIX) == null) {
                return allInstances;
            }
            
            // 获取所有服务名称
            List<String> serviceNames = client.getChildren().forPath(SERVICE_PATH_PREFIX);
            log.debug("发现服务名称: {}", serviceNames);
            
            for (String serviceName : serviceNames) {
                // 递归遍历每个服务的所有版本和分组
                List<ServiceInfo> serviceInstances = discoverAllVersionsAndGroups(serviceName);
                allInstances.addAll(serviceInstances);
            }
            
            log.info("总共发现 {} 个服务实例", allInstances.size());
        } catch (Exception e) {
            log.error("获取所有服务实例失败", e);
            throw e;
        }
        return allInstances;
    }
    
    /**
     * 发现指定服务名称下的所有版本和分组的实例
     */
    private List<ServiceInfo> discoverAllVersionsAndGroups(String serviceName) throws Exception {
        List<ServiceInfo> instances = new ArrayList<>();
        String serviceBasePath = SERVICE_PATH_PREFIX + "/" + serviceName;
        
        if (client.checkExists().forPath(serviceBasePath) == null) {
            return instances;
        }
        
        // 递归遍历版本目录
        traverseServicePath(serviceBasePath, serviceName, instances);
        
        return instances;
    }
    
    /**
     * 递归遍历服务路径，发现所有实例
     */
    private void traverseServicePath(String currentPath, String serviceName, List<ServiceInfo> instances) throws Exception {
        List<String> children = client.getChildren().forPath(currentPath);
        
        for (String child : children) {
            String childPath = currentPath + "/" + child;
            
            if (child.startsWith("instance-")) {
                // 这是一个实例节点，解析服务数据
                try {
                    byte[] data = client.getData().forPath(childPath);
                    if (data != null) {
                        ServiceInfo serviceInfo = parseServiceData(new String(data));
                        if (serviceInfo != null) {
                            serviceInfo.setServiceName(serviceName);
                            instances.add(serviceInfo);
                            log.debug("发现服务实例: {} -> {}:{}", serviceName, serviceInfo.getAddress(), serviceInfo.getPort());
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析服务实例数据失败: {}", childPath, e);
                }
            } else {
                // 这是版本或分组目录，继续递归
                traverseServicePath(childPath, serviceName, instances);
            }
        }
    }

    @Override
    public boolean exists(String serviceName) throws Exception {
        String serviceTypePath = buildServiceTypePath(serviceName, null, null);
        return client.checkExists().forPath(serviceTypePath) != null;
    }

    @Override
    public int getServiceInstanceCount(String serviceName) throws Exception {
        List<ServiceInfo> serviceInfos = discover(serviceName);
        return serviceInfos.size();
    }

    @Override
    public void destroy() throws Exception {
        try {
            // 关闭所有路径缓存
            for(PathChildrenCache pathCache: pathCacheMap.values()) {
                pathCache.close();
            }
            pathCacheMap.clear();
            // 清理监听器
            listenerMap.clear();
            // 删除已注册的服务
            for(String servicePath: registeredServices) {
                try {
                    if(client.checkExists().forPath(servicePath) != null) {
                        client.delete().forPath(servicePath);
                    }
                } catch (Exception e) {
                    log.warn("删除注册服务失败：{}", servicePath, e);
                }
            }
            registeredServices.clear();
            // 关闭Curator客户端
            client.close();
            log.info("Zookeeper服务注册中心已销毁");
        } catch (Exception e) {
            log.error("销毁Zookeeper服务注册中心·失败", e);
            throw e;
        }
    }

    @Override
    public boolean isAvailable() throws Exception {
        return client.getState() == CuratorFrameworkState.STARTED;
    }

    /**
     * 服务路径子节点缓存监听器
     */
    private class ServicePathChildrenCacheListener implements PathChildrenCacheListener {
        private final String serviceName;

        public ServicePathChildrenCacheListener(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
            List<ServiceRegistry.ServiceChangeListener> listeners = listenerMap.get(serviceName);
            if(listeners == null || listeners.isEmpty()) {
                return;
            }
            switch(event.getType()) {
                case CHILD_ADDED:
                    handleChildAdded(event, listeners);
                    break;
                case CHILD_REMOVED:
                    handleChildRemoved(event, listeners);
                    break;
                case CHILD_UPDATED:
                    handleChildUpdated(event, listeners);
                    break;
                default:
                    break;
            }
            // 通知服务列表变化
            try {
                List<ServiceInfo> currentServices = discover(serviceName);
                for(ServiceChangeListener listener: listeners) {
                    listener.onServiceListChanged(serviceName, currentServices);
                }
            } catch (Exception e) {
                log.error("通知服务列表变化失败", e);
            }
        }

        private void handleChildAdded(PathChildrenCacheEvent event, List<ServiceChangeListener> listeners) {
            if(event.getData() != null && event.getData().getData() != null) {
                ServiceInfo serviceInfo = parseServiceData(new String(event.getData().getData()));
                if(serviceInfo != null) {
                    for(ServiceChangeListener listener: listeners) {
                        listener.onServiceAdded(serviceName, serviceInfo);
                    }
                }
            }
        }

        private void handleChildRemoved(PathChildrenCacheEvent event, List<ServiceChangeListener> listeners) {
            if(event.getData() != null && event.getData().getData() != null) {
                ServiceInfo serviceInfo = parseServiceData(new String(event.getData().getData()));
                if(serviceInfo != null) {
                    for(ServiceChangeListener listener: listeners) {
                        listener.onServiceRemoved(serviceName, serviceInfo);
                    }
                }
            }
        }

        private void handleChildUpdated(PathChildrenCacheEvent event, List<ServiceChangeListener> listeners) {
            // Zookeeper中临时节点通常不会更新，这里主要是为了完整性
            if (event.getData() != null && event.getData().getData() != null) {
                ServiceInfo newServiceInfo = parseServiceData(new String(event.getData().getData()));
                if (newServiceInfo != null) {
                    for (ServiceChangeListener listener : listeners) {
                        listener.onServiceUpdated(serviceName, null, newServiceInfo);
                    }
                }
            }
        }
    }

}
