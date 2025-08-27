package com.hejiexmu.rpc.samples.provider.controller;

import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.registry.ServiceRegistry;
import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import com.rpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Actuator集成控制器
 * 整合Spring Boot Actuator的监控数据
 * 
 * @author hejiexmu
 */
@Slf4j
@RestController
@RequestMapping("/api/actuator")
@CrossOrigin(origins = "*")
public class ActuatorController {

    @Autowired(required = false)
    private HealthEndpoint healthEndpoint;
    
    @Autowired(required = false)
    private InfoEndpoint infoEndpoint;
    
    @Autowired(required = false)
    private MetricsEndpoint metricsEndpoint;
    
    @Autowired(required = false)
    private EnvironmentEndpoint environmentEndpoint;
    
    @Autowired(required = false)
    private RpcServer rpcServer;
    
    @Autowired(required = false)
    private ServiceRegistry serviceRegistry;
    
    @Autowired
    private Environment environment;

    /**
     * 获取增强的健康检查信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getEnhancedHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (healthEndpoint != null) {
                // 获取Actuator健康信息
                var health = healthEndpoint.health();
                response.put("actuator", health);
            }
            
            // 添加自定义健康检查
            Map<String, Object> customHealth = new HashMap<>();
            customHealth.put("rpcProvider", checkRpcProviderHealth());
            customHealth.put("database", checkDatabaseHealth());
            customHealth.put("registry", checkRegistryHealth());
            response.put("custom", customHealth);
            
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "UP");
            
        } catch (Exception e) {
            log.error("获取健康检查信息失败", e);
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取增强的指标信息
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getEnhancedMetrics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (metricsEndpoint != null) {
                // 获取关键指标
                Map<String, Object> metrics = new HashMap<>();
                
                // JVM内存指标
                var jvmMemoryUsed = metricsEndpoint.metric("jvm.memory.used", null);
                var jvmMemoryMax = metricsEndpoint.metric("jvm.memory.max", null);
                metrics.put("jvm.memory.used", jvmMemoryUsed);
                metrics.put("jvm.memory.max", jvmMemoryMax);
                
                // HTTP请求指标
                try {
                    var httpRequests = metricsEndpoint.metric("http.server.requests", null);
                    metrics.put("http.server.requests", httpRequests);
                } catch (Exception e) {
                    // HTTP指标可能不存在
                }
                
                // 系统CPU指标
                try {
                    var systemCpu = metricsEndpoint.metric("system.cpu.usage", null);
                    var processCpu = metricsEndpoint.metric("process.cpu.usage", null);
                    metrics.put("system.cpu.usage", systemCpu);
                    metrics.put("process.cpu.usage", processCpu);
                } catch (Exception e) {
                    // CPU指标可能不存在
                }
                
                response.put("actuator", metrics);
            }
            
            // 添加自定义指标
            Map<String, Object> customMetrics = new HashMap<>();
            customMetrics.put("rpc.calls.total", getRpcCallsTotal());
            customMetrics.put("rpc.calls.success", getRpcCallsSuccess());
            customMetrics.put("rpc.calls.failed", getRpcCallsFailed());
            customMetrics.put("rpc.connections.active", getActiveConnections());
            customMetrics.put("rpc.response.time.avg", getAverageResponseTime());
            response.put("custom", customMetrics);
            
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取指标信息失败", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取应用信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getEnhancedInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (infoEndpoint != null) {
                var info = infoEndpoint.info();
                response.put("actuator", info);
            }
            
            // 添加自定义应用信息
            Map<String, Object> customInfo = new HashMap<>();
            customInfo.put("application.name", "RPC Provider Sample");
            customInfo.put("application.version", "1.0.0");
            customInfo.put("application.description", "RPC框架Provider示例应用");
            customInfo.put("build.time", "2024-01-15 10:30:00");
            customInfo.put("rpc.framework.version", "1.0.0");
            response.put("custom", customInfo);
            
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取应用信息失败", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取环境配置信息
     */
    @GetMapping("/env")
    public ResponseEntity<Map<String, Object>> getEnvironmentInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (environmentEndpoint != null) {
                var env = environmentEndpoint.environment(null);
                
                // 过滤敏感信息
                Map<String, Object> filteredEnv = new HashMap<>();
                if (env.getPropertySources() != null) {
                    env.getPropertySources().forEach(source -> {
                        Map<String, Object> properties = new HashMap<>();
                        if (source.getProperties() != null) {
                            source.getProperties().forEach((key, value) -> {
                                // 过滤敏感配置
                                if (!isSensitiveProperty(key)) {
                                    properties.put(key, value);
                                }
                            });
                        }
                        filteredEnv.put(source.getName(), properties);
                    });
                }
                response.put("actuator", filteredEnv);
            }
            
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            log.error("获取环境信息失败", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // 私有方法：检查RPC Provider健康状态
    private Map<String, Object> checkRpcProviderHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            // 获取所有服务提供者实例的状态
            List<Map<String, Object>> allProviders = new ArrayList<>();
            Set<String> processedInstances = new HashSet<>();
            
            if (serviceRegistry != null) {
                try {
                    // 使用ZookeeperServiceRegistry的getAllServiceInstances方法来获取所有实例
                    if (serviceRegistry instanceof ZookeeperServiceRegistry) {
                        ZookeeperServiceRegistry zkRegistry =
                            (ZookeeperServiceRegistry) serviceRegistry;
                        
                        // 获取所有服务实例
                        List<ServiceInfo> allServiceInstances = zkRegistry.getAllServiceInstances();
                        log.info("从注册中心获取到所有服务实例数量：{}", allServiceInstances.size());
                        
                        for (ServiceInfo serviceInfo : allServiceInstances) {
                            String instanceKey = serviceInfo.getAddress() + ":" + serviceInfo.getPort();
                            if (!processedInstances.contains(instanceKey)) {
                                processedInstances.add(instanceKey);
                                
                                Map<String, Object> providerInfo = new HashMap<>();
                                providerInfo.put("address", serviceInfo.getAddress());
                                providerInfo.put("port", serviceInfo.getPort());
                                providerInfo.put("status", "UP");
                                providerInfo.put("activeConnections", 0); // 模拟值
                                providerInfo.put("lastCheck", System.currentTimeMillis());
                                
                                allProviders.add(providerInfo);
                                log.info("添加服务实例到健康检查：{}:{}", serviceInfo.getAddress(), serviceInfo.getPort());
                            }
                        }
                    } else {
                        // 回退方案：使用标准的服务发现方法
                        List<String> allServiceNames = serviceRegistry.getAllServiceNames();
                        
                        if (allServiceNames != null && !allServiceNames.isEmpty()) {
                            for (String serviceName : allServiceNames) {
                                try {
                                    // 发现该服务的所有实例
                                    List<ServiceInfo> serviceInstances = serviceRegistry.discover(serviceName);
                                    for (ServiceInfo serviceInfo : serviceInstances) {
                                        String instanceKey = serviceInfo.getAddress() + ":" + serviceInfo.getPort();
                                        if (!processedInstances.contains(instanceKey)) {
                                            processedInstances.add(instanceKey);
                                            
                                            Map<String, Object> providerInfo = new HashMap<>();
                                            providerInfo.put("address", serviceInfo.getAddress());
                                            providerInfo.put("port", serviceInfo.getPort());
                                            providerInfo.put("status", "UP");
                                            providerInfo.put("activeConnections", 0); // 模拟值
                                            providerInfo.put("lastCheck", System.currentTimeMillis());
                                            
                                            allProviders.add(providerInfo);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("发现服务 {} 的实例失败: {}", serviceName, e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取所有服务实例失败: {}", e.getMessage());
                }
            }
            
            // 如果没有发现其他实例，至少包含当前实例
            if (allProviders.isEmpty()) {
                Map<String, Object> currentProvider = new HashMap<>();
                currentProvider.put("address", "localhost");
                currentProvider.put("port", Integer.parseInt(environment.getProperty("rpc.provider.port", "9081")));
                currentProvider.put("status", "UP");
                currentProvider.put("activeConnections", getActiveConnections());
                currentProvider.put("lastCheck", System.currentTimeMillis());
                allProviders.add(currentProvider);
            }
            
            health.put("status", "UP");
            health.put("totalProviders", allProviders.size());
            health.put("providers", allProviders);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            log.error("检查RPC Provider健康状态失败", e);
        }
        return health;
    }

    // 私有方法：检查数据库健康状态
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            // 模拟数据库健康检查
            health.put("status", "UP");
            health.put("database", "H2");
            health.put("connectionPool", "HikariCP");
            health.put("activeConnections", 5);
            health.put("maxConnections", 10);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    // 私有方法：检查注册中心健康状态
    private Map<String, Object> checkRegistryHealth() {
        Map<String, Object> health = new HashMap<>();
        try {
            // 模拟注册中心健康检查
            health.put("status", "UP");
            health.put("type", "Zookeeper");
            health.put("address", "192.168.109.103:2181");
            health.put("connected", true);
            health.put("sessionTimeout", 30000);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        return health;
    }

    // 私有方法：获取RPC调用总数
    private long getRpcCallsTotal() {
        if (rpcServer != null) {
            try {
                return rpcServer.getMetricsSummary().getTotalRequestCount();
            } catch (Exception e) {
                log.warn("获取RPC调用总数失败", e);
            }
        }
        return 0;
    }

    // 私有方法：获取RPC成功调用数
    private long getRpcCallsSuccess() {
        if (rpcServer != null) {
            try {
                return rpcServer.getMetricsSummary().getSuccessRequestCount();
            } catch (Exception e) {
                log.warn("获取RPC成功调用数失败", e);
            }
        }
        return 0;
    }

    // 私有方法：获取RPC失败调用数
    private long getRpcCallsFailed() {
        if (rpcServer != null) {
            try {
                return rpcServer.getMetricsSummary().getFailedRequestCount();
            } catch (Exception e) {
                log.warn("获取RPC失败调用数失败", e);
            }
        }
        return 0;
    }

    // 私有方法：获取活跃连接数
    private int getActiveConnections() {
        if (rpcServer != null) {
            try {
                return (int) rpcServer.getMetricsSummary().getActiveConnectionCount();
            } catch (Exception e) {
                log.warn("获取活跃连接数失败", e);
            }
        }
        return 0;
    }

    // 私有方法：获取平均响应时间
    private double getAverageResponseTime() {
        if (rpcServer != null) {
            try {
                // 暂时返回0，因为MetricsSummary没有直接提供平均响应时间
                return 0.0;
            } catch (Exception e) {
                log.warn("获取平均响应时间失败", e);
            }
        }
        return 0.0;
    }

    // 私有方法：判断是否为敏感配置
    private boolean isSensitiveProperty(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || 
               lowerKey.contains("secret") || 
               lowerKey.contains("key") || 
               lowerKey.contains("token") ||
               lowerKey.contains("credential");
    }
}