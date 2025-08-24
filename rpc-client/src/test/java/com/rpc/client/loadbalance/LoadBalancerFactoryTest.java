package com.rpc.client.loadbalance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LoadBalancerFactory单元测试
 * 
 * @author 何杰
 * @version 1.0
 */
@DisplayName("LoadBalancerFactory测试")
class LoadBalancerFactoryTest {
    
    @BeforeEach
    void setUp() {
        // 清理缓存，确保测试独立性
        LoadBalancerFactory.clearCache();
    }
    
    @Test
    @DisplayName("测试获取支持的算法列表")
    void testGetSupportedAlgorithms() {
        Set<String> algorithms = LoadBalancerFactory.getSupportedAlgorithms();
        
        assertNotNull(algorithms, "算法列表不应为null");
        assertFalse(algorithms.isEmpty(), "算法列表不应为空");
        
        // 验证包含预期的算法
        assertTrue(algorithms.contains("random"), "应包含random算法");
        assertTrue(algorithms.contains("round_robin"), "应包含round_robin算法");
        assertTrue(algorithms.contains("consistent_hash"), "应包含consistent_hash算法");
        assertTrue(algorithms.contains("lru"), "应包含lru算法");
        assertTrue(algorithms.contains("lfu"), "应包含lfu算法");
    }
    
    @Test
    @DisplayName("测试算法支持检查")
    void testIsAlgorithmSupported() {
        // 测试支持的算法
        assertTrue(LoadBalancerFactory.isSupported("random"), "random算法应被支持");
        assertTrue(LoadBalancerFactory.isSupported("round_robin"), "round_robin算法应被支持");
        assertTrue(LoadBalancerFactory.isSupported("lru"), "lru算法应被支持");
        assertTrue(LoadBalancerFactory.isSupported("lfu"), "lfu算法应被支持");
        assertTrue(LoadBalancerFactory.isSupported("consistent_hash"), "consistent_hash算法应被支持");
        
        // 测试不支持的算法
        assertFalse(LoadBalancerFactory.isSupported("unknown"), "unknown算法不应被支持");
        assertFalse(LoadBalancerFactory.isSupported("invalid"), "invalid算法不应被支持");
        assertFalse(LoadBalancerFactory.isSupported(""), "空字符串不应被支持");
        assertFalse(LoadBalancerFactory.isSupported(null), "null不应被支持");
    }
    
    @Test
    @DisplayName("测试创建负载均衡器")
    void testGetLoadBalancer() {
        // 测试创建各种负载均衡器
        LoadBalancer randomLB = LoadBalancerFactory.getLoadBalancer("random");
        assertNotNull(randomLB, "random负载均衡器不应为null");
        assertEquals("random", randomLB.getAlgorithm().toLowerCase(), "算法名称应匹配");
        
        LoadBalancer roundRobinLB = LoadBalancerFactory.getLoadBalancer("round_robin");
        assertNotNull(roundRobinLB, "round_robin负载均衡器不应为null");
        assertEquals("round_robin", roundRobinLB.getAlgorithm().toLowerCase(), "算法名称应匹配");
        
        LoadBalancer lruLB = LoadBalancerFactory.getLoadBalancer("lru");
        assertNotNull(lruLB, "lru负载均衡器不应为null");
        assertEquals("lru", lruLB.getAlgorithm().toLowerCase(), "算法名称应匹配");
        
        LoadBalancer lfuLB = LoadBalancerFactory.getLoadBalancer("lfu");
        assertNotNull(lfuLB, "lfu负载均衡器不应为null");
        assertEquals("lfu", lfuLB.getAlgorithm().toLowerCase(), "算法名称应匹配");
        
        LoadBalancer consistentHashLB = LoadBalancerFactory.getLoadBalancer("consistent_hash");
        assertNotNull(consistentHashLB, "consistent_hash负载均衡器不应为null");
        assertEquals("consistent_hash", consistentHashLB.getAlgorithm().toLowerCase(), "算法名称应匹配");
    }
    
