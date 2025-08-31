package com.hejiexmu.rpc.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * 负载均衡配置类
 * 实现数据库读写分离的负载均衡策略
 * 
 * @author hejiexmu
 */
@Configuration
@EnableScheduling
public class LoadBalanceConfig {
    
    private static final Logger logger = Logger.getLogger(LoadBalanceConfig.class.getName());
    
    /**
     * 数据源健康检查器
     */
    @Component
    public static class DataSourceHealthChecker {
        
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final List<DataSourceHealth> masterDataSources = new ArrayList<>();
        private final List<DataSourceHealth> slaveDataSources = new ArrayList<>();
        
        /**
         * 数据源健康状态
         */
        public static class DataSourceHealth {
            private final DataSource dataSource;
            private final String name;
            private volatile boolean healthy = true;
            private volatile long lastCheckTime = System.currentTimeMillis();
            private volatile int failureCount = 0;
            
            public DataSourceHealth(DataSource dataSource, String name) {
                this.dataSource = dataSource;
                this.name = name;
            }
            
            public boolean isHealthy() {
                return healthy;
            }
            
            public void setHealthy(boolean healthy) {
                this.healthy = healthy;
                this.lastCheckTime = System.currentTimeMillis();
                if (healthy) {
                    this.failureCount = 0;
                } else {
                    this.failureCount++;
                }
            }
            
            public DataSource getDataSource() {
                return dataSource;
            }
            
            public String getName() {
                return name;
            }
            
            public long getLastCheckTime() {
                return lastCheckTime;
            }
            
            public int getFailureCount() {
                return failureCount;
            }
        }
        
