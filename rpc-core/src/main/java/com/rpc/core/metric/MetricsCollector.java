package com.rpc.core.metric;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * 监控指标收集器
 * 用于统计RPC框架的各种监控指标
 *
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class MetricsCollector {
    /** 单例实例 */
    private static final MetricsCollector INSTANCE = new MetricsCollector();
    
    /** 服务注册次数 */
    private final LongAdder serviceRegistrationCount = new LongAdder();
    /** 服务注销次数 */
    private final LongAdder serviceUnregistrationCount = new LongAdder();
    /** 服务发现次数 */
    private final LongAdder serviceDiscoveryCount = new LongAdder();
    
    /** RPC请求总数 */
    private final LongAdder totalRequestCount = new LongAdder();
    /** RPC请求成功数 */
    private final LongAdder successRequestCount = new LongAdder();
    /** RPC请求失败数 */
    private final LongAdder failedRequestCount = new LongAdder();
    
    /** 健康检查总数 */
    private final LongAdder healthCheckCount = new LongAdder();
    /** 健康检查成功数 */
    private final LongAdder healthCheckSuccessCount = new LongAdder();
    /** 健康检查失败数 */
    private final LongAdder healthCheckFailedCount = new LongAdder();
    
    /** 连接创建次数 */
    private final LongAdder connectionCreatedCount = new LongAdder();
    /** 连接关闭次数 */
    private final LongAdder connectionClosedCount = new LongAdder();
    /** 当前活跃连接数 */
    private final AtomicLong activeConnectionCount = new AtomicLong(0);
    
    /** 按服务名统计的请求次数 */
    private final Map<String, LongAdder> serviceRequestCounts = new ConcurrentHashMap<>();
    /** 按服务名统计的成功次数 */
    private final Map<String, LongAdder> serviceSuccessCounts = new ConcurrentHashMap<>();
    /** 按服务名统计的失败次数 */
    private final Map<String, LongAdder> serviceFailedCounts = new ConcurrentHashMap<>();
    
    /** 请求响应时间统计 */
    private final Map<String, ResponseTimeStats> responseTimeStats = new ConcurrentHashMap<>();
    
    private MetricsCollector() {
        // 私有构造函数，单例模式
    }
    
    /**
     * 获取单例实例
     */
    public static MetricsCollector getInstance() {
        return INSTANCE;
    }
    
    // ========== 服务注册相关指标 ==========
    
    /**
     * 记录服务注册
     */
    public void recordServiceRegistration() {
        serviceRegistrationCount.increment();
        log.debug("记录服务注册，当前总数：{}", serviceRegistrationCount.sum());
    }
    
    /**
     * 记录服务注销
     */
    public void recordServiceUnregistration() {
        serviceUnregistrationCount.increment();
        log.debug("记录服务注销，当前总数：{}", serviceUnregistrationCount.sum());
    }
    
    /**
     * 记录服务发现
     */
    public void recordServiceDiscovery() {
        serviceDiscoveryCount.increment();
        log.debug("记录服务发现，当前总数：{}", serviceDiscoveryCount.sum());
    }
    
    // ========== RPC请求相关指标 ==========
    
    /**
     * 记录RPC请求开始
     */
    public void recordRequestStart(String serviceName) {
        totalRequestCount.increment();
        serviceRequestCounts.computeIfAbsent(serviceName, k -> new LongAdder()).increment();
        log.debug("记录RPC请求开始，服务：{}，总请求数：{}", serviceName, totalRequestCount.sum());
    }
    
    /**
     * 记录RPC请求成功
     */
    public void recordRequestSuccess(String serviceName, long responseTimeMs) {
        successRequestCount.increment();
        serviceSuccessCounts.computeIfAbsent(serviceName, k -> new LongAdder()).increment();
        recordResponseTime(serviceName, responseTimeMs);
        log.debug("记录RPC请求成功，服务：{}，响应时间：{}ms，总成功数：{}", serviceName, responseTimeMs, successRequestCount.sum());
    }
    
    /**
     * 记录RPC请求失败
     */
    public void recordRequestFailure(String serviceName, Throwable exception) {
        failedRequestCount.increment();
        serviceFailedCounts.computeIfAbsent(serviceName, k -> new LongAdder()).increment();
        log.debug("记录RPC请求失败，服务：{}，异常：{}，总失败数：{}", serviceName, exception.getClass().getSimpleName(), failedRequestCount.sum());
    }
    
    // ========== 健康检查相关指标 ==========
    
    /**
     * 记录健康检查
     */
    public void recordHealthCheck(boolean success) {
        healthCheckCount.increment();
        if (success) {
            healthCheckSuccessCount.increment();
            log.debug("记录健康检查成功，总检查数：{}，成功数：{}", healthCheckCount.sum(), healthCheckSuccessCount.sum());
        } else {
            healthCheckFailedCount.increment();
            log.debug("记录健康检查失败，总检查数：{}，失败数：{}", healthCheckCount.sum(), healthCheckFailedCount.sum());
        }
    }
    
    // ========== 连接相关指标 ==========
    
    /**
     * 记录连接创建
     */
    public void recordConnectionCreated() {
        connectionCreatedCount.increment();
        activeConnectionCount.incrementAndGet();
        log.debug("记录连接创建，总创建数：{}，当前活跃数：{}", connectionCreatedCount.sum(), activeConnectionCount.get());
    }
    
    /**
     * 记录连接关闭
     */
    public void recordConnectionClosed() {
        connectionClosedCount.increment();
        activeConnectionCount.decrementAndGet();
        log.debug("记录连接关闭，总关闭数：{}，当前活跃数：{}", connectionClosedCount.sum(), activeConnectionCount.get());
    }
    
    // ========== 响应时间统计 ==========
    
    /**
     * 记录响应时间
     */
    private void recordResponseTime(String serviceName, long responseTimeMs) {
        responseTimeStats.computeIfAbsent(serviceName, k -> new ResponseTimeStats()).addResponseTime(responseTimeMs);
    }
    
    // ========== 指标获取方法 ==========
    
    /**
     * 获取服务注册次数
     */
    public long getServiceRegistrationCount() {
        return serviceRegistrationCount.sum();
    }
    
    /**
     * 获取服务注销次数
     */
    public long getServiceUnregistrationCount() {
        return serviceUnregistrationCount.sum();
    }
    
    /**
     * 获取服务发现次数
     */
    public long getServiceDiscoveryCount() {
        return serviceDiscoveryCount.sum();
    }
    
    /**
     * 获取RPC请求总数
     */
    public long getTotalRequestCount() {
        return totalRequestCount.sum();
    }
    
    /**
     * 获取RPC请求成功数
     */
    public long getSuccessRequestCount() {
        return successRequestCount.sum();
    }
    
    /**
     * 获取RPC请求失败数
     */
    public long getFailedRequestCount() {
        return failedRequestCount.sum();
    }
    
    /**
     * 获取RPC请求成功率
     */
    public double getRequestSuccessRate() {
        long total = totalRequestCount.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) successRequestCount.sum() / total;
    }
    
    /**
     * 获取健康检查总数
     */
    public long getHealthCheckCount() {
        return healthCheckCount.sum();
    }
    
    /**
     * 获取健康检查成功数
     */
    public long getHealthCheckSuccessCount() {
        return healthCheckSuccessCount.sum();
    }
    
    /**
     * 获取健康检查失败数
     */
    public long getHealthCheckFailedCount() {
        return healthCheckFailedCount.sum();
    }
    
    /**
     * 获取健康检查成功率
     */
    public double getHealthCheckSuccessRate() {
        long total = healthCheckCount.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) healthCheckSuccessCount.sum() / total;
    }
    
    /**
     * 获取连接创建次数
     */
    public long getConnectionCreatedCount() {
        return connectionCreatedCount.sum();
    }
    
    /**
     * 获取连接关闭次数
     */
    public long getConnectionClosedCount() {
        return connectionClosedCount.sum();
    }
    
    /**
     * 获取当前活跃连接数
     */
    public long getActiveConnectionCount() {
        return activeConnectionCount.get();
    }
    
    /**
     * 获取指定服务的请求次数
     */
    public long getServiceRequestCount(String serviceName) {
        LongAdder counter = serviceRequestCounts.get(serviceName);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * 获取指定服务的成功次数
     */
    public long getServiceSuccessCount(String serviceName) {
        LongAdder counter = serviceSuccessCounts.get(serviceName);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * 获取指定服务的失败次数
     */
    public long getServiceFailedCount(String serviceName) {
        LongAdder counter = serviceFailedCounts.get(serviceName);
        return counter != null ? counter.sum() : 0;
    }
    
    /**
     * 获取指定服务的成功率
     */
    public double getServiceSuccessRate(String serviceName) {
        long total = getServiceRequestCount(serviceName);
        if (total == 0) {
            return 0.0;
        }
        return (double) getServiceSuccessCount(serviceName) / total;
    }
    
    /**
     * 获取指定服务的响应时间统计
     */
    public ResponseTimeStats getServiceResponseTimeStats(String serviceName) {
        return responseTimeStats.get(serviceName);
    }
    
    /**
     * 获取所有监控指标的摘要
     */
    public MetricsSummary getMetricsSummary() {
        return new MetricsSummary(
                getServiceRegistrationCount(),
                getServiceUnregistrationCount(),
                getServiceDiscoveryCount(),
                getTotalRequestCount(),
                getSuccessRequestCount(),
                getFailedRequestCount(),
                getRequestSuccessRate(),
                getHealthCheckCount(),
                getHealthCheckSuccessCount(),
                getHealthCheckFailedCount(),
                getHealthCheckSuccessRate(),
                getConnectionCreatedCount(),
                getConnectionClosedCount(),
                getActiveConnectionCount()
        );
    }
    
    /**
     * 响应时间统计类
     */
    public static class ResponseTimeStats {
        private final LongAdder totalResponseTime = new LongAdder();
        private final LongAdder requestCount = new LongAdder();
        private volatile long minResponseTime = Long.MAX_VALUE;
        private volatile long maxResponseTime = Long.MIN_VALUE;
        
        public synchronized void addResponseTime(long responseTimeMs) {
            totalResponseTime.add(responseTimeMs);
            requestCount.increment();
            
            if (responseTimeMs < minResponseTime) {
                minResponseTime = responseTimeMs;
            }
            if (responseTimeMs > maxResponseTime) {
                maxResponseTime = responseTimeMs;
            }
        }
        
        public double getAverageResponseTime() {
            long count = requestCount.sum();
            if (count == 0) {
                return 0.0;
            }
            return (double) totalResponseTime.sum() / count;
        }
        
        public long getMinResponseTime() {
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime == Long.MIN_VALUE ? 0 : maxResponseTime;
        }
        
        public long getRequestCount() {
            return requestCount.sum();
        }
        
        @Override
        public String toString() {
            return String.format("ResponseTimeStats{avg=%.2fms, min=%dms, max=%dms, count=%d}",
                    getAverageResponseTime(), getMinResponseTime(), getMaxResponseTime(), getRequestCount());
        }
    }
    
    /**
     * 监控指标摘要类
     */
    public static class MetricsSummary {
        private final long serviceRegistrationCount;
        private final long serviceUnregistrationCount;
        private final long serviceDiscoveryCount;
        private final long totalRequestCount;
        private final long successRequestCount;
        private final long failedRequestCount;
        private final double requestSuccessRate;
        private final long healthCheckCount;
        private final long healthCheckSuccessCount;
        private final long healthCheckFailedCount;
        private final double healthCheckSuccessRate;
        private final long connectionCreatedCount;
        private final long connectionClosedCount;
        private final long activeConnectionCount;
        
        public MetricsSummary(long serviceRegistrationCount, long serviceUnregistrationCount,
                             long serviceDiscoveryCount, long totalRequestCount, long successRequestCount,
                             long failedRequestCount, double requestSuccessRate, long healthCheckCount,
                             long healthCheckSuccessCount, long healthCheckFailedCount,
                             double healthCheckSuccessRate, long connectionCreatedCount,
                             long connectionClosedCount, long activeConnectionCount) {
            this.serviceRegistrationCount = serviceRegistrationCount;
            this.serviceUnregistrationCount = serviceUnregistrationCount;
            this.serviceDiscoveryCount = serviceDiscoveryCount;
            this.totalRequestCount = totalRequestCount;
            this.successRequestCount = successRequestCount;
            this.failedRequestCount = failedRequestCount;
            this.requestSuccessRate = requestSuccessRate;
            this.healthCheckCount = healthCheckCount;
            this.healthCheckSuccessCount = healthCheckSuccessCount;
            this.healthCheckFailedCount = healthCheckFailedCount;
            this.healthCheckSuccessRate = healthCheckSuccessRate;
            this.connectionCreatedCount = connectionCreatedCount;
            this.connectionClosedCount = connectionClosedCount;
            this.activeConnectionCount = activeConnectionCount;
        }
        
        // Getters
        public long getServiceRegistrationCount() { return serviceRegistrationCount; }
        public long getServiceUnregistrationCount() { return serviceUnregistrationCount; }
        public long getServiceDiscoveryCount() { return serviceDiscoveryCount; }
        public long getTotalRequestCount() { return totalRequestCount; }
        public long getSuccessRequestCount() { return successRequestCount; }
        public long getFailedRequestCount() { return failedRequestCount; }
        public double getRequestSuccessRate() { return requestSuccessRate; }
        public long getHealthCheckCount() { return healthCheckCount; }
        public long getHealthCheckSuccessCount() { return healthCheckSuccessCount; }
        public long getHealthCheckFailedCount() { return healthCheckFailedCount; }
        public double getHealthCheckSuccessRate() { return healthCheckSuccessRate; }
        public long getConnectionCreatedCount() { return connectionCreatedCount; }
        public long getConnectionClosedCount() { return connectionClosedCount; }
        public long getActiveConnectionCount() { return activeConnectionCount; }
        
        @Override
        public String toString() {
            return String.format(
                    "MetricsSummary{\n" +
                    "  服务注册: %d, 服务注销: %d, 服务发现: %d\n" +
                    "  RPC请求: 总数=%d, 成功=%d, 失败=%d, 成功率=%.2f%%\n" +
                    "  健康检查: 总数=%d, 成功=%d, 失败=%d, 成功率=%.2f%%\n" +
                    "  连接: 创建=%d, 关闭=%d, 活跃=%d\n" +
                    "}",
                    serviceRegistrationCount, serviceUnregistrationCount, serviceDiscoveryCount,
                    totalRequestCount, successRequestCount, failedRequestCount, requestSuccessRate * 100,
                    healthCheckCount, healthCheckSuccessCount, healthCheckFailedCount, healthCheckSuccessRate * 100,
                    connectionCreatedCount, connectionClosedCount, activeConnectionCount
            );
        }
    }
}