    @Test
    @DisplayName("测试创建不存在的负载均衡器")
    void testGetNonExistentLoadBalancer() {
        // 测试不存在的算法应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalancerFactory.getLoadBalancer("unknown");
        }, "不存在的算法应抛出IllegalArgumentException");
        
        // 测试null算法应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalancerFactory.getLoadBalancer(null);
        }, "null算法应抛出IllegalArgumentException");
        
        // 测试空字符串算法应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalancerFactory.getLoadBalancer("");
        }, "空字符串算法应抛出IllegalArgumentException");
    }
    
    @Test
    @DisplayName("测试缓存机制")
    void testCaching() {
        // 第一次获取
        LoadBalancer lb1 = LoadBalancerFactory.getLoadBalancer("random");
        assertNotNull(lb1, "第一次获取不应为null");
        
        // 第二次获取，应该是同一个实例（缓存）
        LoadBalancer lb2 = LoadBalancerFactory.getLoadBalancer("random");
        assertNotNull(lb2, "第二次获取不应为null");
        assertSame(lb1, lb2, "应该返回缓存的同一个实例");
        
        // 清理缓存后再获取
        LoadBalancerFactory.clearCache();
        LoadBalancer lb3 = LoadBalancerFactory.getLoadBalancer("random");
        assertNotNull(lb3, "清理缓存后获取不应为null");
        assertNotSame(lb1, lb3, "清理缓存后应该是新实例");
    }
    
    @Test
    @DisplayName("测试获取所有负载均衡器")
    void testGetAllLoadBalancers() {
        List<LoadBalancer> allLoadBalancers = LoadBalancerFactory.getAllLoadBalancers();
        
        assertNotNull(allLoadBalancers, "负载均衡器列表不应为null");
        assertFalse(allLoadBalancers.isEmpty(), "负载均衡器列表不应为空");
        
        // 验证每个负载均衡器都有有效的算法名称
        for (LoadBalancer lb : allLoadBalancers) {
            assertNotNull(lb, "负载均衡器不应为null");
            assertNotNull(lb.getAlgorithm(), "算法名称不应为null");
            assertFalse(lb.getAlgorithm().trim().isEmpty(), "算法名称不应为空");
        }
    }
    
    @Test
    @DisplayName("测试获取负载均衡器信息")
    void testGetLoadBalancerInfo() {
        Map<String, String> info = LoadBalancerFactory.getLoadBalancerInfo();
        
        assertNotNull(info, "负载均衡器信息不应为null");
        assertFalse(info.isEmpty(), "负载均衡器信息不应为空");
        
        // 验证信息的完整性
        for (Map.Entry<String, String> entry : info.entrySet()) {
            assertNotNull(entry.getKey(), "算法名称不应为null");
            assertNotNull(entry.getValue(), "类名不应为null");
            assertFalse(entry.getKey().trim().isEmpty(), "算法名称不应为空");
            assertFalse(entry.getValue().trim().isEmpty(), "类名不应为空");
        }
        
        // 验证包含预期的算法
        assertTrue(info.containsKey("random"), "应包含random算法信息");
        assertTrue(info.containsKey("round_robin"), "应包含round_robin算法信息");
        assertTrue(info.containsKey("lru"), "应包含lru算法信息");
        assertTrue(info.containsKey("lfu"), "应包含lfu算法信息");
        assertTrue(info.containsKey("consistent_hash"), "应包含consistent_hash算法信息");
    }
    
    @Test
    @DisplayName("测试大小写不敏感")
    void testCaseInsensitive() {
        // 测试不同大小写的算法名称
        LoadBalancer lb1 = LoadBalancerFactory.getLoadBalancer("RANDOM");
        LoadBalancer lb2 = LoadBalancerFactory.getLoadBalancer("Random");
        LoadBalancer lb3 = LoadBalancerFactory.getLoadBalancer("random");
        
        assertNotNull(lb1, "大写算法名称应该有效");
        assertNotNull(lb2, "混合大小写算法名称应该有效");
        assertNotNull(lb3, "小写算法名称应该有效");
        
        // 验证算法名称统一为小写
        assertEquals("random", lb1.getAlgorithm().toLowerCase());
        assertEquals("random", lb2.getAlgorithm().toLowerCase());
        assertEquals("random", lb3.getAlgorithm().toLowerCase());
    }
}