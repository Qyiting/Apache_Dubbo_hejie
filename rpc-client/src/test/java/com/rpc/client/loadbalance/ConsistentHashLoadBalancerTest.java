package com.rpc.client.loadbalance;

import com.rpc.client.loadbalance.hash.ConsistentHashLoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * 一致性哈希负载均衡器测试类
 * 
 * @author 何杰
 * @version 1.0
 */
public class ConsistentHashLoadBalancerTest {

    private ConsistentHashLoadBalancer loadBalancer;
    private List<ServiceInfo> serviceInfos;
    
    @Before
    public void setUp() {
        loadBalancer = new ConsistentHashLoadBalancer();
        serviceInfos = createTestServiceInfos();
    }
    
    /**
     * 创建测试用的服务实例列表
     */
    private List<ServiceInfo> createTestServiceInfos() {
        List<ServiceInfo> services = new ArrayList<>();
        
        // 创建5个测试服务实例
        for (int i = 1; i <= 5; i++) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setServiceName("com.rpc.example.UserService");
            serviceInfo.setAddress("192.168.1." + i);
            serviceInfo.setPort(8080 + i);
            serviceInfo.setVersion("1.0.0");
            serviceInfo.setGroup("default");
            serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
            serviceInfo.setWeight(1);
            services.add(serviceInfo);
        }
        
