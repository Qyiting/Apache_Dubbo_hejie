package com.rpc.client.loadbalance;

import com.rpc.client.loadbalance.lfu.LFULoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * LFU负载均衡器测试类
 * 
 * @author 何杰
 * @version 1.0
 */
public class LFULoadBalancerTest {

    private LFULoadBalancer loadBalancer;
    private List<ServiceInfo> serviceInfos;
    private RpcRequest rpcRequest;

    @Before
    public void setUp() {
        loadBalancer = new LFULoadBalancer(5); // 最大跟踪5个服务
        serviceInfos = createTestServices(3);
        rpcRequest = createTestRequest();
    }

    @Test
    public void testBasicServiceSelection() {
        ServiceInfo selected = loadBalancer.select(serviceInfos, rpcRequest);
        
        assertNotNull("应该选择一个服务实例", selected);
        assertTrue("选择的服务应该在服务列表中", serviceInfos.contains(selected));
        assertEquals("选择的服务应该是活跃状态", ServiceInfo.ServiceStatus.ACTIVE, selected.getStatus());
    }

    @Test
    public void testEmptyServiceList() {
        ServiceInfo selected = loadBalancer.select(Collections.emptyList(), rpcRequest);
        assertNull("空服务列表应该返回null", selected);
        
        selected = loadBalancer.select(null, rpcRequest);
        assertNull("null服务列表应该返回null", selected);
    }

    @Test
    public void testInactiveServiceFiltering() {
        // 创建包含非活跃服务的列表
        List<ServiceInfo> mixedServices = new ArrayList<>();
        mixedServices.add(createServiceInfo("192.168.1.1", 8001, ServiceInfo.ServiceStatus.ACTIVE));
        mixedServices.add(createServiceInfo("192.168.1.2", 8002, ServiceInfo.ServiceStatus.INACTIVE));
        mixedServices.add(createServiceInfo("192.168.1.3", 8003, ServiceInfo.ServiceStatus.ACTIVE));
        
        ServiceInfo selected = loadBalancer.select(mixedServices, rpcRequest);
        
        assertNotNull("应该选择一个活跃的服务实例", selected);
        assertEquals("选择的服务应该是活跃状态", ServiceInfo.ServiceStatus.ACTIVE, selected.getStatus());
    }

    @Test
    public void testLFUFrequencyTracking() {
        ServiceInfo service1 = serviceInfos.get(0);
        ServiceInfo service2 = serviceInfos.get(1);
        
        // 初始频率应该为0
        assertEquals("初始频率应该为0", 0, loadBalancer.getServiceFrequency(service1));
        assertEquals("初始频率应该为0", 0, loadBalancer.getServiceFrequency(service2));
        
        // 选择service1多次
        for (int i = 0; i < 3; i++) {
            loadBalancer.select(Arrays.asList(service1), rpcRequest);
        }
        
        // 选择service2一次
        loadBalancer.select(Arrays.asList(service2), rpcRequest);
        
        assertEquals("service1频率应该为3", 3, loadBalancer.getServiceFrequency(service1));
        assertEquals("service2频率应该为1", 1, loadBalancer.getServiceFrequency(service2));
    }

    @Test
    public void testLFUSelection() {
        ServiceInfo service1 = serviceInfos.get(0);
        ServiceInfo service2 = serviceInfos.get(1);
        ServiceInfo service3 = serviceInfos.get(2);
        
        // 让service1被访问3次
        for (int i = 0; i < 3; i++) {
            loadBalancer.select(Arrays.asList(service1), rpcRequest);
        }
        
        // 让service2被访问2次
        for (int i = 0; i < 2; i++) {
            loadBalancer.select(Arrays.asList(service2), rpcRequest);
        }
        
        // service3未被访问，频率为0
        
        // 现在从所有服务中选择，应该选择频率最低的service3
        ServiceInfo selected = loadBalancer.select(serviceInfos, rpcRequest);
        assertEquals("应该选择频率最低的service3", service3, selected);
        
        // 再次选择，service3频率变为1，仍然是最低的
        selected = loadBalancer.select(serviceInfos, rpcRequest);
        assertEquals("应该继续选择频率最低的service3", service3, selected);
        
        // 让service3频率达到2
        loadBalancer.select(Arrays.asList(service3), rpcRequest);
        
        // 现在service2和service3频率都是2，service1频率是3
        // 应该从频率为2的服务中选择
        selected = loadBalancer.select(serviceInfos, rpcRequest);
        assertTrue("应该选择频率为2的服务（service2或service3）", 
                selected == service2 || selected == service3);
    }

