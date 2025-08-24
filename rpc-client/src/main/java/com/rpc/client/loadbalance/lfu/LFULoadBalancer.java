package com.rpc.client.loadbalance.lfu;

import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * LFU（Least Frequently Used）负载均衡器
 * 选择访问频率最低的服务实例进行负载均衡
 * 使用频率统计算法跟踪服务实例的使用情况
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class LFULoadBalancer implements LoadBalancer {

    /** 默认最大跟踪服务数量 */
    private static final int DEFAULT_MAX_SERVICES = 100;
    
    /** 最大跟踪服务数量 */
    private final int maxServices;
    
    /** 服务实例访问频率统计 */
    private final Map<String, FrequencyNode> frequencyMap = new ConcurrentHashMap<>();
    
    /** 频率桶，按频率分组存储服务实例 */
    private final Map<Integer, Set<String>> frequencyBuckets = new ConcurrentHashMap<>();
    
    /** 最小频率值 */
    private volatile int minFrequency = 1;
    
    /** 读写锁，保证线程安全 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    /** 随机数生成器，用于随机选择相同频率的服务 */
    private final Random random = new Random();
    
    /** 缓存服务实例列表的哈希值，用于检测服务列表变化 */
    private volatile int lastServiceListHash = 0;
    
    /**
     * 默认构造函数，使用默认最大服务数量
     */
    public LFULoadBalancer() {
        this(DEFAULT_MAX_SERVICES);
    }
    
    /**
     * 构造函数
     * 
     * @param maxServices 最大跟踪服务数量
     */
    public LFULoadBalancer(int maxServices) {
        if (maxServices <= 0) {
            throw new IllegalArgumentException("最大服务数量必须大于0");
        }
        this.maxServices = maxServices;
        log.info("LFU负载均衡器初始化完成，最大跟踪服务数量：{}", maxServices);
    }
    
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfos, RpcRequest request) {
        if (serviceInfos == null || serviceInfos.isEmpty()) {
            log.warn("服务实例列表为空，无法进行负载均衡");
            return null;
        }
        
        // 过滤活跃的服务实例
        List<ServiceInfo> activeServices = serviceInfos.stream()
                .filter(service -> service.getStatus() == ServiceInfo.ServiceStatus.ACTIVE)
                .collect(Collectors.toList());
        
        if (activeServices.isEmpty()) {
            log.warn("没有活跃的服务实例可用");
            return null;
        }
        
        // 检查服务列表是否发生变化
        int currentHash = activeServices.hashCode();
        if (currentHash != lastServiceListHash) {
            updateActiveServiceList(activeServices);
            lastServiceListHash = currentHash;
        }
        
        // 选择频率最低的服务实例
        log.debug("开始选择LFU服务，活跃服务数量：{}", activeServices.size());
        ServiceInfo selectedService = selectLFUService(activeServices);
        log.debug("LFU服务选择完成，选中服务：{}", selectedService != null ? selectedService.getAddress() + ":" + selectedService.getPort() : "null");
        
        if (selectedService != null) {
            // 更新服务频率（如果是新服务会自动初始化）
            updateFrequency(selectedService);
            
            log.debug("LFU负载均衡选择服务实例：{}:{}, 当前频率：{}", 
                    selectedService.getAddress(), selectedService.getPort(),
                    getServiceFrequency(selectedService));
        }
        
        return selectedService;
    }
    
    /**
     * 选择频率最低的服务实例
     * 
     * @param activeServices 活跃的服务实例列表
     * @return 选中的服务实例
     */
    private ServiceInfo selectLFUService(List<ServiceInfo> activeServices) {
        lock.readLock().lock();
        try {
            log.debug("selectLFUService开始，活跃服务数量：{}", activeServices.size());
            // 检查服务列表是否为空
            if (activeServices.isEmpty()) {
                log.debug("活跃服务列表为空，返回null");
                return null;
            }
            
            // 找出频率最低的服务（未跟踪的服务频率视为0）
            int minFreq = Integer.MAX_VALUE;
            List<ServiceInfo> minFreqServices = new ArrayList<>();
            
            for (ServiceInfo service : activeServices) {
                int frequency = getServiceFrequency(service); // 未跟踪的服务返回0
                
                if (frequency < minFreq) {
                    minFreq = frequency;
                    minFreqServices.clear();
                    minFreqServices.add(service);
                } else if (frequency == minFreq) {
                    minFreqServices.add(service);
                }
            }
            
            log.debug("最小频率：{}, 最小频率服务数量：{}", minFreq, minFreqServices.size());
            
            // 从最小频率的服务中随机选择一个
            if (!minFreqServices.isEmpty()) {
                int randomIndex = random.nextInt(minFreqServices.size());
                ServiceInfo selectedService = minFreqServices.get(randomIndex);
                log.debug("选择最小频率服务：{}:{}, 频率：{}", selectedService.getAddress(), selectedService.getPort(), minFreq);
                return selectedService;
            }
            
            // 理论上不应该到达这里，但作为备用方案
            ServiceInfo fallbackService = activeServices.get(0);
            log.debug("备用选择：{}:{}", fallbackService.getAddress(), fallbackService.getPort());
            return fallbackService;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 更新服务实例的访问频率
     * 
     * @param serviceInfo 服务实例
     */
    private void updateFrequency(ServiceInfo serviceInfo) {
        String serviceKey = buildServiceKey(serviceInfo);
        
        lock.writeLock().lock();
        try {
            FrequencyNode node = frequencyMap.get(serviceKey);
            if (node == null) {
                // 新服务，初始化频率为1
                initializeService(serviceKey);
                return;
            }
            
            int oldFreq = node.frequency;
            int newFreq = oldFreq + 1;
            
            // 从旧频率桶中移除
            Set<String> oldBucket = frequencyBuckets.get(oldFreq);
            if (oldBucket != null) {
                oldBucket.remove(serviceKey);
                if (oldBucket.isEmpty() && oldFreq == minFrequency) {
                    // 如果最小频率桶为空，更新最小频率
                    minFrequency++;
                }
            }
            
            // 更新频率节点
            node.frequency = newFreq;
            node.lastAccessTime = System.currentTimeMillis();
            
            // 添加到新频率桶
            frequencyBuckets.computeIfAbsent(newFreq, k -> ConcurrentHashMap.newKeySet()).add(serviceKey);
            
            log.debug("服务 {} 频率更新：{} -> {}", serviceKey, oldFreq, newFreq);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 初始化新服务的频率统计
     * 
     * @param serviceKey 服务键
     */
    private void initializeService(String serviceKey) {
        lock.writeLock().lock();
        try {
            // 检查是否需要淘汰服务
            if (frequencyMap.size() >= maxServices) {
                evictLeastFrequentService();
            }
            
            // 添加新服务，初始频率为1
            FrequencyNode node = new FrequencyNode(1, System.currentTimeMillis());
            frequencyMap.put(serviceKey, node);
            
            // 添加到频率桶
            frequencyBuckets.computeIfAbsent(1, k -> ConcurrentHashMap.newKeySet()).add(serviceKey);
            
            // 更新最小频率
            minFrequency = 1;
            
            log.debug("初始化服务 {} 频率统计，初始频率：1", serviceKey);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 淘汰访问频率最低的服务
     */
    private void evictLeastFrequentService() {
        Set<String> minFreqServices = frequencyBuckets.get(minFrequency);
        if (minFreqServices != null && !minFreqServices.isEmpty()) {
            // 从最小频率的服务中选择最久未访问的进行淘汰
            String evictKey = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (String serviceKey : minFreqServices) {
                FrequencyNode node = frequencyMap.get(serviceKey);
                if (node != null && node.lastAccessTime < oldestTime) {
                    oldestTime = node.lastAccessTime;
                    evictKey = serviceKey;
                }
            }
            
            if (evictKey != null) {
                // 移除服务
                frequencyMap.remove(evictKey);
                minFreqServices.remove(evictKey);
                
                // 如果最小频率桶为空，寻找下一个最小频率
                if (minFreqServices.isEmpty()) {
                    updateMinFrequency();
                }
                
                log.debug("淘汰服务：{}", evictKey);
            }
        }
    }
    
    /**
     * 更新最小频率值
     */
    private void updateMinFrequency() {
        minFrequency = frequencyBuckets.keySet().stream()
                .filter(freq -> !frequencyBuckets.get(freq).isEmpty())
                .min(Integer::compareTo)
                .orElse(1);
    }
    
    /**
     * 更新服务列表，清理不再活跃的服务实例
     * 注意：只有当服务列表发生实际变化时才会移除服务
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
                .filter(service -> service.getStatus() == ServiceInfo.ServiceStatus.ACTIVE)
                .collect(Collectors.toList());
                
        updateActiveServiceList(activeServices);
    }
    
    /**
     * 更新活跃服务列表，清理不再活跃的服务实例
     * 注意：只有当服务列表发生实际变化时才会移除服务
     * 
     * @param activeServices 当前活跃的服务实例列表
     */
    private void updateActiveServiceList(List<ServiceInfo> activeServices) {
        lock.writeLock().lock();
        try {
            Set<String> activeServiceKeys = activeServices.stream()
                    .map(this::buildServiceKey)
                    .collect(Collectors.toSet());
            
            // 只有当前活跃服务列表包含所有已跟踪服务时，才移除不在列表中的服务
            // 这避免了在部分服务选择时错误地移除其他有效服务
            boolean shouldCleanup = activeServices.size() > 1 || 
                    (activeServices.size() == 1 && frequencyMap.isEmpty());
            
            if (shouldCleanup) {
                // 移除不再活跃的服务实例
                Iterator<Map.Entry<String, FrequencyNode>> iterator = frequencyMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, FrequencyNode> entry = iterator.next();
                    String serviceKey = entry.getKey();
                    
                    if (!activeServiceKeys.contains(serviceKey)) {
                        // 从频率桶中移除
                        int frequency = entry.getValue().frequency;
                        Set<String> bucket = frequencyBuckets.get(frequency);
                        if (bucket != null) {
                            bucket.remove(serviceKey);
                            if (bucket.isEmpty()) {
                                frequencyBuckets.remove(frequency);
                            }
                        }
                        
                        // 从频率映射中移除
                        iterator.remove();
                        log.debug("移除不活跃的服务：{}", serviceKey);
                    }
                }
                
                // 更新最小频率
                if (!frequencyBuckets.isEmpty()) {
                    updateMinFrequency();
                } else {
                    minFrequency = 1;
                }
            }
            
            log.debug("服务列表更新完成，当前跟踪服务数量：{}", frequencyMap.size());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 构建服务实例的唯一键
     * 
     * @param serviceInfo 服务实例
     * @return 服务键
     */
    private String buildServiceKey(ServiceInfo serviceInfo) {
        return serviceInfo.getAddress() + ":" + serviceInfo.getPort();
    }
    
    /**
     * 获取服务实例的访问频率
     * 
     * @param serviceInfo 服务实例
     * @return 访问频率
     */
    public int getServiceFrequency(ServiceInfo serviceInfo) {
        String serviceKey = buildServiceKey(serviceInfo);
        lock.readLock().lock();
        try {
            FrequencyNode node = frequencyMap.get(serviceKey);
            return node != null ? node.frequency : 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String getAlgorithm() {
        return Algorithm.LFU;
    }
    
    /**
     * 获取最大跟踪服务数量
     * 
     * @return 最大跟踪服务数量
     */
    public int getMaxServices() {
        return maxServices;
    }
    
    /**
     * 获取当前跟踪的服务数量
     * 
     * @return 当前跟踪的服务数量
     */
    public int getCurrentServiceCount() {
        lock.readLock().lock();
        try {
            return frequencyMap.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取最小频率值
     * 
     * @return 最小频率值
     */
    public int getMinFrequency() {
        return minFrequency;
    }
    
    /**
     * 清空频率统计
     */
    public void clearFrequencyStats() {
        lock.writeLock().lock();
        try {
            frequencyMap.clear();
            frequencyBuckets.clear();
            minFrequency = 1;
            log.info("LFU频率统计已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取所有跟踪的服务及其频率
     * 
     * @return 服务频率映射
     */
    public Map<String, Integer> getServiceFrequencies() {
        lock.readLock().lock();
        try {
            Map<String, Integer> result = new HashMap<>();
            for (Map.Entry<String, FrequencyNode> entry : frequencyMap.entrySet()) {
                result.put(entry.getKey(), entry.getValue().frequency);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        return String.format("LFULoadBalancer{maxServices=%d, currentServices=%d, minFrequency=%d}", 
                maxServices, getCurrentServiceCount(), minFrequency);
    }
    
    /**
     * 频率节点，存储服务的访问频率和最后访问时间
     */
    private static class FrequencyNode {
        volatile int frequency;
        volatile long lastAccessTime;
        
        public FrequencyNode(int frequency, long lastAccessTime) {
            this.frequency = frequency;
            this.lastAccessTime = lastAccessTime;
        }
    }
}