        return services;
    }
    
    /**
     * 创建测试用的RPC请求
     */
    private RpcRequest createTestRequest(Long requestId, String interfaceName, String methodName) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(requestId);
        request.setInterfaceName(interfaceName);
        request.setMethodName(methodName);
        request.setVersion("1.0.0");
        request.setGroup("default");
        return request;
    }
    
    @Test
    public void testGetAlgorithm() {
        assertEquals(LoadBalancer.Algorithm.CONSISTENT_HASH, loadBalancer.getAlgorithm());
    }
    
    @Test
    public void testSelectWithNullServiceInfos() {
        ServiceInfo result = loadBalancer.select(null, createTestRequest(1L, "test", "method"));
        assertNull("空服务列表应返回null", result);
    }
    
    @Test
    public void testSelectWithEmptyServiceInfos() {
        ServiceInfo result = loadBalancer.select(new ArrayList<>(), createTestRequest(1L, "test", "method"));
        assertNull("空服务列表应返回null", result);
    }
    
    @Test
    public void testSelectWithInactiveServices() {
        // 将所有服务设置为非活跃状态
        serviceInfos.forEach(service -> service.setStatus(ServiceInfo.ServiceStatus.INACTIVE));
        
        ServiceInfo result = loadBalancer.select(serviceInfos, createTestRequest(1L, "test", "method"));
        assertNull("所有服务都非活跃时应返回null", result);
    }
    
    @Test
    public void testSelectWithSingleService() {
        List<ServiceInfo> singleService = Arrays.asList(serviceInfos.get(0));
        ServiceInfo result = loadBalancer.select(singleService, createTestRequest(1L, "test", "method"));
        
        assertNotNull("单个服务应能正常选择", result);
        assertEquals("应选择唯一的服务实例", serviceInfos.get(0).getFullAddress(), result.getFullAddress());
    }
    
    @Test
    public void testSelectConsistency() {
        RpcRequest request = createTestRequest(12345L, "com.rpc.example.UserService", "getUser");
        
        // 多次选择同一个请求，应该返回相同的服务实例
        ServiceInfo first = loadBalancer.select(serviceInfos, request);
        ServiceInfo second = loadBalancer.select(serviceInfos, request);
        ServiceInfo third = loadBalancer.select(serviceInfos, request);
        
        assertNotNull("选择结果不应为null", first);
        assertEquals("相同请求应选择相同的服务实例", first.getFullAddress(), second.getFullAddress());
        assertEquals("相同请求应选择相同的服务实例", first.getFullAddress(), third.getFullAddress());
    }
    
    @Test
    public void testSelectDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        int totalRequests = 10000;
        
        // 生成大量不同的请求，统计分布情况
        for (int i = 0; i < totalRequests; i++) {
            RpcRequest request = createTestRequest((long) i, "com.rpc.example.UserService", "getUser");
            ServiceInfo selected = loadBalancer.select(serviceInfos, request);
            
            assertNotNull("选择结果不应为null", selected);
            
            String address = selected.getFullAddress();
            distribution.put(address, distribution.getOrDefault(address, 0) + 1);
        }
        
        // 验证所有服务实例都被选择过
        assertEquals("应该选择到所有服务实例", serviceInfos.size(), distribution.size());
        // 验证分布的相对均匀性（每个实例的请求数应在合理范围内）
        int expectedPerInstance = totalRequests / serviceInfos.size();
        int tolerance = expectedPerInstance / 2; // 50%的容忍度
        
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            int count = entry.getValue();
            assertTrue(String.format("服务实例 %s 的请求分布不均匀，期望约 %d，实际 %d", 
                    entry.getKey(), expectedPerInstance, count),
                    Math.abs(count - expectedPerInstance) <= tolerance);
        }
        
        System.out.println("请求分布统计：");
        distribution.forEach((address, count) -> 
            System.out.printf("  %s: %d (%.2f%%)%n", address, count, count * 100.0 / totalRequests));
    }
    
    @Test
    public void testServiceListChange() {
        RpcRequest request = createTestRequest(12345L, "com.rpc.example.UserService", "getUser");
        
        // 初始选择
        ServiceInfo initialSelection = loadBalancer.select(serviceInfos, request);
        assertNotNull("初始选择不应为null", initialSelection);
        
        // 添加新的服务实例
        ServiceInfo newService = new ServiceInfo();
        newService.setServiceName("com.rpc.example.UserService");
        newService.setAddress("192.168.1.10");
        newService.setPort(8090);
        newService.setVersion("1.0.0");
        newService.setGroup("default");
        newService.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
        newService.setWeight(1);
        
        List<ServiceInfo> expandedServices = new ArrayList<>(serviceInfos);
        expandedServices.add(newService);
        
        // 使用扩展后的服务列表进行选择
        ServiceInfo newSelection = loadBalancer.select(expandedServices, request);
        assertNotNull("扩展后的选择不应为null", newSelection);
        
        // 验证虚拟节点环已重建
        assertTrue("虚拟节点环大小应该增加", 
                loadBalancer.getCurrentVirtualNodeRingSize() > serviceInfos.size() * loadBalancer.getVirtualNodeCount());
    }
    
    @Test
    public void testVirtualNodeCount() {
        ConsistentHashLoadBalancer customLoadBalancer = new ConsistentHashLoadBalancer(200);
        assertEquals("虚拟节点数量应该正确设置", 200, customLoadBalancer.getVirtualNodeCount());
        
        // 选择一个服务实例以触发虚拟节点环构建
        RpcRequest request = createTestRequest(1L, "test", "method");
        customLoadBalancer.select(serviceInfos, request);
        
        // 验证虚拟节点环大小
        int expectedRingSize = serviceInfos.size() * 200;
        assertEquals("虚拟节点环大小应该正确", expectedRingSize, customLoadBalancer.getCurrentVirtualNodeRingSize());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidVirtualNodeCount() {
        new ConsistentHashLoadBalancer(0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeVirtualNodeCount() {
        new ConsistentHashLoadBalancer(-1);
    }
    
    @Test
    public void testClearVirtualNodeRing() {
        // 先进行一次选择以构建虚拟节点环
        RpcRequest request = createTestRequest(1L, "test", "method");
        loadBalancer.select(serviceInfos, request);
        
        assertTrue("虚拟节点环应该不为空", loadBalancer.getCurrentVirtualNodeRingSize() > 0);
        
        // 清空虚拟节点环
        loadBalancer.clearVirtualNodeRing();
        assertEquals("虚拟节点环应该为空", 0, loadBalancer.getCurrentVirtualNodeRingSize());
    }
    
    @Test
    public void testHashKeyGeneration() {
        // 测试不同类型的请求生成不同的哈希键
        RpcRequest request1 = createTestRequest(1L, "service1", "method1");
        RpcRequest request2 = createTestRequest(2L, "service1", "method1");
        RpcRequest request3 = createTestRequest(1L, "service2", "method1");
        RpcRequest request4 = createTestRequest(1L, "service1", "method2");
        
        ServiceInfo result1 = loadBalancer.select(serviceInfos, request1);
        ServiceInfo result2 = loadBalancer.select(serviceInfos, request2);
        ServiceInfo result3 = loadBalancer.select(serviceInfos, request3);
        ServiceInfo result4 = loadBalancer.select(serviceInfos, request4);
        
        assertNotNull("所有选择结果都不应为null", result1);
        assertNotNull("所有选择结果都不应为null", result2);
        assertNotNull("所有选择结果都不应为null", result3);
        assertNotNull("所有选择结果都不应为null", result4);
        
        // 相同请求ID的请求应该选择相同的服务实例
        ServiceInfo result1Again = loadBalancer.select(serviceInfos, request1);
        assertEquals("相同请求应选择相同服务实例", result1.getFullAddress(), result1Again.getFullAddress());
    }
    
    @Test
    public void testToString() {
        String str = loadBalancer.toString();
        assertNotNull("toString不应返回null", str);
        assertTrue("toString应包含算法名称", str.contains("ConsistentHashLoadBalancer"));
        assertTrue("toString应包含虚拟节点数量", str.contains("virtualNodeCount"));
    }
    
    @Test
    public void testMonotonicity() {
        // 测试一致性哈希的单调性：添加节点时，只有部分请求会重新映射
        int totalRequests = 1000;
        Map<Long, ServiceInfo> originalMapping = new HashMap<>();
        
        // 记录原始映射
        for (long i = 0; i < totalRequests; i++) {
            RpcRequest request = createTestRequest(i, "com.rpc.example.UserService", "getUser");
            ServiceInfo selected = loadBalancer.select(serviceInfos, request);
            originalMapping.put(i, selected);
        }
        
        // 添加新节点
        ServiceInfo newService = new ServiceInfo();
        newService.setServiceName("com.rpc.example.UserService");
        newService.setAddress("192.168.1.100");
        newService.setPort(8100);
        newService.setVersion("1.0.0");
        newService.setGroup("default");
        newService.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
        newService.setWeight(1);
        
        List<ServiceInfo> expandedServices = new ArrayList<>(serviceInfos);
        expandedServices.add(newService);
        
        // 记录新映射
        int unchangedCount = 0;
        for (long i = 0; i < totalRequests; i++) {
            RpcRequest request = createTestRequest(i, "com.rpc.example.UserService", "getUser");
            ServiceInfo newSelected = loadBalancer.select(expandedServices, request);
            
            if (originalMapping.get(i).getFullAddress().equals(newSelected.getFullAddress())) {
                unchangedCount++;
            }
        }
        
        // 验证大部分请求的映射没有改变（一致性哈希的优势）
        double unchangedRatio = (double) unchangedCount / totalRequests;
        assertTrue(String.format("添加节点后应该有大部分请求映射不变，实际不变比例：%.2f%%", unchangedRatio * 100),
                unchangedRatio > 0.7); // 至少70%的请求映射不变
        
        System.out.printf("添加节点后映射不变的请求比例：%.2f%% (%d/%d)%n", 
                unchangedRatio * 100, unchangedCount, totalRequests);
    }
}