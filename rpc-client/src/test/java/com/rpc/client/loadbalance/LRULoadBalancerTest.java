package com.rpc.client.loadbalance;

import com.rpc.client.loadbalance.lru.LRULoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * LRU负载均衡器测试类
 * 
 * @author 何杰
 * @version 1.0
 */
public class LRULoadBalancerTest {

    private LRULoadBalancer loadBalancer;
    private List<ServiceInfo> serviceInfos;
    
    @Before
    public void setUp() {
        loadBalancer = new LRULoadBalancer(5); // 设置缓存大小为5
        serviceInfos = createTestServiceInfos();
    }
    
    /**
     * 创建测试用的服务实例列表
     */
    private List<ServiceInfo> createTestServiceInfos() {
        List<ServiceInfo> services = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setServiceName("TestService");
            serviceInfo.setAddress("192.168.1." + i);
            serviceInfo.setPort(8080 + i);
            serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
            services.add(serviceInfo);
        }
        return services;
    }
    
    /**
     * 创建测试用的RPC请求
     */
    private RpcRequest createTestRequest(String interfaceName) {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(interfaceName);
        request.setMethodName("testMethod");
        request.setParameters(new Object[]{"test"});
        request.setParameterTypes(new Class[]{String.class});
        return request;
    }
    
    @Test
    public void testGetAlgorithm() {
        assertEquals("算法名称应该是lru", LoadBalancer.Algorithm.LRU, loadBalancer.getAlgorithm());
    }
    
    @Test
    public void testSelectWithEmptyList() {
        ServiceInfo result = loadBalancer.select(null, createTestRequest("TestService"));
        assertNull("空列表应该返回null", result);
        
        result = loadBalancer.select(new ArrayList<>(), createTestRequest("TestService"));
        assertNull("空列表应该返回null", result);
    }
    
    @Test
    public void testSelectWithSingleService() {
        List<ServiceInfo> singleService = Arrays.asList(serviceInfos.get(0));
        ServiceInfo result = loadBalancer.select(singleService, createTestRequest("TestService"));
        
        assertNotNull("单个服务实例应该被选中", result);
        assertEquals("应该选择唯一的服务实例", serviceInfos.get(0).getFullAddress(), result.getFullAddress());
    }
    
    @Test
    public void testSelectWithInactiveServices() {
        // 将所有服务设置为非活跃状态
        serviceInfos.forEach(service -> service.setStatus(ServiceInfo.ServiceStatus.INACTIVE));
        
        ServiceInfo result = loadBalancer.select(serviceInfos, createTestRequest("TestService"));
        assertNull("所有服务都非活跃时应该返回null", result);
    }
    
    @Test
    public void testLRUBehavior() {
        RpcRequest request = createTestRequest("TestService");
        
        // 第一次选择，应该选择第一个服务（因为都没有被使用过）
        ServiceInfo first = loadBalancer.select(serviceInfos, request);
        assertNotNull("第一次选择应该成功", first);
        
        // 再次选择，应该选择不同的服务（因为第一个已经被使用过了）
        ServiceInfo second = loadBalancer.select(serviceInfos, request);
        assertNotNull("第二次选择应该成功", second);
        
        // 验证缓存中有服务实例
        assertTrue("缓存应该包含服务实例", loadBalancer.getCurrentCacheSize() > 0);
    }
    
    @Test
    public void testCacheSize() {
        assertEquals("缓存大小应该是5", 5, loadBalancer.getCacheSize());
        assertEquals("初始缓存应该为空", 0, loadBalancer.getCurrentCacheSize());
        
        // 选择服务后缓存应该增加
        loadBalancer.select(serviceInfos, createTestRequest("TestService"));
        assertTrue("选择服务后缓存应该增加", loadBalancer.getCurrentCacheSize() > 0);
    }
    
    @Test
    public void testCacheSizeLimit() {
        // 创建一个缓存大小为2的负载均衡器
        LRULoadBalancer smallCacheBalancer = new LRULoadBalancer(2);
        
        // 创建更多的服务实例
        List<ServiceInfo> moreServices = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setServiceName("TestService");
            serviceInfo.setAddress("192.168.1." + i);
            serviceInfo.setPort(8080 + i);
            serviceInfo.setStatus(ServiceInfo.ServiceStatus.ACTIVE);
            moreServices.add(serviceInfo);
        }
        
        RpcRequest request = createTestRequest("TestService");