        /**
         * 注册主数据源
         */
        public void registerMasterDataSource(DataSource dataSource, String name) {
            lock.writeLock().lock();
            try {
                masterDataSources.add(new DataSourceHealth(dataSource, name));
                logger.info("Registered master data source: " + name);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        /**
         * 注册从数据源
         */
        public void registerSlaveDataSource(DataSource dataSource, String name) {
            lock.writeLock().lock();
            try {
                slaveDataSources.add(new DataSourceHealth(dataSource, name));
                logger.info("Registered slave data source: " + name);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        /**
         * 获取健康的主数据源
         */
        public List<DataSourceHealth> getHealthyMasterDataSources() {
            lock.readLock().lock();
            try {
                return masterDataSources.stream()
                        .filter(DataSourceHealth::isHealthy)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        /**
         * 获取健康的从数据源
         */
        public List<DataSourceHealth> getHealthySlaveDataSources() {
            lock.readLock().lock();
            try {
                return slaveDataSources.stream()
                        .filter(DataSourceHealth::isHealthy)
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            } finally {
                lock.readLock().unlock();
            }
        }
        
        /**
         * 检查数据源健康状态
         */
        private boolean checkDataSourceHealth(DataSourceHealth dsHealth) {
            try (Connection connection = dsHealth.getDataSource().getConnection()) {
                // 执行简单查询检查连接
                return connection.isValid(5); // 5秒超时
            } catch (SQLException e) {
                logger.warning("Health check failed for data source " + dsHealth.getName() + ": " + e.getMessage());
                return false;
            }
        }
        
        /**
         * 定时健康检查
         */
        @Scheduled(fixedRate = 30000) // 每30秒检查一次
        public void performHealthCheck() {
            lock.writeLock().lock();
            try {
                // 检查主数据源
                for (DataSourceHealth dsHealth : masterDataSources) {
                    boolean healthy = checkDataSourceHealth(dsHealth);
                    if (dsHealth.isHealthy() != healthy) {
                        logger.info("Master data source " + dsHealth.getName() + " health status changed to: " + healthy);
                    }
                    dsHealth.setHealthy(healthy);
                }
                
                // 检查从数据源
                for (DataSourceHealth dsHealth : slaveDataSources) {
                    boolean healthy = checkDataSourceHealth(dsHealth);
                    if (dsHealth.isHealthy() != healthy) {
                        logger.info("Slave data source " + dsHealth.getName() + " health status changed to: " + healthy);
                    }
                    dsHealth.setHealthy(healthy);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * 负载均衡器
     */
    @Component
    public static class LoadBalancer {
        
        private final AtomicInteger masterRoundRobinCounter = new AtomicInteger(0);
        private final AtomicInteger slaveRoundRobinCounter = new AtomicInteger(0);
        private final DataSourceHealthChecker healthChecker;
        
        public LoadBalancer(DataSourceHealthChecker healthChecker) {
            this.healthChecker = healthChecker;
        }
        
        /**
         * 获取主数据源（写操作）
         */
        public DataSource getMasterDataSource() {
            List<DataSourceHealthChecker.DataSourceHealth> healthyMasters = healthChecker.getHealthyMasterDataSources();
            
            if (healthyMasters.isEmpty()) {
                throw new RuntimeException("No healthy master data source available");
            }
            
            if (healthyMasters.size() == 1) {
                return healthyMasters.get(0).getDataSource();
            }
            
            // 轮询负载均衡
            int index = Math.abs(masterRoundRobinCounter.getAndIncrement()) % healthyMasters.size();
            return healthyMasters.get(index).getDataSource();
        }
        
        /**
         * 获取从数据源（读操作）
         */
        public DataSource getSlaveDataSource() {
            List<DataSourceHealthChecker.DataSourceHealth> healthySlaves = healthChecker.getHealthySlaveDataSources();
            
            // 如果没有健康的从数据源，回退到主数据源
            if (healthySlaves.isEmpty()) {
                logger.warning("No healthy slave data source available, falling back to master");
                return getMasterDataSource();
            }
            
            if (healthySlaves.size() == 1) {
                return healthySlaves.get(0).getDataSource();
            }
            
            // 轮询负载均衡
            int index = Math.abs(slaveRoundRobinCounter.getAndIncrement()) % healthySlaves.size();
            return healthySlaves.get(index).getDataSource();
        }
        
        /**
         * 获取数据源统计信息
         */
        public LoadBalanceStats getStats() {
            List<DataSourceHealthChecker.DataSourceHealth> healthyMasters = healthChecker.getHealthyMasterDataSources();
            List<DataSourceHealthChecker.DataSourceHealth> healthySlaves = healthChecker.getHealthySlaveDataSources();
            
            return new LoadBalanceStats(
                    healthyMasters.size(),
                    healthySlaves.size(),
                    masterRoundRobinCounter.get(),
                    slaveRoundRobinCounter.get()
            );
        }
    }
    
    /**
     * 负载均衡统计信息
     */
    public static class LoadBalanceStats {
        private final int healthyMasterCount;
        private final int healthySlaveCount;
        private final int masterRequestCount;
        private final int slaveRequestCount;
        
        public LoadBalanceStats(int healthyMasterCount, int healthySlaveCount, 
                               int masterRequestCount, int slaveRequestCount) {
            this.healthyMasterCount = healthyMasterCount;
            this.healthySlaveCount = healthySlaveCount;
            this.masterRequestCount = masterRequestCount;
            this.slaveRequestCount = slaveRequestCount;
        }
        
        public int getHealthyMasterCount() {
            return healthyMasterCount;
        }
        
        public int getHealthySlaveCount() {
            return healthySlaveCount;
        }
        
        public int getMasterRequestCount() {
            return masterRequestCount;
        }
        
        public int getSlaveRequestCount() {
            return slaveRequestCount;
        }
        
        @Override
        public String toString() {
            return String.format("LoadBalanceStats{healthyMasters=%d, healthySlaves=%d, masterRequests=%d, slaveRequests=%d}",
                    healthyMasterCount, healthySlaveCount, masterRequestCount, slaveRequestCount);
        }
    }
    
    @Bean
    public DataSourceHealthChecker dataSourceHealthChecker() {
        return new DataSourceHealthChecker();
    }
    
    @Bean
    public LoadBalancer loadBalancer(DataSourceHealthChecker healthChecker) {
        return new LoadBalancer(healthChecker);
    }
}