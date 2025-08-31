package com.hejiexmu.rpc.auth.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置类
 * 实现MySQL主从分离的读写操作
 * 
 * @author hejiexmu
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {
    
    /**
     * 主数据源（写）
     */
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return new HikariDataSource();
    }
    
    /**
     * 从数据源（读）
     */
    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return new HikariDataSource();
    }
    
    /**
     * 动态数据源
     */
    @Bean(name = "dynamicDataSource")
    @Primary
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.MASTER, masterDataSource());
        dataSourceMap.put(DataSourceType.SLAVE, slaveDataSource());
        
        // 设置数据源映射
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(masterDataSource());
        
        return dynamicDataSource;
    }
    
    /**
     * 动态数据源实现
     */
    public static class DynamicDataSource extends AbstractRoutingDataSource {
        
        @Override
        protected Object determineCurrentLookupKey() {
            return DataSourceContextHolder.getDataSourceType();
        }
    }
    
    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        MASTER, SLAVE
    }
    
    /**
     * 数据源上下文持有者
     */
    public static class DataSourceContextHolder {
        
        private static final ThreadLocal<DataSourceType> CONTEXT_HOLDER = new ThreadLocal<>();
        
        /**
         * 设置数据源类型
         */
        public static void setDataSourceType(DataSourceType dataSourceType) {
            CONTEXT_HOLDER.set(dataSourceType);
        }
        
        /**
         * 获取数据源类型
         */
        public static DataSourceType getDataSourceType() {
            DataSourceType dataSourceType = CONTEXT_HOLDER.get();
            return dataSourceType != null ? dataSourceType : DataSourceType.MASTER;
        }
        
        /**
         * 清除数据源类型
         */
        public static void clearDataSourceType() {
            CONTEXT_HOLDER.remove();
        }
        
        /**
         * 设置为主数据源
         */
        public static void useMaster() {
            setDataSourceType(DataSourceType.MASTER);
        }
        
        /**
         * 设置为从数据源
         */
        public static void useSlave() {
            setDataSourceType(DataSourceType.SLAVE);
        }
    }
    
    /**
     * 数据源属性配置
     */
    @Component
    @ConfigurationProperties(prefix = "spring.datasource")
    public static class DataSourceProperties {
        
        private Master master = new Master();
        private Slave slave = new Slave();
        
        // Getters and Setters
        public Master getMaster() {
            return master;
        }
        
        public void setMaster(Master master) {
            this.master = master;
        }
        
        public Slave getSlave() {
            return slave;
        }
        
        public void setSlave(Slave slave) {
            this.slave = slave;
        }
        
        /**
         * 主数据源配置
         */
        public static class Master {
            private String url;
            private String username;
            private String password;
            private String driverClassName = "com.mysql.cj.jdbc.Driver";
            private int maximumPoolSize = 20;
            private int minimumIdle = 5;
            private long connectionTimeout = 30000;
            private long idleTimeout = 600000;
            private long maxLifetime = 1800000;
            
            // Getters and Setters
            public String getUrl() {
                return url;
            }
            
            public void setUrl(String url) {
                this.url = url;
            }
            
            public String getUsername() {
                return username;
            }
            
            public void setUsername(String username) {
                this.username = username;
            }
            
            public String getPassword() {
                return password;
            }
            
            public void setPassword(String password) {
                this.password = password;
            }
            
            public String getDriverClassName() {
                return driverClassName;
            }
            
            public void setDriverClassName(String driverClassName) {
                this.driverClassName = driverClassName;
            }
            
            public int getMaximumPoolSize() {
                return maximumPoolSize;
            }
            
            public void setMaximumPoolSize(int maximumPoolSize) {
                this.maximumPoolSize = maximumPoolSize;
            }
            
            public int getMinimumIdle() {
                return minimumIdle;
            }
            
            public void setMinimumIdle(int minimumIdle) {
                this.minimumIdle = minimumIdle;
            }
            
            public long getConnectionTimeout() {
                return connectionTimeout;
            }
            
            public void setConnectionTimeout(long connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
            }
            
            public long getIdleTimeout() {
                return idleTimeout;
            }
            
            public void setIdleTimeout(long idleTimeout) {
                this.idleTimeout = idleTimeout;
            }
            
            public long getMaxLifetime() {
                return maxLifetime;
            }
            
            public void setMaxLifetime(long maxLifetime) {
                this.maxLifetime = maxLifetime;
            }
        }
        
        /**
         * 从数据源配置
         */
        public static class Slave {
            private String url;
            private String username;
            private String password;
            private String driverClassName = "com.mysql.cj.jdbc.Driver";
            private int maximumPoolSize = 20;
            private int minimumIdle = 5;
            private long connectionTimeout = 30000;
            private long idleTimeout = 600000;
            private long maxLifetime = 1800000;
            
            // Getters and Setters
            public String getUrl() {
                return url;
            }
            
            public void setUrl(String url) {
                this.url = url;
            }
            
            public String getUsername() {
                return username;
            }
            
            public void setUsername(String username) {
                this.username = username;
            }
            
            public String getPassword() {
                return password;
            }
            
            public void setPassword(String password) {
                this.password = password;
            }
            
            public String getDriverClassName() {
                return driverClassName;
            }
            
            public void setDriverClassName(String driverClassName) {
                this.driverClassName = driverClassName;
            }
            
            public int getMaximumPoolSize() {
                return maximumPoolSize;
            }
            
            public void setMaximumPoolSize(int maximumPoolSize) {
                this.maximumPoolSize = maximumPoolSize;
            }
            
            public int getMinimumIdle() {
                return minimumIdle;
            }
            
            public void setMinimumIdle(int minimumIdle) {
                this.minimumIdle = minimumIdle;
            }
            
            public long getConnectionTimeout() {
                return connectionTimeout;
            }
            
            public void setConnectionTimeout(long connectionTimeout) {
                this.connectionTimeout = connectionTimeout;
            }
            
            public long getIdleTimeout() {
                return idleTimeout;
            }
            
            public void setIdleTimeout(long idleTimeout) {
                this.idleTimeout = idleTimeout;
            }
            
            public long getMaxLifetime() {
                return maxLifetime;
            }
            
            public void setMaxLifetime(long maxLifetime) {
                this.maxLifetime = maxLifetime;
            }
        }
    }
}