    @Test
    public void testServiceEviction() {
        // 创建超过最大跟踪数量的服务
        List<ServiceInfo> manyServices = createTestServices(7); // 超过最大跟踪数量5
        
        // 访问前5个服务
        for (int i = 0; i < 5; i++) {
            loadBalancer.select(Arrays.asList(manyServices.get(i)), rpcRequest);
        }
        
        assertEquals("应该跟踪5个服务", 5, loadBalancer.getCurrentServiceCount());
        
        // 访问第6个服务，应该触发淘汰
        System.out.println("\n准备访问第6个服务: " + manyServices.get(5).getAddress());
        System.out.println("淘汰前缓存内容: " + loadBalancer.getServiceFrequencies());
        loadBalancer.select(Arrays.asList(manyServices.get(5)), rpcRequest);
        
        assertEquals("淘汰后仍应该跟踪5个服务", 5, loadBalancer.getCurrentServiceCount());
        System.out.println("淘汰后缓存内容: " + loadBalancer.getServiceFrequencies());
        // 访问第7个服务
        System.out.println("\n准备访问第7个服务: " + manyServices.get(6).getAddress());
        System.out.println("淘汰前缓存内容: " + loadBalancer.getServiceFrequencies());
        loadBalancer.select(Arrays.asList(manyServices.get(6)), rpcRequest);
        System.out.println("淘汰后缓存内容: " + loadBalancer.getServiceFrequencies());
        assertEquals("淘汰后仍应该跟踪5个服务", 5, loadBalancer.getCurrentServiceCount());
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        ServiceInfo selected = loadBalancer.select(serviceInfos, rpcRequest);
                        assertNotNull("并发访问时应该能选择到服务", selected);
                        
                        // 随机访问一些统计方法
                        if (j % 10 == 0) {
                            loadBalancer.getCurrentServiceCount();
                            loadBalancer.getMinFrequency();
                            loadBalancer.getServiceFrequencies();
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue("并发测试应该在30秒内完成", latch.await(30, java.util.concurrent.TimeUnit.SECONDS));
        executor.shutdown();
        
        assertTrue("并发访问不应该产生异常: " + exceptions, exceptions.isEmpty());
        
        // 验证频率统计的一致性
        Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();
        int totalFrequency = frequencies.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals("总访问频率应该等于总操作数", 
                threadCount * operationsPerThread, totalFrequency);
    }

    @Test
    public void testServiceListChange() {
        // 初始选择，建立频率统计
        for (int i = 0; i < 5; i++) {
            loadBalancer.select(serviceInfos, rpcRequest);
        }
        
        int originalServiceCount = loadBalancer.getCurrentServiceCount();
        assertTrue("应该有服务被跟踪", originalServiceCount > 0);
        
        // 创建新的服务列表，移除一些服务
        List<ServiceInfo> newServices = Arrays.asList(
                serviceInfos.get(0), // 保留第一个服务
                createServiceInfo("192.168.1.100", 9001, ServiceInfo.ServiceStatus.ACTIVE) // 新服务
        );
        
        // 使用新服务列表进行选择
        ServiceInfo selected = loadBalancer.select(newServices, rpcRequest);
        assertNotNull("使用新服务列表应该能选择到服务", selected);
        
        // 验证不活跃的服务已被清理
        // 由于服务列表变化，一些服务应该被移除
        int newServiceCount = loadBalancer.getCurrentServiceCount();
        assertTrue("服务列表变化后跟踪的服务数量应该减少或保持不变", newServiceCount <= originalServiceCount);
    }

    @Test
    public void testClearFrequencyStats() {
        // 建立一些频率统计
        for (int i = 0; i < 10; i++) {
            loadBalancer.select(serviceInfos, rpcRequest);
        }
        
        assertTrue("清空前应该有服务被跟踪", loadBalancer.getCurrentServiceCount() > 0);
        
        // 清空统计
        loadBalancer.clearFrequencyStats();
        
        assertEquals("清空后不应该有服务被跟踪", 0, loadBalancer.getCurrentServiceCount());
        assertEquals("清空后最小频率应该重置为1", 1, loadBalancer.getMinFrequency());
        assertTrue("清空后频率映射应该为空", loadBalancer.getServiceFrequencies().isEmpty());
    }

    @Test
    public void testAlgorithmName() {
        assertEquals("算法名称应该是lfu", "lfu", loadBalancer.getAlgorithm());
    }

    @Test
    public void testConfiguration() {
        assertEquals("最大服务数量应该是5", 5, loadBalancer.getMaxServices());
        
        // 测试默认构造函数
        LFULoadBalancer defaultBalancer = new LFULoadBalancer();
        assertEquals("默认最大服务数量应该是100", 100, defaultBalancer.getMaxServices());
    }

    @Test
    public void testIllegalArguments() {
        try {
            new LFULoadBalancer(0);
            fail("最大服务数量为0应该抛出异常");
        } catch (IllegalArgumentException e) {
            // 预期的异常
        }
        
        try {
            new LFULoadBalancer(-1);
            fail("最大服务数量为负数应该抛出异常");
        } catch (IllegalArgumentException e) {
            // 预期的异常
        }
    }

    @Test
    public void testToString() {
        String str = loadBalancer.toString();
        assertNotNull("toString不应该返回null", str);
        assertTrue("toString应该包含类名", str.contains("LFULoadBalancer"));
        assertTrue("toString应该包含最大服务数量", str.contains("maxServices=5"));
    }

    @Test
    public void testFrequencyDistribution() {
        // 创建更多服务进行测试
        List<ServiceInfo> moreServices = createTestServices(3);
        
        // 进行大量选择操作
        for (int i = 0; i < 300; i++) {
            loadBalancer.select(moreServices, rpcRequest);
        }
        
        Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();
        
        // 验证所有服务都被访问了
        assertEquals("应该跟踪3个服务", 3, frequencies.size());
        
        // 验证频率总和
        int totalFrequency = frequencies.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals("总频率应该等于总选择次数", 300, totalFrequency);
        
        // 由于LFU算法的特性，频率应该相对均匀
        List<Integer> freqList = new ArrayList<>(frequencies.values());
        Collections.sort(freqList);
        
        // 最高频率和最低频率的差值不应该太大
        int maxFreq = freqList.get(freqList.size() - 1);
        int minFreq = freqList.get(0);
        assertTrue("频率分布应该相对均匀，差值不超过2", maxFreq - minFreq <= 2);
    }

    /**
     * 创建测试用的服务实例列表
     */
    private List<ServiceInfo> createTestServices(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createServiceInfo("192.168.1." + (i + 1), 8000 + i, ServiceInfo.ServiceStatus.ACTIVE))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 创建测试用的服务实例
     */
    private ServiceInfo createServiceInfo(String address, int port, ServiceInfo.ServiceStatus status) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceName("TestService");
        serviceInfo.setAddress(address);
        serviceInfo.setPort(port);
        serviceInfo.setVersion("1.0");
        serviceInfo.setGroup("default");
        serviceInfo.setWeight(1);
        serviceInfo.setStatus(status);
        serviceInfo.setRegisterTime(System.currentTimeMillis());
        serviceInfo.setLastUpdateTime(System.currentTimeMillis());
        return serviceInfo;
    }

    /**
     * 创建测试用的RPC请求
     */
    private RpcRequest createTestRequest() {
        RpcRequest request = new RpcRequest();
        request.setInterfaceName("TestService");
        request.setMethodName("testMethod");
        request.setVersion("1.0");
        request.setGroup("default");
        return request;
    }
}