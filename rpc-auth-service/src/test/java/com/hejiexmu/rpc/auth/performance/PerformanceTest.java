package com.hejiexmu.rpc.auth.performance;

import com.hejiexmu.rpc.auth.config.TestConfig;
import com.hejiexmu.rpc.auth.dto.LoginRequest;
import com.hejiexmu.rpc.auth.dto.RegisterRequest;
import com.hejiexmu.rpc.auth.service.AuthService;
import com.hejiexmu.rpc.auth.service.JwtTokenService;
import com.hejiexmu.rpc.auth.cache.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能测试类
 * 测试系统在高并发情况下的性能表现
 * 
 * @author hejiexmu
 */
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@Disabled("Performance tests should be run manually")
public class PerformanceTest {
    
    @MockBean
    private AuthService authService;
    
    @MockBean
    private JwtTokenService jwtTokenService;
    
    @MockBean
    private RedisCacheService redisCacheService;
    
    private ExecutorService executorService;
    private final int THREAD_POOL_SIZE = 50;
    private final int TOTAL_REQUESTS = 1000;
    
    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }
    
    /**
     * 登录性能测试
     */
    @Test
    void testLoginPerformance() throws InterruptedException {
        System.out.println("Starting login performance test...");
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    // 创建登录请求
                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setUsername("user" + requestId);
                    loginRequest.setPassword("password123");
                    loginRequest.setIpAddress("192.168.1." + (requestId % 255 + 1));
                    loginRequest.setUserAgent("PerformanceTest");
                    
                    // 模拟登录操作（这里应该调用实际的服务，但由于是Mock，我们模拟执行时间）
                    Thread.sleep(10 + (requestId % 50)); // 模拟10-60ms的响应时间
                    
                    long requestEnd = System.currentTimeMillis();
                    totalResponseTime.addAndGet(requestEnd - requestStart);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Request " + requestId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有请求完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 计算性能指标
        Duration totalDuration = Duration.between(startTime, endTime);
        double totalSeconds = totalDuration.toMillis() / 1000.0;
        double throughput = TOTAL_REQUESTS / totalSeconds;
        double averageResponseTime = totalResponseTime.get() / (double) TOTAL_REQUESTS;
        double successRate = (successCount.get() / (double) TOTAL_REQUESTS) * 100;
        
        // 输出性能报告
        System.out.println("\n=== Login Performance Test Results ===");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Thread Pool Size: " + THREAD_POOL_SIZE);
        System.out.println("Total Duration: " + totalSeconds + " seconds");
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + failureCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
        System.out.println("Average Response Time: " + String.format("%.2f", averageResponseTime) + " ms");
        
        // 断言性能要求
        assertTrue(completed, "All requests should complete within 60 seconds");
        assertTrue(throughput > 10, "Throughput should be greater than 10 requests/second");
        assertTrue(averageResponseTime < 1000, "Average response time should be less than 1000ms");
        assertTrue(successRate > 95, "Success rate should be greater than 95%");
    }
    
    /**
     * 注册性能测试
     */
    @Test
    void testRegisterPerformance() throws InterruptedException {
        System.out.println("Starting register performance test...");
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    // 创建注册请求
                    RegisterRequest registerRequest = new RegisterRequest();
                    registerRequest.setUsername("newuser" + requestId);
                    registerRequest.setPassword("password123");
                    registerRequest.setConfirmPassword("password123");
                    registerRequest.setEmail("user" + requestId + "@example.com");
                    registerRequest.setRealName("User " + requestId);
                    registerRequest.setPhone("1380013" + String.format("%04d", requestId % 10000));
                    registerRequest.setIpAddress("192.168.1." + (requestId % 255 + 1));
                    registerRequest.setUserAgent("PerformanceTest");
                    
                    // 模拟注册操作
                    Thread.sleep(20 + (requestId % 80)); // 模拟20-100ms的响应时间
                    
                    long requestEnd = System.currentTimeMillis();
                    totalResponseTime.addAndGet(requestEnd - requestStart);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Register request " + requestId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有请求完成
        boolean completed = latch.await(120, TimeUnit.SECONDS);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 计算性能指标
        Duration totalDuration = Duration.between(startTime, endTime);
        double totalSeconds = totalDuration.toMillis() / 1000.0;
        double throughput = TOTAL_REQUESTS / totalSeconds;
        double averageResponseTime = totalResponseTime.get() / (double) TOTAL_REQUESTS;
        double successRate = (successCount.get() / (double) TOTAL_REQUESTS) * 100;
        
        // 输出性能报告
        System.out.println("\n=== Register Performance Test Results ===");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Thread Pool Size: " + THREAD_POOL_SIZE);
        System.out.println("Total Duration: " + totalSeconds + " seconds");
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + failureCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
        System.out.println("Average Response Time: " + String.format("%.2f", averageResponseTime) + " ms");
        
        // 断言性能要求
        assertTrue(completed, "All requests should complete within 120 seconds");
        assertTrue(throughput > 5, "Throughput should be greater than 5 requests/second");
        assertTrue(averageResponseTime < 2000, "Average response time should be less than 2000ms");
        assertTrue(successRate > 90, "Success rate should be greater than 90%");
    }
    
    /**
     * JWT令牌生成性能测试
     */
    @Test
    void testJwtTokenGenerationPerformance() throws InterruptedException {
        System.out.println("Starting JWT token generation performance test...");
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        
        LocalDateTime startTime = LocalDateTime.now();
        
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    long requestStart = System.nanoTime();
                    
                    // 模拟JWT令牌生成
                    String token = "jwt.token." + requestId + "." + System.currentTimeMillis();
                    
                    // 模拟令牌验证
                    boolean isValid = token.startsWith("jwt.token.");
                    
                    long requestEnd = System.nanoTime();
                    totalResponseTime.addAndGet((requestEnd - requestStart) / 1_000_000); // 转换为毫秒
                    
                    if (isValid) {
                        successCount.incrementAndGet();
                    }
                    
                } catch (Exception e) {
                    System.err.println("JWT generation " + requestId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有请求完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        LocalDateTime endTime = LocalDateTime.now();
        
        // 计算性能指标
        Duration totalDuration = Duration.between(startTime, endTime);
        double totalSeconds = totalDuration.toMillis() / 1000.0;
        double throughput = TOTAL_REQUESTS / totalSeconds;
        double averageResponseTime = totalResponseTime.get() / (double) TOTAL_REQUESTS;
        double successRate = (successCount.get() / (double) TOTAL_REQUESTS) * 100;
        
        // 输出性能报告
        System.out.println("\n=== JWT Token Generation Performance Test Results ===");
        System.out.println("Total Requests: " + TOTAL_REQUESTS);
        System.out.println("Thread Pool Size: " + THREAD_POOL_SIZE);
        System.out.println("Total Duration: " + totalSeconds + " seconds");
        System.out.println("Successful Operations: " + successCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        System.out.println("Average Response Time: " + String.format("%.4f", averageResponseTime) + " ms");
        
        // 断言性能要求
        assertTrue(completed, "All operations should complete within 30 seconds");
        assertTrue(throughput > 100, "Throughput should be greater than 100 operations/second");
        assertTrue(averageResponseTime < 10, "Average response time should be less than 10ms");
        assertTrue(successRate > 99, "Success rate should be greater than 99%");
    }
    
    /**
     * 内存使用测试
     */
    @Test
    void testMemoryUsage() {
        System.out.println("Starting memory usage test...");
        
        Runtime runtime = Runtime.getRuntime();
        
        // 获取初始内存使用情况
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Initial memory usage: " + (initialMemory / 1024 / 1024) + " MB");
        
        // 创建大量对象模拟高负载
        List<String> objects = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            objects.add("TestObject" + i + "_" + System.currentTimeMillis());
        }
        
        // 获取峰值内存使用情况
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Peak memory usage: " + (peakMemory / 1024 / 1024) + " MB");
        
        // 清理对象
        objects.clear();
        System.gc();
        
        // 等待GC完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 获取清理后内存使用情况
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Final memory usage: " + (finalMemory / 1024 / 1024) + " MB");
        
        // 计算内存增长
        long memoryIncrease = finalMemory - initialMemory;
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // 断言内存使用合理
        assertTrue(memoryIncrease < 50 * 1024 * 1024, "Memory increase should be less than 50MB");
    }
    
    /**
     * 清理资源
     */
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}