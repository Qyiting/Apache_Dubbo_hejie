package com.rpc.client.loadbalance.hash;

import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一致性哈希负载均衡器
 * 使用一致性哈希算法进行负载均衡，支持虚拟节点以提高负载均衡的均匀性
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ConsistentHashLoadBalancer implements LoadBalancer {

    /** 默认虚拟节点数量 */
    private static final int DEFAULT_VIRTUAL_NODE_COUNT = 160;
    
    /** 虚拟节点数量 */
    private final int virtualNodeCount;
    
    /** 虚拟节点环，使用TreeMap保持有序 */
    private final TreeMap<Long, ServiceInfo> virtualNodes = new TreeMap<>();
    
    /** 缓存服务实例列表的哈希值，用于检测服务列表变化 */
    private volatile int lastServiceListHash = 0;
    
    /** 哈希算法实例缓存 */
    private static final ThreadLocal<MessageDigest> MD5_DIGEST = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    });
    
    /**
     * 默认构造函数，使用默认虚拟节点数量
     */
    public ConsistentHashLoadBalancer() {
        this(DEFAULT_VIRTUAL_NODE_COUNT);
    }
    
    /**
     * 构造函数
     * 
     * @param virtualNodeCount 虚拟节点数量
     */
    public ConsistentHashLoadBalancer(int virtualNodeCount) {
        if (virtualNodeCount <= 0) {
            throw new IllegalArgumentException("虚拟节点数量必须大于0");
        }
        this.virtualNodeCount = virtualNodeCount;
        log.info("一致性哈希负载均衡器初始化完成，虚拟节点数量：{}", virtualNodeCount);
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
            log.debug("只有一个活跃服务实例，直接选择：{}", selected.getFullAddress());
            return selected;
        }
        
        // 检查服务列表是否发生变化，如果变化则重建虚拟节点环
        int currentServiceListHash = calculateServiceListHash(activeServices);
        if (currentServiceListHash != lastServiceListHash) {
            synchronized (this) {
                if (currentServiceListHash != lastServiceListHash) {
                    buildVirtualNodeRing(activeServices);
                    lastServiceListHash = currentServiceListHash;
                    log.debug("服务列表发生变化，重建虚拟节点环，活跃服务数量：{}", activeServices.size());
                }
            }
        }
        
        // 根据请求计算哈希值并选择服务实例
        String hashKey = buildHashKey(request);
        long hash = hash(hashKey);
        ServiceInfo selected = selectFromRing(hash);
        
        log.debug("一致性哈希负载均衡选择服务实例：{} (哈希键：{}, 哈希值：{})", 
                selected.getFullAddress(), hashKey, hash);
        return selected;
    }
    
    /**
     * 构建虚拟节点环
     * 
     * @param serviceInfos 活跃的服务实例列表
     */
    private void buildVirtualNodeRing(List<ServiceInfo> serviceInfos) {
        virtualNodes.clear();
        
        for (ServiceInfo serviceInfo : serviceInfos) {
            for (int i = 0; i < virtualNodeCount; i++) {
                String virtualNodeKey = serviceInfo.getFullAddress() + "#" + i;
                long hash = hash(virtualNodeKey);
                virtualNodes.put(hash, serviceInfo);
            }
        }
        
        log.debug("虚拟节点环构建完成，服务实例数：{}，虚拟节点总数：{}", 
                serviceInfos.size(), virtualNodes.size());
    }
    
    /**
     * 从虚拟节点环中选择服务实例
     * 
     * @param hash 请求的哈希值
     * @return 选中的服务实例
     */
    private ServiceInfo selectFromRing(long hash) {
        if (virtualNodes.isEmpty()) {
            return null;
        }
        
        // 顺时针查找第一个大于等于hash值的虚拟节点
        Map.Entry<Long, ServiceInfo> entry = virtualNodes.ceilingEntry(hash);
        
        // 如果没有找到，则选择环上的第一个节点（环形结构）
        if (entry == null) {
            entry = virtualNodes.firstEntry();
        }
        
        return entry.getValue();
    }
    
    /**
     * 构建请求的哈希键
     * 优先使用请求ID，如果没有则使用接口名和方法名
     * 
     * @param request RPC请求
     * @return 哈希键
     */
    private String buildHashKey(RpcRequest request) {
        if (request == null) {
            return String.valueOf(System.currentTimeMillis());
        }
        
        // 优先使用请求ID
        if (request.getRequestId() != null) {
            return String.valueOf(request.getRequestId());
        }
        
        // 使用接口名和方法名组合
        StringBuilder keyBuilder = new StringBuilder();
        if (request.getInterfaceName() != null) {
            keyBuilder.append(request.getInterfaceName());
        }
        if (request.getMethodName() != null) {
            keyBuilder.append("#").append(request.getMethodName());
        }
        
        // 如果都没有，使用当前时间戳
        if (keyBuilder.length() == 0) {
            keyBuilder.append(System.currentTimeMillis());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 计算字符串的哈希值
     * 使用MD5算法计算哈希值，并取前8个字节转换为long
     * 
     * @param key 要计算哈希值的字符串
     * @return 哈希值
     */
    private long hash(String key) {
        MessageDigest md5 = MD5_DIGEST.get();
        md5.reset();
        byte[] digest = md5.digest(key.getBytes(StandardCharsets.UTF_8));
        
        // 取前8个字节转换为long
        long hash = 0;
        for (int i = 0; i < 8 && i < digest.length; i++) {
            hash = (hash << 8) | (digest[i] & 0xFF);
        }
        
        return hash;
    }
    
    /**
     * 计算服务列表的哈希值，用于检测服务列表变化
     * 
     * @param serviceInfos 服务实例列表
     * @return 哈希值
     */
    private int calculateServiceListHash(List<ServiceInfo> serviceInfos) {
        return serviceInfos.stream()
                .map(ServiceInfo::getServiceInstanceKey)
                .sorted()
                .collect(Collectors.joining(","))
                .hashCode();
    }

    @Override
    public String getAlgorithm() {
        return Algorithm.CONSISTENT_HASH;
    }
    
    /**
     * 获取虚拟节点数量
     * 
     * @return 虚拟节点数量
     */
    public int getVirtualNodeCount() {
        return virtualNodeCount;
    }
    
    /**
     * 获取当前虚拟节点环的大小
     * 
     * @return 虚拟节点环大小
     */
    public int getCurrentVirtualNodeRingSize() {
        return virtualNodes.size();
    }
    
    /**
     * 清空虚拟节点环（主要用于测试）
     */
    public void clearVirtualNodeRing() {
        synchronized (this) {
            virtualNodes.clear();
            lastServiceListHash = 0;
            log.debug("虚拟节点环已清空");
        }
    }
    
    @Override
    public String toString() {
        return "ConsistentHashLoadBalancer{" +
                "algorithm=" + getAlgorithm() +
                ", virtualNodeCount=" + virtualNodeCount +
                ", currentRingSize=" + virtualNodes.size() +
                "}";
    }
}