package com.rpc.client.loadbalance;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器工厂类
 * 使用SPI机制动态加载负载均衡器实现
 * 
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class LoadBalancerFactory {
    
    /** 负载均衡器缓存，避免重复创建 */
    private static final Map<String, LoadBalancer> LOAD_BALANCER_CACHE = new ConcurrentHashMap<>();
    
    /** ServiceLoader实例，用于加载LoadBalancer实现 */
    private static final ServiceLoader<LoadBalancer> SERVICE_LOADER = ServiceLoader.load(LoadBalancer.class);
    
    /** 可用的负载均衡器映射 */
    private static final Map<String, Class<? extends LoadBalancer>> AVAILABLE_LOAD_BALANCERS = new HashMap<>();
    
    static {
        // 初始化时扫描所有可用的负载均衡器
        loadAvailableLoadBalancers();
    }
    
    /**
     * 根据算法名称获取负载均衡器实例
     * 
     * @param algorithm 负载均衡算法名称
     * @return 负载均衡器实例
     * @throws IllegalArgumentException 如果算法名称不支持
     */
    public static LoadBalancer getLoadBalancer(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("负载均衡算法名称不能为空");
        }
        
        algorithm = algorithm.trim().toLowerCase();
        
        // 先从缓存中获取
        LoadBalancer cachedBalancer = LOAD_BALANCER_CACHE.get(algorithm);
        if (cachedBalancer != null) {
            log.debug("从缓存中获取负载均衡器：{}", algorithm);
            return cachedBalancer;
        }
        
        // 通过SPI加载
        LoadBalancer loadBalancer = createLoadBalancerBySPI(algorithm);
        if (loadBalancer != null) {
            // 缓存实例
            LOAD_BALANCER_CACHE.put(algorithm, loadBalancer);
            log.info("成功创建负载均衡器：{} -> {}", algorithm, loadBalancer.getClass().getSimpleName());
            return loadBalancer;
        }
        
        throw new IllegalArgumentException("不支持的负载均衡算法：" + algorithm + 
                ". 支持的算法：" + getSupportedAlgorithms());
    }
    
    /**
     * 通过SPI机制创建负载均衡器
     * 
     * @param algorithm 算法名称
     * @return 负载均衡器实例，如果未找到则返回null
     */
    private static LoadBalancer createLoadBalancerBySPI(String algorithm) {
        synchronized (SERVICE_LOADER) {
            // 重新加载以确保获取最新的实现
            SERVICE_LOADER.reload();
            
            for (LoadBalancer loadBalancer : SERVICE_LOADER) {
                if (algorithm.equals(loadBalancer.getAlgorithm().toLowerCase())) {
                    log.debug("通过SPI找到负载均衡器：{} -> {}", algorithm, loadBalancer.getClass().getName());
                    return loadBalancer;
                }
            }
        }
        
        log.warn("未通过SPI找到负载均衡器：{}", algorithm);
        return null;
    }
    
    /**
     * 获取所有支持的负载均衡算法
     * 
     * @return 支持的算法列表
     */
    public static Set<String> getSupportedAlgorithms() {
        Set<String> algorithms = new HashSet<>();
        
        synchronized (SERVICE_LOADER) {
            SERVICE_LOADER.reload();
            for (LoadBalancer loadBalancer : SERVICE_LOADER) {
                algorithms.add(loadBalancer.getAlgorithm().toLowerCase());
            }
        }
        
        return algorithms;
    }
    
    /**
     * 获取所有可用的负载均衡器实例
     * 
     * @return 负载均衡器实例列表
     */
    public static List<LoadBalancer> getAllLoadBalancers() {
        List<LoadBalancer> loadBalancers = new ArrayList<>();
        
        synchronized (SERVICE_LOADER) {
            SERVICE_LOADER.reload();
            for (LoadBalancer loadBalancer : SERVICE_LOADER) {
                loadBalancers.add(loadBalancer);
            }
        }
        
        return loadBalancers;
    }
    
    /**
     * 清空负载均衡器缓存
     */
    public static void clearCache() {
        LOAD_BALANCER_CACHE.clear();
        log.info("负载均衡器缓存已清空");
    }
    
    /**
     * 检查是否支持指定的负载均衡算法
     * 
     * @param algorithm 算法名称
     * @return 是否支持
     */
    public static boolean isSupported(String algorithm) {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            return false;
        }
        
        return getSupportedAlgorithms().contains(algorithm.trim().toLowerCase());
    }
    
    /**
     * 加载所有可用的负载均衡器
     */
    private static void loadAvailableLoadBalancers() {
        try {
            synchronized (SERVICE_LOADER) {
                SERVICE_LOADER.reload();
                int count = 0;
                for (LoadBalancer loadBalancer : SERVICE_LOADER) {
                    String algorithm = loadBalancer.getAlgorithm().toLowerCase();
                    AVAILABLE_LOAD_BALANCERS.put(algorithm, loadBalancer.getClass());
                    count++;
                    log.debug("发现负载均衡器：{} -> {}", algorithm, loadBalancer.getClass().getName());
                }
                log.info("成功加载 {} 个负载均衡器实现", count);
            }
        } catch (Exception e) {
            log.error("加载负载均衡器时发生异常", e);
        }
    }
    
    /**
     * 获取负载均衡器的详细信息
     * 
     * @return 负载均衡器信息映射
     */
    public static Map<String, String> getLoadBalancerInfo() {
        Map<String, String> info = new HashMap<>();
        
        synchronized (SERVICE_LOADER) {
            SERVICE_LOADER.reload();
            for (LoadBalancer loadBalancer : SERVICE_LOADER) {
                String algorithm = loadBalancer.getAlgorithm().toLowerCase();
                String className = loadBalancer.getClass().getName();
                info.put(algorithm, className);
            }
        }
        
        return info;
    }
    

    
    /**
     * 私有构造函数，防止实例化
     */
    private LoadBalancerFactory() {
        throw new UnsupportedOperationException("工厂类不允许实例化");
    }
}