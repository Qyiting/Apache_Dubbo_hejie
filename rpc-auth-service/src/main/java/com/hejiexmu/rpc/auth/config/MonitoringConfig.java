package com.hejiexmu.rpc.auth.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
// import org.springframework.boot.actuator.health.Health;
// import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

/**
 * 性能监控配置类
 * 配置应用性能指标监控
 * 
 * @author hejiexmu
 */
@Configuration
public class MonitoringConfig {
    
    /**
     * 性能指标收集器
     */
    @Component
    public static class PerformanceMetricsCollector {
        
        private final MeterRegistry meterRegistry;
        private final AtomicLong activeUsers = new AtomicLong(0);
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successfulRequests = new AtomicLong(0);
        private final AtomicLong failedRequests = new AtomicLong(0);
        private final AtomicInteger currentConnections = new AtomicInteger(0);
        
        // 计时器
        private final Timer loginTimer;
        private final Timer databaseQueryTimer;
        private final Timer cacheOperationTimer;
        
        // 计数器
        private final Counter loginAttempts;
        private final Counter loginSuccesses;
        private final Counter loginFailures;
        private final Counter cacheHits;
        private final Counter cacheMisses;
        
        public PerformanceMetricsCollector(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            
            // 注册计时器
            this.loginTimer = Timer.builder("auth.login.duration")
                    .description("Time taken for login operations")
                    .register(meterRegistry);
            
            this.databaseQueryTimer = Timer.builder("database.query.duration")
                    .description("Time taken for database queries")
                    .register(meterRegistry);
            
            this.cacheOperationTimer = Timer.builder("cache.operation.duration")
                    .description("Time taken for cache operations")
                    .register(meterRegistry);
            
            // 注册计数器
            this.loginAttempts = Counter.builder("auth.login.attempts")
                    .description("Total login attempts")
                    .register(meterRegistry);
            
            this.loginSuccesses = Counter.builder("auth.login.successes")
                    .description("Successful login attempts")
                    .register(meterRegistry);
            
            this.loginFailures = Counter.builder("auth.login.failures")
                    .description("Failed login attempts")
                    .register(meterRegistry);
            
            this.cacheHits = Counter.builder("cache.hits")
                    .description("Cache hit count")
                    .register(meterRegistry);
            
            this.cacheMisses = Counter.builder("cache.misses")
                    .description("Cache miss count")
                    .register(meterRegistry);
            
            // 注册仪表
            Gauge.builder("auth.active.users", this, PerformanceMetricsCollector::getActiveUsers)
                    .description("Number of active users")
                    .register(meterRegistry);
            
            Gauge.builder("auth.current.connections", this, PerformanceMetricsCollector::getCurrentConnections)
                    .description("Current number of connections")
                    .register(meterRegistry);
            
            Gauge.builder("jvm.memory.used", this, PerformanceMetricsCollector::getUsedMemory)
                    .description("Used JVM memory")
                    .register(meterRegistry);
            
            Gauge.builder("jvm.threads.count", this, PerformanceMetricsCollector::getThreadCount)
                    .description("Number of JVM threads")
                    .register(meterRegistry);
        }
        
        // 指标记录方法
        public void recordLoginAttempt() {
            loginAttempts.increment();
            totalRequests.incrementAndGet();
        }
        
        public void recordLoginSuccess() {
            loginSuccesses.increment();
            successfulRequests.incrementAndGet();
        }
        
        public void recordLoginFailure() {
            loginFailures.increment();
            failedRequests.incrementAndGet();
        }
        
        public void recordCacheHit() {
            cacheHits.increment();
        }
        
        public void recordCacheMiss() {
            cacheMisses.increment();
        }
        
        public Timer.Sample startLoginTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void stopLoginTimer(Timer.Sample sample) {
            sample.stop(loginTimer);
        }
        
        public Timer.Sample startDatabaseTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void stopDatabaseTimer(Timer.Sample sample) {
            sample.stop(databaseQueryTimer);
        }
        
