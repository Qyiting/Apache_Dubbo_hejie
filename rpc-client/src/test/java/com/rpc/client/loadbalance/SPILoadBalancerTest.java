package com.rpc.client.loadbalance;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.core.request.RpcRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.ServiceLoader;

/**
 * SPI负载均衡器机制测试类
 * 测试SPI机制的服务发现和动态加载功能
 * 
 * @author 何杰
 * @version 1.0
 */
@DisplayName("SPI负载均衡器机制测试")
public class SPILoadBalancerTest {
    
    private List<ServiceInfo> testServiceInfos;
    private RpcRequest testRequest;
    
    @BeforeEach
    void setUp() {
        testServiceInfos = createTestServiceInfos();
        testRequest = createTestRpcRequest();
    }
    
    @Test
    @DisplayName("测试SPI服务发现机制")
    void testSPIServiceDiscovery() {
        // 测试通过SPI机制发现所有负载均衡器实现
        ServiceLoader<LoadBalancer> serviceLoader = ServiceLoader.load(LoadBalancer.class);
        
        Set<String> discoveredAlgorithms = new HashSet<>();
        for (LoadBalancer loadBalancer : serviceLoader) {
            discoveredAlgorithms.add(loadBalancer.getAlgorithm().toLowerCase());
        }
        
        // 验证发现的算法数量
        assertFalse(discoveredAlgorithms.isEmpty(), "SPI应该发现至少一个负载均衡器实现");
        
        // 验证预期的算法都被发现
        String[] expectedAlgorithms = {"random", "round_robin", "consistent_hash", "lru", "lfu"};
        for (String algorithm : expectedAlgorithms) {
            assertTrue(discoveredAlgorithms.contains(algorithm), 
                    "SPI应该发现算法: " + algorithm);
        }
        
        System.out.println("通过SPI发现的负载均衡算法: " + discoveredAlgorithms);
    }
    
    @Test
    @DisplayName("测试SPI动态加载机制")
    void testSPIDynamicLoading() {
        // 测试通过LoadBalancerFactory动态加载各种负载均衡器
        String[] algorithms = {"random", "round_robin", "lru", "lfu", "consistent_hash"};
        
        for (String algorithm : algorithms) {
            LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(algorithm);
            
            assertNotNull(loadBalancer, "应该能够动态加载算法: " + algorithm);
            assertEquals(algorithm, loadBalancer.getAlgorithm().toLowerCase(), 
                    "加载的负载均衡器算法名称应该匹配");
            
            // 测试负载均衡器的基本功能
            ServiceInfo selected = loadBalancer.select(testServiceInfos, testRequest);
            assertNotNull(selected, "负载均衡器应该能够选择服务实例: " + algorithm);
            assertTrue(testServiceInfos.contains(selected), "选择的服务实例应该在原列表中");
        }
    }
    
    @Test
    @DisplayName("测试SPI异常处理")
    void testSPIExceptionHandling() {
        // 测试不存在的算法应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalancerFactory.getLoadBalancer("unknown_algorithm");
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
    @DisplayName("测试负载均衡器功能完整性")
    void testLoadBalancerFunctionality() {
        String[] algorithms = {"random", "round_robin", "lru", "lfu", "consistent_hash"};
        
        for (String algorithm : algorithms) {
            LoadBalancer loadBalancer = LoadBalancerFactory.getLoadBalancer(algorithm);
            
            // 测试基本选择功能
            testBasicSelection(loadBalancer, algorithm);
            
            // 测试边界条件
            testBoundaryConditions(loadBalancer, algorithm);
        }
    }
    
    private List<ServiceInfo> createTestServiceInfos() {
        List<ServiceInfo> serviceInfos = new ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setServiceName("TestService" + i);
            serviceInfo.setAddress("192.168.1." + i);
            serviceInfo.setPort(8080 + i);
            serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
            serviceInfos.add(serviceInfo);
        }
        
        return serviceInfos;
    }
    
    private RpcRequest createTestRpcRequest() {
        RpcRequest request = new RpcRequest();
        request.setRequestId(System.currentTimeMillis());
        request.setInterfaceName("com.rpc.test.TestService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameters(new Object[]{"test"});
        return request;
    }
    
    private void testBasicSelection(LoadBalancer loadBalancer, String algorithm) {
        ServiceInfo selected = loadBalancer.select(testServiceInfos, testRequest);
        assertNotNull(selected, algorithm + "负载均衡器应该能够选择服务实例");
        
        // 检查选择的服务实例是否在原列表中（通过服务关键信息比较）
        boolean found = testServiceInfos.stream().anyMatch(service -> 
            service.getServiceName().equals(selected.getServiceName()) &&
            service.getAddress().equals(selected.getAddress()) &&
            service.getPort() == selected.getPort()
        );
        assertTrue(found, "选择的服务实例应该在原列表中: " + selected.getServiceName() + "@" + selected.getAddress() + ":" + selected.getPort());
    }
    
    private void testBoundaryConditions(LoadBalancer loadBalancer, String algorithm) {
        // 测试空列表
        ServiceInfo selected = loadBalancer.select(new ArrayList<>(), testRequest);
        assertNull(selected, algorithm + "负载均衡器在空列表时应返回null");
        
        // 测试单个服务
        List<ServiceInfo> singleService = Arrays.asList(testServiceInfos.get(0));
        selected = loadBalancer.select(singleService, testRequest);
        assertEquals(testServiceInfos.get(0), selected, 
                algorithm + "负载均衡器在单个服务时应返回该服务");
    }
}