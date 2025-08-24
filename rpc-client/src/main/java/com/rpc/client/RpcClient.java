package com.rpc.client;

import com.rpc.client.health.ConnectionHealthChecker;
import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.client.loadbalance.LoadBalancerFactory;
import com.rpc.client.loadbalance.random.RandomLoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.core.metric.MetricsCollector;
import com.rpc.core.retry.RetryStrategy;
import com.rpc.netty.client.NettyRpcClient;
import com.rpc.registry.ServiceRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
@Data
public class RpcClient implements ServiceRegistry.ServiceChangeListener {
    /** 服务注册中心 */
    private final ServiceRegistry serviceRegistry;
    /** 负载均衡器 */
    private LoadBalancer loadBalancer;
    /** Netty客户端连接池 */
    private final Map<String, NettyRpcClient> clientPool = new ConcurrentHashMap<>();
    /** 默认请求超时时间（毫秒） */
    private final long defaultTimeout;
    /** 最大连接数 */
    private final int maxConnections;
    /** 是否已关闭 */
    private volatile boolean closed = false;
    /** 服务缓存：服务名 -> 服务实例列表 */
    private final Map<String, List<ServiceInfo>> serviceCache = new ConcurrentHashMap<>();
    /** 已订阅的服务名集合 */
    private final Set<String> subscribedServices = ConcurrentHashMap.newKeySet();
    /** 连接健康检查器 */
    private final ConnectionHealthChecker healthChecker;
    /** 不健康连接的重试计数 */
    private final Map<String, Integer> unhealthyRetryCount = new ConcurrentHashMap<>();
    /** 监控指标收集器 */
    private final MetricsCollector metricsCollector = MetricsCollector.getInstance();
    /**
     * 构造函数
     * @param serviceRegistry 服务注册中心
     */
    public RpcClient(ServiceRegistry serviceRegistry) {
        this(serviceRegistry, new RandomLoadBalancer(), 5000, 10);
    }
    
    /**
     * 构造函数（支持通过算法名称创建负载均衡器）
     * @param serviceRegistry 服务注册中心
     * @param loadBalancerAlgorithm 负载均衡算法名称
     */
    public RpcClient(ServiceRegistry serviceRegistry, String loadBalancerAlgorithm) {
        this(serviceRegistry, loadBalancerAlgorithm, 5000, 10);
    }
    
