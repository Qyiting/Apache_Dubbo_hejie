package com.rpc.client.loadbalance.lru;

import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * LRU（Least Recently Used）负载均衡器
 * 选择最近最少使用的服务实例进行负载均衡
 * 使用LRU缓存算法跟踪服务实例的使用情况
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class LRULoadBalancer implements LoadBalancer {

    /** 默认LRU缓存容量 */
    private static final int DEFAULT_CACHE_SIZE = 100;
    
    /** LRU缓存容量 */
    private final int cacheSize;
    
    /** LRU缓存，用于跟踪服务实例的使用顺序 */
    private final LRUCache<String, ServiceInfo> lruCache;
    
    /** 读写锁，保证线程安全 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /** 缓存服务实例列表的哈希值，用于检测服务列表变化 */
    private volatile int lastServiceListHash = 0;
    
    /**
     * 默认构造函数，使用默认缓存大小
     */
    public LRULoadBalancer() {
        this(DEFAULT_CACHE_SIZE);
    }
    
    /**
     * 构造函数
     * 
     * @param cacheSize LRU缓存大小
     */
    public LRULoadBalancer(int cacheSize) {
        if (cacheSize <= 0) {
            throw new IllegalArgumentException("LRU缓存大小必须大于0");
        }
        this.cacheSize = cacheSize;
        this.lruCache = new LRUCache<>(cacheSize);
        log.info("LRU负载均衡器初始化完成，缓存大小：{}", cacheSize);
    }
    
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfos, RpcRequest request) {
        if (serviceInfos == null || serviceInfos.isEmpty()) {
            log.warn("服务实例列表为空，无法进行负载均衡选择");
            return null;
        }
        
        // 过滤出活跃的服务实例
        List<ServiceInfo> activeServices = serviceInfos.stream()
                .filter(ServiceInfo::isActive)
                .collect(Collectors.toList());
                
        if (activeServices.isEmpty()) {
            log.warn("没有活跃的服务实例可用于负载均衡选择");
            return null;
        }
        
        // 如果只有一个实例，直接返回
        if (activeServices.size() == 1) {
            ServiceInfo selected = activeServices.get(0);
            updateLRUCache(selected);
            log.debug("只有一个活跃服务实例，直接选择：{}", selected.getFullAddress());
            return selected;
        }
        
        // 检查服务列表是否发生变化
        int currentServiceListHash = activeServices.hashCode();
        if (currentServiceListHash != lastServiceListHash) {
            updateActiveServiceList(activeServices);
            lastServiceListHash = currentServiceListHash;
        }
        
        // 使用LRU算法选择服务实例
        ServiceInfo selected = selectLRUService(activeServices);
        if (selected != null) {
            updateLRUCache(selected);
            log.debug("LRU负载均衡选择服务实例：{} (缓存大小：{}/{})", 
                    selected.getFullAddress(), lruCache.size(), cacheSize);
        }
        
        return selected;
    }
    
    /**
     * 使用LRU算法选择服务实例
     * 
     * @param activeServices 活跃的服务实例列表
     * @return 选中的服务实例
     */
    private ServiceInfo selectLRUService(List<ServiceInfo> activeServices) {
        lock.readLock().lock();
        try {
            // 1. 优先选择不在缓存中的服务（全新服务，认为是最久未使用的）
            for (ServiceInfo service : activeServices) {
                if (!lruCache.containsKey(service.getFullAddress())) {
                    log.debug("选择未缓存的服务实例：{}", service.getFullAddress());
                    return service;
                }
            }
            
            // 2. 如果所有服务都在缓存中，选择最久未使用的
            // LinkedHashMap的迭代顺序就是访问顺序（最久未使用的在前面）
            for (Map.Entry<String, ServiceInfo> entry : lruCache.entrySet()) {
                String serviceKey = entry.getKey();
                // 检查这个缓存的服务是否在当前活跃服务列表中
                for (ServiceInfo activeService : activeServices) {
                    if (activeService.getFullAddress().equals(serviceKey)) {
                        log.debug("选择LRU缓存中最久未使用的服务实例：{}", serviceKey);
                        return activeService; // 返回最久未使用的活跃服务
                    }
                }
            }
            
            // 3. 兜底：如果上述逻辑都没有找到合适的服务，随机选择一个
            log.warn("LRU算法未能选择到合适的服务，随机选择第一个服务");
            return activeServices.get(0);
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 更新LRU缓存
     * 
     * @param serviceInfo 被选中的服务实例
     */
    private void updateLRUCache(ServiceInfo serviceInfo) {
        lock.writeLock().lock();
        try {
            String serviceKey = serviceInfo.getFullAddress();
            lruCache.put(serviceKey, serviceInfo);
            log.trace("更新LRU缓存，服务实例：{}", serviceKey);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 更新服务列表，清理不再活跃的服务实例
     * 
     * @param serviceInfos 当前的服务实例列表
     */
    @Override
    public void updateServiceList(List<ServiceInfo> serviceInfos) {
        if (serviceInfos == null || serviceInfos.isEmpty()) {
            return;
        }
        
        // 过滤出活跃的服务实例
        List<ServiceInfo> activeServices = serviceInfos.stream()
                .filter(ServiceInfo::isActive)
                .collect(Collectors.toList());
                
        updateActiveServiceList(activeServices);
    }
    
    /**
     * 更新活跃服务列表，清理不再活跃的服务实例
     * @param activeServices 当前活跃的服务实例列表
     */
    private void updateActiveServiceList(List<ServiceInfo> activeServices) {
        lock.writeLock().lock();
        try {
            Set<String> activeServiceKeys = activeServices.stream()
                    .map(ServiceInfo::getFullAddress)
                    .collect(Collectors.toSet());
            
            // 移除不再活跃的服务实例
            lruCache.entrySet().removeIf(entry -> !activeServiceKeys.contains(entry.getKey()));
            
            log.debug("更新服务列表，当前活跃服务数量：{}，缓存大小：{}", 
                    activeServices.size(), lruCache.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public String getAlgorithm() {
        return Algorithm.LRU;
    }
    
    /**
     * 获取LRU缓存大小
     * 
     * @return 缓存大小
     */
    public int getCacheSize() {
        return cacheSize;
    }
    
    /**
     * 获取当前缓存中的服务实例数量
     * 
     * @return 当前缓存大小
     */
    public int getCurrentCacheSize() {
        lock.readLock().lock();
        try {
            return lruCache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 清空LRU缓存
     * 主要用于测试
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            lruCache.clear();
            lastServiceListHash = 0;
            log.debug("LRU缓存已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取缓存中的服务实例列表（按使用顺序）
     * 主要用于测试和调试
     * 
     * @return 服务实例列表
     */
    public List<String> getCachedServices() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(lruCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        return "LRULoadBalancer{algorithm=" + getAlgorithm() + 
               ", cacheSize=" + cacheSize + 
               ", currentCacheSize=" + getCurrentCacheSize() + "}";
    }
    
    /**
     * LRU缓存实现
     * 基于LinkedHashMap实现的线程安全LRU缓存
     * 
     * @param <K> 键类型
     * @param <V> 值类型
     */
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;
        
        public LRUCache(int maxSize) {
            // 设置初始容量、负载因子和访问顺序
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }
        
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}