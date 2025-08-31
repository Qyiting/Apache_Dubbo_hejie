package com.hejiexmu.rpc.auth.config;

import com.hejiexmu.rpc.auth.cache.RedisCacheService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 测试配置类
 * 为单元测试和集成测试提供Mock对象和测试配置
 * 
 * @author hejiexmu
 */
@TestConfiguration
@ActiveProfiles("test")
public class TestConfig {
    
    /**
     * Mock Redis模板
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> mockRedisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }
    
    /**
     * Mock Redis缓存服务
     */
    @Bean
    @Primary
    public RedisCacheService mockRedisCacheService() {
        return Mockito.mock(RedisCacheService.class);
    }
    
    /**
     * 测试用密码编码器
     */
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(4); // 使用较低的强度以加快测试速度
    }
    
    /**
     * Mock主数据源
     */
    @Bean(name = "masterDataSource")
    @Primary
    public DataSource mockMasterDataSource() throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.isValid(Mockito.anyInt())).thenReturn(true);
        return dataSource;
    }
    
    /**
     * Mock从数据源
     */
    @Bean(name = "slaveDataSource")
    public DataSource mockSlaveDataSource() throws SQLException {
        DataSource dataSource = Mockito.mock(DataSource.class);
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.isValid(Mockito.anyInt())).thenReturn(true);
        return dataSource;
    }
}