    /**
     * 构造函数（支持通过算法名称创建负载均衡器）
     * @param serviceRegistry 服务注册中心
     * @param loadBalancerAlgorithm 负载均衡算法名称
     * @param defaultTimeout 默认超时时间
     * @param maxConnections 最大连接数
     */
    public RpcClient(ServiceRegistry serviceRegistry, String loadBalancerAlgorithm, long defaultTimeout, int maxConnections) {
        LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(loadBalancerAlgorithm);
        if (loadBalancer == null) {
            log.warn("未找到负载均衡算法：{}，使用默认的随机负载均衡", loadBalancerAlgorithm);
            loadBalancer = new RandomLoadBalancer();
        }
        
        this.serviceRegistry = serviceRegistry;
        this.loadBalancer = loadBalancer;
        this.defaultTimeout = defaultTimeout;
        this.maxConnections = maxConnections;
        // 初始化连接健康检查器（检查间隔30秒，连接超时60秒，最大重试3次）
        this.healthChecker = new ConnectionHealthChecker(30000, 60000, 3);
        // 启动健康检查
        this.healthChecker.start(clientPool, new ConnectionHealthCallback());
        log.info("RPC客户端初始化完成，负载均衡算法：{}，默认超时：{}ms，最大连接数：{}", 
                loadBalancer.getAlgorithm(), defaultTimeout, maxConnections);
    }
    /**
     * 构造函数
     *
     * @param serviceRegistry 服务注册中心
     * @param loadBalancer 负载均衡器
     * @param defaultTimeout 默认超时时间
     * @param maxConnections 最大连接数
     */
    public RpcClient(ServiceRegistry serviceRegistry, LoadBalancer loadBalancer, long defaultTimeout, int maxConnections) {
        this.serviceRegistry = serviceRegistry;
        this.loadBalancer = loadBalancer;
        this.defaultTimeout = defaultTimeout;
        this.maxConnections = maxConnections;
        // 初始化连接健康检查器（检查间隔30秒，连接超时60秒，最大重试3次）
        this.healthChecker = new ConnectionHealthChecker(30000, 60000, 3);
        // 启动健康检查
        this.healthChecker.start(clientPool, new ConnectionHealthCallback());
        log.info("RPC客户端初始化完成，默认超时：{}ms，最大连接数：{}", defaultTimeout, maxConnections);
    }
    /**
     * 发送RPC请求（同步）
     *
     * @param request RPC请求
     * @return RPC响应
     * @throws Exception 请求异常
     */
    public RpcResponse sendRequest(RpcRequest request) throws Exception {
        return sendRequest(request, defaultTimeout);
    }
    /**
     * 发送RPC请求（同步）
     *
     * @param request RPC请求
     * @param timeout 超时时间（毫秒）
     * @return RPC响应
     * @throws Exception 请求异常
     */
    public RpcResponse sendRequest(RpcRequest request, long timeout) throws Exception {
        checkClosed();
        
        String serviceName = request.getInterfaceName();
        long startTime = System.currentTimeMillis();
        
        // 记录请求开始
        metricsCollector.recordRequestStart(serviceName);
        
        try {
            // 使用重试策略执行请求
            RpcResponse response = RetryStrategy.executeWithRetry(() -> {
                try {
                    return doSendRequest(request, timeout);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, serviceName);
            
            // 记录请求成功
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordRequestSuccess(serviceName, responseTime);
            
            return response;
        } catch (Exception e) {
            log.error("发送RPC请求失败：{}", request, e);
            // 记录请求失败
            metricsCollector.recordRequestFailure(serviceName, e);
            throw e;
        }
    }
    
    /**
     * 执行单次RPC请求
     *
     * @param request RPC请求
     * @param timeout 超时时间（毫秒）
     * @return RPC响应
     * @throws Exception 请求异常
     */
    private RpcResponse doSendRequest(RpcRequest request, long timeout) throws Exception {
        ServiceInfo selectedService = null;
        
        try {
            // 服务发现
            List<ServiceInfo> serviceInfos = discoverServices(request);
            if(serviceInfos.isEmpty()) {
                throw new RuntimeException("未发现可用的服务实例：" + request.getServiceKey());
            }
            // 负载均衡选择服务实例
            selectedService = loadBalancer.select(serviceInfos, request);
            if(selectedService == null) {
                throw new RuntimeException("负载均衡选择服务实例实例："+ request.getServiceKey());
            }
            // 获取或创建连接
            NettyRpcClient client = getOrCreateClient(selectedService, null);
            // 发送请求
            RpcResponse response = client.sendRequest(request, timeout);
            // 检查响应状态
            if(response.getStatusCode() != RpcResponse.StatusCode.SUCCESS) {
                throw new RuntimeException("RPC调用失败：" + response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            // 如果是连接异常，移除客户端
            if(isConnectionException(e) && selectedService != null) {
                removeClient(selectedService);
            }
            throw e;
        }
    }

    /**
     * 发送RPC请求（同步）方法重载
     *
     * @param request RPC请求
     * @param timeout 超时时间（毫秒）
     * @return RPC响应
     * @throws Exception 请求异常
     */
    public RpcResponse sendRequest(RpcRequest request, long timeout, Byte serializationType) throws Exception {
        checkClosed();
        
        String serviceName = request.getInterfaceName();
        long startTime = System.currentTimeMillis();
        ServiceInfo selectedService = null;
        
        // 记录请求开始
        metricsCollector.recordRequestStart(serviceName);
        
        try {
            // 服务发现
            List<ServiceInfo> serviceInfos = discoverServices(request);
            if(serviceInfos.isEmpty()) {
                throw new RuntimeException("未发现可用的服务实例：" + request.getServiceKey());
            }
            // 负载均衡选择服务实例
            selectedService = loadBalancer.select(serviceInfos, request);
            if(selectedService == null) {
                throw new RuntimeException("负载均选择服务实例实例："+ request.getServiceKey());
            }
            // 获取或创建连接
            NettyRpcClient client = getOrCreateClient(selectedService, serializationType);
            // 发送请求
            RpcResponse response = client.sendRequest(request, timeout);
            // 检查响应状态
            if(response.getStatusCode() != RpcResponse.StatusCode.SUCCESS) {
                throw new RuntimeException("RPC调用失败：" + response.getMessage());
            }
            
            // 记录请求成功
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordRequestSuccess(serviceName, responseTime);
            
            return response;
        } catch (Exception e) {
            log.error("发送RPC请求失败：{}", request, e);
            // 记录请求失败
            metricsCollector.recordRequestFailure(serviceName, e);
            // 如果是连接异常，移除客户端
            if(isConnectionException(e) && selectedService != null) {
                removeClient(selectedService);
            }
            throw e;
        }
    }

    /**
     * 发送RPC请求（异步）
     *
     * @param request RPC请求
     * @return CompletableFuture
     */
    public CompletableFuture<RpcResponse> sendRequestAsync(RpcRequest request, Byte serializationType) {
        return sendRequestAsync(request, defaultTimeout, serializationType);
    }
    /**
     * 发送RPC请求（异步）
     *
     * @param request RPC请求
     * @param timeout 超时时间（毫秒）
     * @return CompletableFuture
     */
    public CompletableFuture<RpcResponse> sendRequestAsync(RpcRequest request, long timeout, Byte serializationType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendRequest(request, timeout, serializationType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 关闭客户端
     */
    public void close() {
        if(closed) {
            return;
        }
        closed = true;
        log.info("正在关闭RPC客户端...");
        
        // 停止健康检查器
        if (healthChecker != null) {
            healthChecker.stop();
        }
        
        // 取消所有服务订阅
        for (String serviceName : subscribedServices) {
            try {
                serviceRegistry.unsubscribe(serviceName, this);
                log.info("已取消订阅服务：{}", serviceName);
            } catch (Exception e) {
                log.warn("取消订阅服务失败：{}", serviceName, e);
            }
        }
        subscribedServices.clear();
        serviceCache.clear();
        unhealthyRetryCount.clear();
        
        // 关闭所有连接
        for(NettyRpcClient client : clientPool.values()) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭Netty客户端失败", e);
            }
        }
        clientPool.clear();
        log.info("RPC客户端已关闭");
    }
    /**
     * 设置负载均衡器
     *
     * @param loadBalancer 负载均衡器
     */
    public void setLoadBalancer(LoadBalancer loadBalancer) {
        if(loadBalancer == null) {
            throw new IllegalArgumentException("负载均衡器不能为空");
        }
        this.loadBalancer = loadBalancer;
    }
    
    /**
     * 通过算法名称设置负载均衡器
     * @param algorithm 负载均衡算法名称
     */
    public void setLoadBalancer(String algorithm) {
        LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(algorithm);
        if (loadBalancer == null) {
            throw new IllegalArgumentException("未找到负载均衡算法：" + algorithm);
        }
        this.loadBalancer = loadBalancer;
        log.info("负载均衡算法已切换为：{}", loadBalancer.getAlgorithm());
    }
    
    /**
     * 获取当前负载均衡算法名称
     * @return 当前负载均衡算法名称
     */
    public String getCurrentLoadBalancerAlgorithm() {
        return loadBalancer != null ? loadBalancer.getAlgorithm() : "unknown";
    }
    
    /**
     * 获取所有支持的负载均衡算法
     * @return 支持的负载均衡算法列表
     */
    public static List<String> getSupportedLoadBalancerAlgorithms() {
        return new ArrayList<>(LoadBalancerFactory.getSupportedAlgorithms());
    }
    /**
     * 检查是否已关闭
     */
    private void checkClosed() {
        if(closed) {
            throw new IllegalArgumentException("RPC客户端已关闭");
        }
    }
    /**
     * 服务发现
     */
    private List<ServiceInfo> discoverServices(RpcRequest request) throws Exception {
        String serviceName = request.getInterfaceName();
        
        // 记录服务发现指标
        metricsCollector.recordServiceDiscovery();
        
        // 首先尝试从缓存获取
        List<ServiceInfo> cachedServices = serviceCache.get(serviceName);
        if (cachedServices != null && !cachedServices.isEmpty()) {
            return new CopyOnWriteArrayList<>(cachedServices);
        }
        
        // 缓存中没有，从注册中心发现服务
        List<ServiceInfo> services = serviceRegistry.discover(request.getInterfaceName(), request.getVersion(), request.getGroup());
        
        // 订阅服务变化（如果还未订阅）
        if (!subscribedServices.contains(serviceName)) {
            try {
                serviceRegistry.subscribe(serviceName, this);
                subscribedServices.add(serviceName);
                log.info("已订阅服务变化：{}", serviceName);
            } catch (Exception e) {
                log.warn("订阅服务变化失败：{}", serviceName, e);
            }
        }
        
        // 更新缓存
        if (services != null && !services.isEmpty()) {
            serviceCache.put(serviceName, new CopyOnWriteArrayList<>(services));
        }
        
        return services;
    }
    /**
     * 获取或创建客户端连接
     */
    private NettyRpcClient getOrCreateClient(ServiceInfo serviceInfo, Byte serializationType) throws Exception {
        String clientKey = buildClientKey(serviceInfo);
        NettyRpcClient client = clientPool.get(clientKey);
        if(client == null || !client.isActive()) {
            synchronized (this) {
                client = clientPool.get(clientKey);
                if(client == null || !client.isActive()) {
                    // 检查连接数限制
                    if(clientPool.size() >+ maxConnections) {
                        // 移除一个不活跃的连接
                        removeInactiveClient();
                    }
                    // 创建新连接
                    client = new NettyRpcClient(serviceInfo.getAddress(), serviceInfo.getPort());
                    if(serializationType != null) {
                        client.connect(serializationType.byteValue());
                    } else {
                        client.connect();
                    }
                    clientPool.put(clientKey, client);
                    // 记录连接创建
                    metricsCollector.recordConnectionCreated();
                    log.info("创建新的客户端连接：{}", clientKey);
                }
            }
        }
        return client;
    }

    /**
     * 移除客户端连接
     */
    private void removeClient(ServiceInfo serviceInfo) {
        String clientKey = buildClientKey(serviceInfo);
        NettyRpcClient client = clientPool.get(clientKey);
        if(client != null) {
            try {
                client.close();
                // 记录连接关闭
                metricsCollector.recordConnectionClosed();
            } catch (Exception e) {
                log.warn("关闭客户端连接失败：{}", clientKey, e);
            }
            log.info("移除客户端连接：{}", clientKey);
        }
    }

    /**
     * 移除不活跃的客户端连接
     */
    private void removeInactiveClient() {
        for(Map.Entry<String, NettyRpcClient> entry: clientPool.entrySet()) {
            NettyRpcClient client = entry.getValue();
            if(!client.isActive()) {
                clientPool.remove(entry.getKey());
                try {
                    client.close();
                    // 记录连接关闭
                    metricsCollector.recordConnectionClosed();
                } catch (Exception e) {
                    log.warn("关闭不活跃客户端连接失败：{}", entry.getKey(), e);
                }
                log.info("移除不活跃客户端连接：{}", entry.getKey());
                break;
            }
        }
    }


    /**
     * 构建客户端键
     */
    private String buildClientKey(ServiceInfo serviceInfo) {
        return serviceInfo.getAddress() + ":" + serviceInfo.getPort();
    }

    /**
     * 判断是否为连接异常
     */
    private boolean isConnectionException(Exception e) {
        if (e.getCause() instanceof ConnectException ||
                e.getCause() instanceof ClosedChannelException) {
            return true;
        }
        String message = e.getMessage();
        return message != null && (message.contains("connection") || message.contains("Connection"));
    }
    /**
     * 获取连接池状态
     */
    public ConnectionPoolStatus getConnectionPoolStatus() {
        int activeConnections = 0;
        int totalConnections = clientPool.size();
        for(NettyRpcClient client: clientPool.values()) {
            if(client.isActive()) {
                activeConnections++;
            }
        }
        return new ConnectionPoolStatus(totalConnections, activeConnections, maxConnections);
    }

    /**
     * 获取服务缓存状态
     *
     * @return 服务缓存状态
     */
    public ServiceCacheStatus getServiceCacheStatus() {
        return new ServiceCacheStatus(serviceCache.size(), subscribedServices.size(), serviceCache);
    }

    /**
     * 连接池状态
     */
    public static class ConnectionPoolStatus {
        private final int totalConnections;
        private final int activeConnections;
        private final int maxConnections;

        public ConnectionPoolStatus(int totalConnections, int activeConnections, int maxConnections) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.maxConnections = maxConnections;
        }

        public int getTotalConnections() {
            return totalConnections;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public int getMaxConnections() {
            return maxConnections;
        }
        @Override
        public String toString() {
            return String.format("ConnectionPoolStatus{total=%d, active=%d, max=%d}",
                    totalConnections, activeConnections, maxConnections);
        }
    }

    /**
     * 服务缓存状态
     */
    public static class ServiceCacheStatus {
        private final int cachedServiceCount;
        private final int subscribedServiceCount;
        private final Map<String, List<ServiceInfo>> serviceCache;

        public ServiceCacheStatus(int cachedServiceCount, int subscribedServiceCount, Map<String, List<ServiceInfo>> serviceCache) {
            this.cachedServiceCount = cachedServiceCount;
            this.subscribedServiceCount = subscribedServiceCount;
            this.serviceCache = new ConcurrentHashMap<>(serviceCache);
        }

        public int getCachedServiceCount() {
            return cachedServiceCount;
        }

        public int getSubscribedServiceCount() {
            return subscribedServiceCount;
        }

        public Map<String, List<ServiceInfo>> getServiceCache() {
            return serviceCache;
        }

        @Override
        public String toString() {
            return String.format("ServiceCacheStatus{cached=%d, subscribed=%d}", 
                    cachedServiceCount, subscribedServiceCount);
        }
    }

    /**
     * 服务变化监听器实现
     */
    @Override
    public void onServiceAdded(String serviceName, ServiceInfo serviceInfo) {
        log.info("服务实例添加：{} -> {}", serviceName, serviceInfo);
        List<ServiceInfo> services = serviceCache.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>());
        if (!services.contains(serviceInfo)) {
            services.add(serviceInfo);
        }
    }

    @Override
    public void onServiceRemoved(String serviceName, ServiceInfo serviceInfo) {
        log.info("服务实例移除：{} -> {}", serviceName, serviceInfo);
        List<ServiceInfo> services = serviceCache.get(serviceName);
        if (services != null) {
            services.remove(serviceInfo);
            // 移除对应的客户端连接
            removeClient(serviceInfo);
        }
    }

    @Override
     public void onServiceUpdated(String serviceName, ServiceInfo oldServiceInfo, ServiceInfo newServiceInfo) {
         log.info("服务实例更新：{} -> {} 替换为 {}", serviceName, oldServiceInfo, newServiceInfo);
         List<ServiceInfo> services = serviceCache.get(serviceName);
         if (services != null) {
             int index = services.indexOf(oldServiceInfo);
             if (index >= 0) {
                 services.set(index, newServiceInfo);
             }
             // 移除旧的客户端连接
             removeClient(oldServiceInfo);
         }
     }

     @Override
     public void onServiceListChanged(String serviceName, List<ServiceInfo> serviceInfos) {
         log.info("服务列表变化：{} -> {} 个实例", serviceName, serviceInfos.size());
         serviceCache.put(serviceName, new CopyOnWriteArrayList<>(serviceInfos));
         
         // 清理不再存在的服务实例对应的客户端连接
         Set<String> currentClientKeys = ConcurrentHashMap.newKeySet();
         for (ServiceInfo serviceInfo : serviceInfos) {
             currentClientKeys.add(buildClientKey(serviceInfo));
         }
         
         // 移除不再存在的客户端连接
         clientPool.entrySet().removeIf(entry -> {
             String clientKey = entry.getKey();
             if (!currentClientKeys.contains(clientKey)) {
                 try {
                     entry.getValue().close();
                     log.info("移除过期客户端连接：{}", clientKey);
                 } catch (Exception e) {
                     log.warn("关闭过期客户端连接失败：{}", clientKey, e);
                 }
                 return true;
             }
             return false;
         });
         
         // 通知负载均衡器更新服务列表
          if (loadBalancer != null) {
              loadBalancer.updateServiceList(serviceInfos);
          }
      }

    /**
     * 连接健康状态回调实现
     */
    private class ConnectionHealthCallback implements ConnectionHealthChecker.HealthCallback {
        @Override
        public void onHealthyConnection(String clientKey, NettyRpcClient client) {
            // 连接健康，重置重试计数
            unhealthyRetryCount.remove(clientKey);
        }

        @Override
        public void onUnhealthyConnection(String clientKey, NettyRpcClient client) {
            // 连接不健康，增加重试计数
            int retryCount = unhealthyRetryCount.getOrDefault(clientKey, 0) + 1;
            unhealthyRetryCount.put(clientKey, retryCount);
            
            log.warn("检测到不健康连接：{}，重试次数：{}", clientKey, retryCount);
            
            // 如果重试次数超过最大值，移除连接
            if (retryCount >= healthChecker.getMaxRetries()) {
                log.warn("连接{}重试次数超过最大值{}，将被移除", clientKey, healthChecker.getMaxRetries());
                NettyRpcClient removedClient = clientPool.remove(clientKey);
                if (removedClient != null) {
                    try {
                        removedClient.close();
                        log.info("已移除不健康连接：{}", clientKey);
                    } catch (Exception e) {
                        log.warn("关闭不健康连接失败：{}", clientKey, e);
                    }
                }
                unhealthyRetryCount.remove(clientKey);
            }
        }
    }

    /**
     * 获取连接健康检查器状态
     *
     * @return 健康检查器状态
     */
    public HealthCheckerStatus getHealthCheckerStatus() {
        return new HealthCheckerStatus(
                healthChecker.isRunning(),
                healthChecker.getCheckInterval(),
                healthChecker.getConnectionTimeout(),
                healthChecker.getMaxRetries(),
                unhealthyRetryCount.size()
        );
    }
    
    /**
     * 获取监控指标摘要
     * @return 监控指标摘要
     */
    public MetricsCollector.MetricsSummary getMetricsSummary() {
        return metricsCollector.getMetricsSummary();
    }

    /**
     * 健康检查器状态
     */
    public static class HealthCheckerStatus {
        private final boolean running;
        private final long checkInterval;
        private final long connectionTimeout;
        private final int maxRetries;
        private final int unhealthyConnectionCount;

        public HealthCheckerStatus(boolean running, long checkInterval, long connectionTimeout, int maxRetries, int unhealthyConnectionCount) {
            this.running = running;
            this.checkInterval = checkInterval;
            this.connectionTimeout = connectionTimeout;
            this.maxRetries = maxRetries;
            this.unhealthyConnectionCount = unhealthyConnectionCount;
        }

        public boolean isRunning() {
            return running;
        }

        public long getCheckInterval() {
            return checkInterval;
        }

        public long getConnectionTimeout() {
            return connectionTimeout;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public int getUnhealthyConnectionCount() {
            return unhealthyConnectionCount;
        }

        @Override
        public String toString() {
            return String.format("HealthCheckerStatus{running=%s, checkInterval=%dms, connectionTimeout=%dms, maxRetries=%d, unhealthyCount=%d}",
                    running, checkInterval, connectionTimeout, maxRetries, unhealthyConnectionCount);
        }
    }

}