        public Timer.Sample startCacheTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void stopCacheTimer(Timer.Sample sample) {
            sample.stop(cacheOperationTimer);
        }
        
        public void incrementActiveUsers() {
            activeUsers.incrementAndGet();
        }
        
        public void decrementActiveUsers() {
            activeUsers.decrementAndGet();
        }
        
        public void incrementConnections() {
            currentConnections.incrementAndGet();
        }
        
        public void decrementConnections() {
            currentConnections.decrementAndGet();
        }
        
        // 获取指标值的方法
        public long getActiveUsers() {
            return activeUsers.get();
        }
        
        public int getCurrentConnections() {
            return currentConnections.get();
        }
        
        public long getTotalRequests() {
            return totalRequests.get();
        }
        
        public long getSuccessfulRequests() {
            return successfulRequests.get();
        }
        
        public long getFailedRequests() {
            return failedRequests.get();
        }
        
        public double getSuccessRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total * 100 : 0;
        }
        
        public long getUsedMemory() {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            return memoryBean.getHeapMemoryUsage().getUsed();
        }
        
        public int getThreadCount() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            return threadBean.getThreadCount();
        }
        
        /**
         * 定时收集系统指标
         */
        @Scheduled(fixedRate = 60000) // 每分钟收集一次
        public void collectSystemMetrics() {
            // 收集JVM指标
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            
            Gauge.builder("jvm.memory.usage.percent", () -> maxMemory > 0 ? (double) usedMemory / maxMemory * 100 : 0)
                    .description("JVM memory usage percentage")
                    .register(meterRegistry);
            
            // 收集GC指标
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                Gauge.builder("jvm.gc.collections", gcBean, GarbageCollectorMXBean::getCollectionCount)
                        .tag("gc", gcBean.getName())
                        .description("GC collection count")
                        .register(meterRegistry);
                
                Gauge.builder("jvm.gc.time", gcBean, GarbageCollectorMXBean::getCollectionTime)
                        .tag("gc", gcBean.getName())
                        .description("GC collection time")
                        .register(meterRegistry);
            }
        }
    }
    
    /**
     * 数据库健康检查器 - 暂时注释掉，因为它依赖于缺失的Health相关类
     */
    /*
    @Component
    public static class DatabaseHealthIndicator implements HealthIndicator {
        
        private final DataSource masterDataSource;
        private final DataSource slaveDataSource;
        
        public DatabaseHealthIndicator(DataSource masterDataSource, DataSource slaveDataSource) {
            this.masterDataSource = masterDataSource;
            this.slaveDataSource = slaveDataSource;
        }
        
        @Override
        public Health health() {
            try {
                // 检查主数据源
                boolean masterHealthy = checkDataSource(masterDataSource, "master");
                // 检查从数据源
                boolean slaveHealthy = checkDataSource(slaveDataSource, "slave");
                
                if (masterHealthy && slaveHealthy) {
                    return Health.up()
                            .withDetail("master", "UP")
                            .withDetail("slave", "UP")
                            .withDetail("database", "All databases are healthy")
                            .build();
                } else if (masterHealthy) {
                    return Health.up()
                            .withDetail("master", "UP")
                            .withDetail("slave", "DOWN")
                            .withDetail("database", "Master database is healthy, slave is down")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("master", masterHealthy ? "UP" : "DOWN")
                            .withDetail("slave", slaveHealthy ? "UP" : "DOWN")
                            .withDetail("database", "Database connection issues")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("database", "Health check failed")
                        .build();
            }
        }
        
        private boolean checkDataSource(DataSource dataSource, String name) {
            try (Connection connection = dataSource.getConnection()) {
                return connection.isValid(5);
            } catch (SQLException e) {
                return false;
            }
        }
    }
    */
    
    @Bean
    public PerformanceMetricsCollector performanceMetricsCollector(MeterRegistry meterRegistry) {
        return new PerformanceMetricsCollector(meterRegistry);
    }
}