//        Collections.shuffle(moreServices);
        // 多次选择服务
        for (int i = 0; i < 5; i++) {
            smallCacheBalancer.select(moreServices, request);
            smallCacheBalancer.getCachedServices().forEach(service -> System.out.println("缓存的服务实例: " + service));
        }
        // 缓存大小不应该超过限制
        assertTrue("缓存大小不应该超过限制", smallCacheBalancer.getCurrentCacheSize() <= 2);
    }
    
    @Test
    public void testClearCache() {
        // 先选择一些服务
        RpcRequest request = createTestRequest("TestService");
        loadBalancer.select(serviceInfos, request);
        loadBalancer.select(serviceInfos, request);
        
        assertTrue("缓存应该包含服务实例", loadBalancer.getCurrentCacheSize() > 0);
        
        // 清空缓存
        loadBalancer.clearCache();
        assertEquals("清空后缓存应该为空", 0, loadBalancer.getCurrentCacheSize());
    }
    
    @Test
    public void testGetCachedServices() {
        RpcRequest request = createTestRequest("TestService");
        
        // 选择一些服务
        ServiceInfo first = loadBalancer.select(serviceInfos, request);
        ServiceInfo second = loadBalancer.select(serviceInfos, request);
        
        List<String> cachedServices = loadBalancer.getCachedServices();
        assertNotNull("缓存的服务列表不应该为null", cachedServices);
        assertTrue("缓存应该包含选择过的服务", cachedServices.size() > 0);
    }
    
    @Test
    public void testServiceListChange() {
        RpcRequest request = createTestRequest("TestService");
        
        // 使用原始服务列表进行多次选择，确保缓存中有多个服务
        for (int i = 0; i < serviceInfos.size(); i++) {
            loadBalancer.select(serviceInfos, request);
        }
        int originalCacheSize = loadBalancer.getCurrentCacheSize();
        assertTrue("原始缓存应该包含服务", originalCacheSize > 0);
        
        // 创建新的服务列表（只保留前2个服务）
        List<ServiceInfo> newServiceInfos = new ArrayList<>(serviceInfos.subList(0, 2));
        loadBalancer.select(newServiceInfos, request);
        
        int newCacheSize = loadBalancer.getCurrentCacheSize();
        
        // 缓存应该被更新（移除不再活跃的服务），新缓存大小应该不超过新服务列表大小
        assertTrue("服务列表变化后缓存应该被更新", 
                newCacheSize <= Math.min(originalCacheSize, newServiceInfos.size()));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int requestsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        RpcRequest request = createTestRequest("TestService" + j);
                        ServiceInfo result = loadBalancer.select(serviceInfos, request);
                        if (result != null) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertTrue("并发访问应该成功", successCount.get() > 0);
        assertTrue("缓存大小应该在合理范围内", loadBalancer.getCurrentCacheSize() <= loadBalancer.getCacheSize());
    }
    
    @Test
    public void testLRUOrdering() {
        // 创建一个缓存大小为3的负载均衡器
        LRULoadBalancer testBalancer = new LRULoadBalancer(3);
        RpcRequest request = createTestRequest("TestService");
        
        // 依次选择所有服务实例
        ServiceInfo service1 = testBalancer.select(Arrays.asList(serviceInfos.get(0)), request);
        ServiceInfo service2 = testBalancer.select(Arrays.asList(serviceInfos.get(1)), request);
        ServiceInfo service3 = testBalancer.select(Arrays.asList(serviceInfos.get(2)), request);
        System.out.println("缓存顺序之前：" + testBalancer.getCachedServices());
        assertEquals("缓存应该包含3个服务", 3, testBalancer.getCurrentCacheSize());
        
        // 再次访问第一个服务，它应该被移到最近使用的位置
        testBalancer.select(Arrays.asList(serviceInfos.get(0)), request);
        
        List<String> cachedServices = testBalancer.getCachedServices();
        System.out.println("缓存顺序之后：" + cachedServices);
        assertEquals("缓存仍应该包含3个服务", 3, cachedServices.size());
    }
    
    @Test
    public void testToString() {
        String result = loadBalancer.toString();
        assertNotNull("toString不应该返回null", result);
        assertTrue("toString应该包含算法名称", result.contains("LRU"));
        assertTrue("toString应该包含缓存大小", result.contains("cacheSize"));
    }
    
    @Test
    public void testConstructorValidation() {
        try {
            new LRULoadBalancer(0);
            fail("缓存大小为0应该抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue("异常消息应该包含相关信息", e.getMessage().contains("大于0"));
        }
        
        try {
            new LRULoadBalancer(-1);
            fail("缓存大小为负数应该抛出异常");
        } catch (IllegalArgumentException e) {
            assertTrue("异常消息应该包含相关信息", e.getMessage().contains("大于0"));
        }
    }
    
    @Test
    public void testDefaultConstructor() {
        LRULoadBalancer defaultBalancer = new LRULoadBalancer();
        assertTrue("默认缓存大小应该大于0", defaultBalancer.getCacheSize() > 0);
        assertEquals("默认缓存大小应该是100", 100, defaultBalancer.getCacheSize());
    }
    
    @Test
    public void testLRUSelectionPattern() {
        // 创建一个缓存大小为2的负载均衡器
        LRULoadBalancer testBalancer = new LRULoadBalancer(2);
        RpcRequest request = createTestRequest("TestService");
        
        // 第一次选择，应该选择第一个未使用的服务
        ServiceInfo first = testBalancer.select(serviceInfos, request);
        assertNotNull("第一次选择应该成功", first);
        
        // 第二次选择，应该选择另一个未使用的服务
        ServiceInfo second = testBalancer.select(serviceInfos, request);
        assertNotNull("第二次选择应该成功", second);
        assertNotEquals("第二次选择应该选择不同的服务", first.getFullAddress(), second.getFullAddress());
        
        // 第三次选择，应该选择第三个未使用的服务
        ServiceInfo third = testBalancer.select(serviceInfos, request);
        assertNotNull("第三次选择应该成功", third);
        assertNotEquals("第三次选择应该选择不同的服务", first.getFullAddress(), third.getFullAddress());
        assertNotEquals("第三次选择应该选择不同的服务", second.getFullAddress(), third.getFullAddress());
    }
}