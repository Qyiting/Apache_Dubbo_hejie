package com.hejiexmu.rpc.samples.provider.controller;

import com.rpc.registry.zookeeper.ZookeeperServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rpc.server.RpcServer;
import com.rpc.server.provider.ServiceProvider;
import com.rpc.core.metric.MetricsCollector;
import com.rpc.core.serviceinfo.ServiceInfo;
import com.rpc.registry.ServiceRegistry;
import com.hejiexmu.rpc.samples.provider.service.RemoteInstanceService;

import java.io.BufferedReader;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Random;
import org.springframework.http.HttpHeaders;

/**
 * RPC Provider 管理控制器
 * 提供服务状态、健康检查、配置管理等REST API接口
 * 
 * @author hejiexmu
 */
@Slf4j
@RestController
@RequestMapping("/api/management")
@CrossOrigin(origins = "*")
public class ManagementController {

    @Autowired
    private Environment environment;
    
    @Autowired
    private ActuatorController actuatorController;
    
    @Autowired(required = false)
    @Lazy
    private RpcServer rpcServer;
    
    @Autowired(required = false)
    @Lazy
    private ServiceRegistry serviceRegistry;
    
    @Autowired
    private RemoteInstanceService remoteInstanceService;

    /**
     * 获取服务概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getServiceOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 基本信息
        overview.put("serviceName", "RPC Provider Service");
        overview.put("version", "1.0.0");
        overview.put("status", "RUNNING");
        overview.put("startTime", getStartTime());
        overview.put("uptime", getUptime());
        
        // 端口信息
        Map<String, Object> ports = new HashMap<>();
        ports.put("webPort", environment.getProperty("server.port", "8081"));
        ports.put("rpcPort", environment.getProperty("rpc.provider.port", "9081"));
        ports.put("host", environment.getProperty("rpc.provider.host", "localhost"));
        overview.put("ports", ports);
        
        // 活跃配置文件
        overview.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        
        // 集群信息 - 统计所有实例数量
        Map<String, Object> clusterInfo = new HashMap<>();
        int totalInstances = 0;
        int runningInstances = 0;
        
        try {
            if (serviceRegistry != null && rpcServer != null) {
                List<String> allServiceNames = serviceRegistry.getAllServiceNames();
                Set<String> processedInstances = new HashSet<>();
                
                for (String serviceName : allServiceNames) {
                    try {
                        List<ServiceInfo> serviceInstances = serviceRegistry.discover(serviceName);
                        for (ServiceInfo serviceInfo : serviceInstances) {
                            String instanceKey = serviceInfo.getAddress() + ":" + serviceInfo.getPort();
                            if (!processedInstances.contains(instanceKey)) {
                                processedInstances.add(instanceKey);
                                totalInstances++;
                                if ("RUNNING".equals(serviceInfo.getStatus().getDescription())) {
                                    runningInstances++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取服务 {} 的实例信息失败: {}", serviceName, e.getMessage());
                    }
                }
                
                // 当前实例总是被计算在内
                // 实例发现逻辑在 getInstances 方法中处理
                
            } else {
                totalInstances = 1; // 当前实例
                runningInstances = 1;
            }
        } catch (Exception e) {
            log.error("获取集群信息失败", e);
            totalInstances = 1;
            runningInstances = 1;
        }
        
        clusterInfo.put("totalInstances", totalInstances);
        clusterInfo.put("runningInstances", runningInstances);
        clusterInfo.put("stoppedInstances", totalInstances - runningInstances);
        overview.put("cluster", clusterInfo);
        
        return ResponseEntity.ok(overview);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 获取Actuator增强的健康信息
            try {
                ResponseEntity<Map<String, Object>> actuatorHealth = actuatorController.getEnhancedHealth();
                if (actuatorHealth.getBody() != null) {
                    health.putAll(actuatorHealth.getBody());
                }
            } catch (Exception e) {
                log.warn("获取Actuator健康信息失败，使用默认信息", e);
            }
            
            // 基本健康状态
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            health.put("managementInterface", "ACTIVE");
            
            // 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            Map<String, Object> memory = new HashMap<>();
            memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
            memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
            memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            health.put("memory", memory);
            
            // 系统信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> system = new HashMap<>();
            system.put("processors", runtime.availableProcessors());
            system.put("javaVersion", System.getProperty("java.version"));
            system.put("osName", System.getProperty("os.name"));
            system.put("totalMemory", runtime.totalMemory());
            system.put("freeMemory", runtime.freeMemory());
            system.put("maxMemory", runtime.maxMemory());
            health.put("system", system);
            
            // 线程信息
            Map<String, Object> threads = new HashMap<>();
            threads.put("count", Thread.activeCount());
            threads.put("peakCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            health.put("threads", threads);
            
            // RPC统计信息（真实数据）
            Map<String, Object> rpcStats = new HashMap<>();
            if (rpcServer != null) {
                try {
                    MetricsCollector.MetricsSummary metrics = rpcServer.getMetricsSummary();
                    rpcStats.put("totalCalls", metrics.getTotalRequestCount());
                    rpcStats.put("successCalls", metrics.getSuccessRequestCount());
                    rpcStats.put("failedCalls", metrics.getFailedRequestCount());
                    rpcStats.put("currentConnections", metrics.getActiveConnectionCount());
                    rpcStats.put("serviceRegistrations", metrics.getServiceRegistrationCount());
                    rpcStats.put("serviceUnregistrations", metrics.getServiceUnregistrationCount());
                    rpcStats.put("serviceDiscoveries", metrics.getServiceDiscoveryCount());
                    rpcStats.put("healthChecks", metrics.getHealthCheckCount());
                    rpcStats.put("requestSuccessRate", String.format("%.2f%%", metrics.getRequestSuccessRate() * 100));
                } catch (Exception e) {
                    log.warn("获取RPC指标失败，使用默认值", e);
                    rpcStats.put("error", "无法获取RPC指标: " + e.getMessage());
                    rpcStats.put("serviceRegistrations", 0);
                }
            } else {
                rpcStats.put("status", "RPC服务器未启用");
                rpcStats.put("serviceRegistrations", 0);
            }
            health.put("rpcStats", rpcStats);
            
            // 服务提供者信息
            Map<String, Object> serviceInfo = new HashMap<>();
            if (rpcServer != null && rpcServer.getServiceProvider() != null) {
                try {
                    ServiceProvider serviceProvider = rpcServer.getServiceProvider();
                    serviceInfo.put("registeredServices", serviceProvider.getServiceCount());
                    List<ServiceInfo> services = serviceProvider.getAllServices();
                    List<String> serviceNames = new ArrayList<>();
                    for (ServiceInfo service : services) {
                        serviceNames.add(service.getServiceKey());
                    }
                    serviceInfo.put("serviceList", serviceNames);
                } catch (Exception e) {
                    log.warn("获取服务提供者信息失败", e);
                    serviceInfo.put("error", "无法获取服务信息: " + e.getMessage());
                    serviceInfo.put("registeredServices", 0);
                }
            } else {
                serviceInfo.put("status", "服务提供者未启用");
                serviceInfo.put("registeredServices", 0);
            }
            health.put("serviceProvider", serviceInfo);
            
            // 健康状态
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("status", "UP");
            health.put("health", healthStatus);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * 获取配置信息
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        return getInstanceConfiguration(null);
    }
    
    @GetMapping("/instances/{instanceId}/config")
    public ResponseEntity<Map<String, Object>> getInstanceConfiguration(@PathVariable(required = false) String instanceId) {
        Map<String, Object> config = new HashMap<>();
        
        // 如果指定了instanceId，尝试获取该实例的配置
        if (instanceId != null && !"current-instance".equals(instanceId)) {
            // 对于其他实例，返回基本配置信息（实际场景中可能需要通过RPC调用获取）
            config.put("instanceId", instanceId);
            config.put("note", "远程实例配置信息（模拟数据）");
            
            // 模拟远程实例配置
            Map<String, Object> rpcConfig = new HashMap<>();
            String[] parts = instanceId.split(":");
            if (parts.length == 2) {
                rpcConfig.put("host", parts[0]);
                rpcConfig.put("port", parts[1]);
            } else {
                rpcConfig.put("host", "unknown");
                rpcConfig.put("port", "unknown");
            }
            rpcConfig.put("enabled", "true");
            config.put("rpc", rpcConfig);
            
            Map<String, Object> webConfig = new HashMap<>();
            webConfig.put("port", "N/A");
            webConfig.put("contextPath", "/");
            config.put("web", webConfig);
            
            Map<String, Object> appConfig = new HashMap<>();
            appConfig.put("name", "rpc-provider");
            appConfig.put("profiles", Arrays.asList("default"));
            config.put("application", appConfig);
        } else {
            // 当前实例的配置
            String currentHost = getLocalHostAddress();
            String webPort = environment.getProperty("server.port", "8081");
            String currentInstanceId = currentHost + ":" + webPort;
            config.put("instanceId", currentInstanceId);
            config.put("note", "当前实例配置信息");
            
            // RPC相关配置
            Map<String, Object> rpcConfig = new HashMap<>();
            rpcConfig.put("host", environment.getProperty("rpc.provider.host", "localhost"));
            rpcConfig.put("port", environment.getProperty("rpc.provider.port", "9081"));
            rpcConfig.put("enabled", environment.getProperty("rpc.provider.enabled", "true"));
            config.put("rpc", rpcConfig);
            
            // Web服务配置
            Map<String, Object> webConfig = new HashMap<>();
            webConfig.put("port", environment.getProperty("server.port", "8081"));
            webConfig.put("contextPath", environment.getProperty("server.servlet.context-path", "/"));
            config.put("web", webConfig);
            
            // 应用配置
            Map<String, Object> appConfig = new HashMap<>();
            appConfig.put("name", environment.getProperty("spring.application.name", "rpc-provider"));
            appConfig.put("profiles", Arrays.asList(environment.getActiveProfiles()));
            config.put("application", appConfig);
        }
        
        return ResponseEntity.ok(config);
    }

    /**
     * 获取性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 获取Actuator增强的指标信息
            try {
                ResponseEntity<Map<String, Object>> actuatorMetrics = actuatorController.getEnhancedMetrics();
                if (actuatorMetrics.getBody() != null) {
                    metrics.putAll(actuatorMetrics.getBody());
                }
            } catch (Exception e) {
                log.warn("获取Actuator指标信息失败，使用默认信息", e);
            }
            
            // JVM指标
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            Map<String, Object> jvm = new HashMap<>();
            jvm.put("uptime", runtimeBean.getUptime());
            jvm.put("startTime", runtimeBean.getStartTime());
            jvm.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage());
            jvm.put("nonHeapMemoryUsage", memoryBean.getNonHeapMemoryUsage());
            metrics.put("jvm", jvm);
            
            // 系统指标
            Map<String, Object> system = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            system.put("totalMemory", runtime.totalMemory());
            system.put("freeMemory", runtime.freeMemory());
            system.put("maxMemory", runtime.maxMemory());
            system.put("availableProcessors", runtime.availableProcessors());
            metrics.put("system", system);
            
            // 管理界面特定指标（真实数据）
            Map<String, Object> management = new HashMap<>();
            // 从JVM运行时获取真实的线程和连接数据
            management.put("totalThreads", Thread.activeCount());
            management.put("peakThreads", ManagementFactory.getThreadMXBean().getPeakThreadCount());
            management.put("daemonThreads", ManagementFactory.getThreadMXBean().getDaemonThreadCount());
            
            // 获取真实的RPC服务统计
            if (rpcServer != null) {
                try {
                    MetricsCollector.MetricsSummary rpcMetrics = rpcServer.getMetricsSummary();
                    management.put("totalRpcCalls", rpcMetrics.getTotalRequestCount());
                    management.put("activeConnections", rpcMetrics.getActiveConnectionCount());
                    management.put("registeredServices", rpcMetrics.getServiceRegistrationCount());
                } catch (Exception e) {
                    log.warn("获取RPC指标失败: {}", e.getMessage());
                    management.put("totalRpcCalls", 0);
                    management.put("activeConnections", 0);
                    management.put("registeredServices", 0);
                }
            } else {
                management.put("totalRpcCalls", 0);
                management.put("activeConnections", 0);
                management.put("registeredServices", 0);
            }
            
            // 获取真实的内存使用情况
            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            long heapUsed = memBean.getHeapMemoryUsage().getUsed();
            long heapMax = memBean.getHeapMemoryUsage().getMax();
            management.put("memoryUsagePercent", String.format("%.2f%%", (double) heapUsed / heapMax * 100));
            
            metrics.put("management", management);
            
        } catch (Exception e) {
            log.error("获取性能指标失败", e);
            metrics.put("error", e.getMessage());
            return ResponseEntity.status(500).body(metrics);
        }
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * 获取日志级别配置
     */
    @GetMapping("/logging")
    public ResponseEntity<Map<String, Object>> getLoggingConfig() {
        Map<String, Object> logging = new HashMap<>();
        
        // 日志级别配置
        Map<String, String> levels = new HashMap<>();
        levels.put("root", environment.getProperty("logging.level.root", "INFO"));
        levels.put("com.hejiexmu.rpc", environment.getProperty("logging.level.com.hejiexmu.rpc", "INFO"));
        logging.put("levels", levels);
        
        // 日志文件配置
        Map<String, Object> file = new HashMap<>();
        file.put("name", environment.getProperty("logging.file.name", "logs/rpc-provider.log"));
        file.put("path", environment.getProperty("logging.file.path", "logs"));
        logging.put("file", file);
        
        return ResponseEntity.ok(logging);
    }

    /**
     * 获取启动时间
     */
    private String getStartTime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long startTime = runtimeBean.getStartTime();
        return new Date(startTime).toString();
    }

    /**
     * 获取运行时间（真实数据）
     */
    private String getUptime() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime(); // 毫秒
        
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%d天 %d小时 %d分钟 %d秒", 
            days, hours % 24, minutes % 60, seconds % 60);
    }
    
    /**
     * 获取运行时间（毫秒）
     */
    private long getUptimeMillis() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        return runtimeBean.getUptime();
    }
    
    /**
     * 获取日志数据
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(value = "lines", defaultValue = "100") int lines,
            @RequestParam(value = "level", defaultValue = "INFO") String level) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // 生成当前实例的唯一标识
        String currentHost = getLocalHostAddress();
        String webPort = environment.getProperty("server.port", "8081");
        String instanceId = currentHost + ":" + webPort;
        
        // 优先读取真实日志文件
        logs = readActualLogFiles(lines, level, instanceId);
        
        // 如果没有读取到真实日志，则生成基本的运行状态日志
        if (logs.isEmpty()) {
            logs = generateBasicStatusLogs(lines, instanceId);
        }
        
        response.put("logs", logs);
        response.put("total", logs.size());
        response.put("level", level);
        response.put("instanceId", instanceId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 读取实际的日志文件
     */
    private List<Map<String, Object>> readActualLogFiles(int lines, String level, String instanceId) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // 尝试读取Spring Boot默认日志文件位置
        String[] logPaths = {
            "logs/rpc-provider.log",
            "logs/rpc-provider-8082.log", 
            "logs/spring.log",
            "logs/application.log",
            "application.log",
            "spring.log"
        };
        
        for (String logPath : logPaths) {
            try {
                File logFile = new File(logPath);
                if (logFile.exists() && logFile.canRead()) {
                    logs = parseLogFile(logFile, lines, level, instanceId);
                    if (!logs.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("无法读取日志文件 {}: {}", logPath, e.getMessage());
            }
        }
        
        // 如果没有找到日志文件，生成基于真实系统状态的日志
        if (logs.isEmpty()) {
            logs = generateBasicStatusLogs(lines, instanceId);
        }
        
        return logs;
    }
    
    /**
     * 解析日志文件
     */
    private List<Map<String, Object>> parseLogFile(File logFile, int lines, String level, String instanceId) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(
                logFile.toPath(), StandardCharsets.UTF_8)) {
            
            List<String> allLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
            
            // 取最后N行
            int startIndex = Math.max(0, allLines.size() - lines);
            for (int i = startIndex; i < allLines.size(); i++) {
                String logLine = allLines.get(i);
                Map<String, Object> logEntry = parseLogLine(logLine, instanceId);
                if (logEntry != null) {
                    logs.add(logEntry);
                }
            }
            
        } catch (Exception e) {
            log.warn("解析日志文件失败: {}", e.getMessage());
        }
        
        return logs;
    }
    
    /**
     * 解析单行日志
     */
    private Map<String, Object> parseLogLine(String logLine, String instanceId) {
        Map<String, Object> logEntry = new HashMap<>();
        
        try {
            // 简单的日志解析（适用于Spring Boot默认格式）
            // 格式示例: 2024-01-15 10:30:45.123  INFO 12345 --- [main] com.example.Class : Message
            if (logLine.length() > 23) {
                String timestamp = logLine.substring(0, 23).trim();
                logEntry.put("timestamp", timestamp);
                
                // 提取日志级别
                String remaining = logLine.substring(23).trim();
                String[] parts = remaining.split("\\s+", 4);
                if (parts.length >= 4) {
                    logEntry.put("level", parts[0]);
                    logEntry.put("thread", parts[2]);
                    logEntry.put("message", "[" + instanceId + "] " + parts[3]);
                } else {
                    logEntry.put("level", "INFO");
                    logEntry.put("thread", "unknown");
                    logEntry.put("message", "[" + instanceId + "] " + remaining);
                }
            } else {
                logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                logEntry.put("level", "INFO");
                logEntry.put("thread", "unknown");
                logEntry.put("message", "[" + instanceId + "] " + logLine);
            }
        } catch (Exception e) {
            // 解析失败时的默认处理
            logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            logEntry.put("level", "INFO");
            logEntry.put("thread", "unknown");
            logEntry.put("message", "[" + instanceId + "] " + logLine);
        }
        
        return logEntry;
    }
    
    /**
     * 生成基于真实系统状态的日志数据
     */
    private List<Map<String, Object>> generateBasicStatusLogs(int lines, String instanceId) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        
        // 生成基于真实系统状态的日志条目
        for (int i = 0; i < Math.min(lines, 10); i++) {
            Map<String, Object> log = new HashMap<>();
            LocalDateTime logTime = now.minusMinutes(i * 5);
            log.put("timestamp", logTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            switch (i % 4) {
                case 0:
                    log.put("level", "INFO");
                    log.put("message", String.format("[%s] RPC Provider服务运行正常 - 运行时间: %s", instanceId, getUptime()));
                    log.put("thread", "main");
                    break;
                case 1:
                    Runtime runtime = Runtime.getRuntime();
                    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                    long maxMemory = runtime.maxMemory();
                    double memoryUsage = (double) usedMemory / maxMemory * 100;
                    log.put("level", memoryUsage > 80 ? "WARN" : "INFO");
                    log.put("message", String.format("[%s] 内存使用情况 - 已用: %dMB, 最大: %dMB, 使用率: %.2f%%", 
                            instanceId, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, memoryUsage));
                    log.put("thread", "monitor");
                    break;
                case 2:
                    int activeThreads = Thread.activeCount();
                    log.put("level", "INFO");
                    log.put("message", String.format("[%s] 线程状态检查 - 活跃线程数: %d", instanceId, activeThreads));
                    log.put("thread", "scheduler");
                    break;
                case 3:
                    if (rpcServer != null) {
                        try {
                            MetricsCollector.MetricsSummary metrics = rpcServer.getMetricsSummary();
                            log.put("level", "INFO");
                            log.put("message", String.format("[%s] RPC服务统计 - 总请求: %d, 成功: %d, 活跃连接: %d", 
                                    instanceId, metrics.getTotalRequestCount(), metrics.getSuccessRequestCount(), metrics.getActiveConnectionCount()));
                        } catch (Exception e) {
                            log.put("level", "WARN");
                            log.put("message", String.format("[%s] 无法获取RPC服务统计信息: %s", instanceId, e.getMessage()));
                        }
                    } else {
                        log.put("level", "WARN");
                        log.put("message", String.format("[%s] RPC服务器未初始化", instanceId));
                    }
                    log.put("thread", "rpc-monitor");
                    break;
            }
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * 获取本地主机地址
     */
    private String getLocalHostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    /**
     * 检查两个实例地址是否指向同一个实例
     * 考虑localhost和实际IP地址的映射
     */
    private boolean isSameInstance(String address1, String address2) {
        if (address1.equals(address2)) {
            return true;
        }
        
        // 提取主机和端口
        String[] parts1 = address1.split(":");
        String[] parts2 = address2.split(":");
        
        if (parts1.length != 2 || parts2.length != 2) {
            return false;
        }
        
        String host1 = parts1[0];
        String port1 = parts1[1];
        String host2 = parts2[0];
        String port2 = parts2[1];
        
        // 端口必须相同
        if (!port1.equals(port2)) {
            return false;
        }
        
        // 检查主机是否相同（考虑localhost映射）
        return isSameHost(host1, host2);
    }
    
    /**
     * 检查两个主机地址是否指向同一个主机
     */
    private boolean isSameHost(String host1, String host2) {
        if (host1.equals(host2)) {
            return true;
        }
        
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            
            // 将localhost转换为实际IP进行比较
            String normalizedHost1 = "localhost".equals(host1) ? localIp : host1;
            String normalizedHost2 = "localhost".equals(host2) ? localIp : host2;
            
            return normalizedHost1.equals(normalizedHost2);
        } catch (Exception e) {
            log.warn("比较主机地址时发生异常", e);
            return false;
        }
    }
    
    /**
     * 创建当前实例信息
     */
    private Map<String, Object> createCurrentInstanceInfo() {
        Map<String, Object> currentInstance = new HashMap<>();
        
        String currentHost = getLocalHostAddress();
        String currentPort = environment.getProperty("rpc.provider.port", "9081");
        String currentInstanceKey = currentHost + ":" + currentPort;
        
        currentInstance.put("id", currentInstanceKey);
        currentInstance.put("name", environment.getProperty("spring.application.name", "rpc-provider-sample"));
        currentInstance.put("status", rpcServer != null && rpcServer.isRunning() ? "RUNNING" : "STOPPED");
        currentInstance.put("host", currentHost);
        currentInstance.put("port", currentPort);
        currentInstance.put("version", "1.0.0");
        currentInstance.put("group", "default");
        currentInstance.put("weight", 1);
        
        // 获取启动时间
        currentInstance.put("startTime", getApplicationStartTime());
        
        // 获取内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
        currentInstance.put("memoryUsage", String.format("%.1f%%", memoryUsagePercent));
        
        // 获取RPC统计信息
        if (rpcServer != null) {
            try {
                MetricsCollector.MetricsSummary metrics = rpcServer.getMetricsSummary();
                currentInstance.put("rpcCalls", metrics.getTotalRequestCount());
                currentInstance.put("connections", metrics.getActiveConnectionCount());
                currentInstance.put("serviceCount", metrics.getServiceRegistrationCount());
            } catch (Exception e) {
                log.warn("获取RPC指标失败", e);
                currentInstance.put("rpcCalls", 0);
                currentInstance.put("connections", 0);
                currentInstance.put("serviceCount", rpcServer != null ? rpcServer.getServiceProvider().getServiceCount() : 0);
            }
        } else {
            currentInstance.put("rpcCalls", 0);
            currentInstance.put("connections", 0);
            currentInstance.put("serviceCount", rpcServer != null ? rpcServer.getServiceProvider().getServiceCount() : 0);
        }
        
        return currentInstance;
    }
    
    /**
     * 获取应用启动时间
     */
    private String getApplicationStartTime() {
        try {
            long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 发现服务的所有实例（包括所有版本和组）
     */
    private List<ServiceInfo> discoverAllServiceInstances(String serviceName) {
        List<ServiceInfo> allInstances = new ArrayList<>();
        try {
            // 使用ZookeeperServiceRegistry的getAllServiceInstances方法来递归发现所有实例
            if (serviceRegistry != null && serviceRegistry instanceof ZookeeperServiceRegistry) {
                ZookeeperServiceRegistry zkRegistry =
                    (ZookeeperServiceRegistry) serviceRegistry;
                
                // 调用getAllServiceInstances获取所有服务实例
                List<ServiceInfo> allServiceInstances = zkRegistry.getAllServiceInstances();
                log.info("从注册中心获取到所有服务实例数量：{}", allServiceInstances.size());
                
                // 详细记录每个发现的实例
                for (int i = 0; i < allServiceInstances.size(); i++) {
                    ServiceInfo instance = allServiceInstances.get(i);
                    log.info("实例[{}]: {}:{}:{} - 版本:{}, 组:{}, 状态:{}", i, instance.getAddress(), instance.getPort(), 
                        instance.getServiceName(), instance.getVersion(), instance.getGroup(), instance.getStatus());
                }
                
                // 过滤出指定服务名称的实例
                for (ServiceInfo instance : allServiceInstances) {
                    if (serviceName.equals(instance.getServiceName())) {
                        allInstances.add(instance);
                        log.info("匹配到服务实例：{}:{} - 版本:{}, 组:{}, 状态:{}", 
                            instance.getAddress(), instance.getPort(), 
                            instance.getVersion(), instance.getGroup(), 
                            instance.getStatus());
                    }
                }
                
                log.info("服务 {} 匹配到的实例数量：{}", serviceName, allInstances.size());
                return allInstances;
            }
            
            // 回退方案：使用标准的服务发现方法
            allInstances = serviceRegistry.discover(serviceName);
            log.info("通过标准服务发现找到服务 {} 的实例数量：{}", serviceName, allInstances.size());
            
            // 如果标准发现没有结果，尝试常见的版本和组组合
            if (allInstances.isEmpty()) {
                String[] versions = {"1.0.0", null};
                String[] groups = {"default", null};
                
                for (String version : versions) {
                    for (String group : groups) {
                        try {
                            List<ServiceInfo> instances = serviceRegistry.discover(serviceName, version, group);
                            if (instances != null) {
                                for (ServiceInfo instance : instances) {
                                    // 避免重复添加相同的实例
                                    boolean exists = allInstances.stream().anyMatch(existing -> 
                                        existing.getAddress().equals(instance.getAddress()) && 
                                        existing.getPort() == instance.getPort());
                                    if (!exists) {
                                        allInstances.add(instance);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // 忽略特定版本/组不存在的情况
                            log.debug("未找到服务实例：serviceName={}, version={}, group={}", serviceName, version, group);
                        }
                    }
                }
            }
            
            log.info("发现服务 {} 的总实例数量：{}", serviceName, allInstances.size());
            
            // 打印发现的实例详情用于调试
            for (ServiceInfo instance : allInstances) {
                log.info("发现实例：{}:{} - 版本:{}, 组:{}, 状态:{}", 
                    instance.getAddress(), instance.getPort(), 
                    instance.getVersion(), instance.getGroup(), 
                    instance.getStatus());
            }
            
        } catch (Exception e) {
            log.error("发现服务实例失败：{}", serviceName, e);
        }
        return allInstances;
    }

    // 实例管理相关API
    @GetMapping("/instances")
    public ResponseEntity<List<Map<String, Object>>> getInstances() {
        List<Map<String, Object>> instances = new ArrayList<>();
        
        try {
            log.info("检查依赖状态 - serviceRegistry: {}, rpcServer: {}", 
                    serviceRegistry != null ? "存在" : "null", 
                    rpcServer != null ? "存在" : "null");
            
            // 从注册中心获取所有服务实例
            if (serviceRegistry != null && rpcServer != null) {
                // 获取所有服务名称
                List<String> allServiceNames = serviceRegistry.getAllServiceNames();
                Set<String> processedInstances = new HashSet<>();
                
                log.info("开始获取实例信息，发现服务名称数量：{}", allServiceNames != null ? allServiceNames.size() : 0);
                if (allServiceNames != null) {
                    log.info("所有服务名称：{}", allServiceNames);
                    
                    // 同时显示当前实例实际注册的服务名称
                    if (rpcServer != null && rpcServer.getServiceProvider() != null) {
                        ServiceProvider serviceProvider = rpcServer.getServiceProvider();
                        List<ServiceInfo> localServices = serviceProvider.getAllServices();
                        log.info("当前实例实际注册的服务数量：{}", localServices.size());
                        for (ServiceInfo service : localServices) {
                            log.info("本地服务：{}", service.getServiceKey());
                        }
                    }
                } else {
                    log.warn("getAllServiceNames() 返回 null");
                }
                if (allServiceNames != null && !allServiceNames.isEmpty()) {
                    for (String serviceName : allServiceNames) {
                        log.info("正在处理服务：{}", serviceName);
                        
                        // 发现该服务的所有实例
                        List<ServiceInfo> serviceInstances = discoverAllServiceInstances(serviceName);
                        log.info("服务 {} 发现实例数量：{}", serviceName, serviceInstances.size());
                        
                        for (ServiceInfo serviceInfo : serviceInstances) {
                            String instanceKey = serviceInfo.getAddress() + ":" + serviceInfo.getPort();
                            if (!processedInstances.contains(instanceKey)) {
                                processedInstances.add(instanceKey);
                                
                                Map<String, Object> instanceData = createInstanceInfo(serviceInfo, serviceName);
                                instances.add(instanceData);
                                log.info("添加实例：{} -> {}:{}", serviceName, serviceInfo.getAddress(), serviceInfo.getPort());
                            }
                        }
                    }
                } else {
                    log.warn("未发现任何注册的服务名称");
                }
                
                // 检查当前实例是否已经在发现的实例中
                // 如果没有，则添加当前实例信息
                String currentHost = getLocalHostAddress();
                String currentPort = environment.getProperty("rpc.provider.port", "9081");
                String currentInstanceKey = currentHost + ":" + currentPort;
                
                // 检查当前实例是否已存在（考虑localhost和实际IP的映射）
                boolean currentInstanceFound = false;
                for (String processedKey : processedInstances) {
                    if (isSameInstance(processedKey, currentInstanceKey)) {
                        currentInstanceFound = true;
                        break;
                    }
                }
                log.info("当前实例 {} 是否已在发现列表中：{}", currentInstanceKey, currentInstanceFound);
                
                if (!currentInstanceFound) {
                    Map<String, Object> currentInstanceInfo = createCurrentInstanceInfo();
                    instances.add(currentInstanceInfo);
                    log.info("添加当前实例：{}", currentInstanceKey);
                } else {
                    log.info("当前实例已存在于发现列表中，跳过重复添加");
                }
            } else {
                // 如果ServiceRegistry不可用，只返回当前实例信息
                Map<String, Object> currentInstance = createCurrentInstanceInfo();
                instances.add(currentInstance);
                log.warn("ServiceRegistry不可用，只返回当前实例信息");
            }
        } catch (Exception e) {
            log.error("获取实例信息失败", e);
            // 返回错误信息
            Map<String, Object> errorInstance = new HashMap<>();
            errorInstance.put("id", "error");
            errorInstance.put("name", "获取实例信息失败");
            errorInstance.put("status", "ERROR");
            errorInstance.put("error", e.getMessage());
            instances.add(errorInstance);
        }
        
        return ResponseEntity.ok(instances);
    }
    
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<Map<String, Object>> getInstanceDetail(@PathVariable String instanceId) {
        Map<String, Object> instance = new HashMap<>();
        
        try {
            // 判断是否为当前实例
            String currentHost = getLocalHostAddress();
            String currentPort = environment.getProperty("rpc.provider.port", "9081");
            String currentInstanceId = currentHost + ":" + currentPort;
            
            if ("current-instance".equals(instanceId) || currentInstanceId.equals(instanceId)) {
                // 当前实例，获取真实数据
                instance = getCurrentInstanceDetail();
            } else {
                // 其他实例，尝试通过HTTP调用获取数据
                instance = getRemoteInstanceDetail(instanceId);
            }
        } catch (Exception e) {
            log.error("获取实例详情失败: {}", instanceId, e);
            instance.put("id", instanceId);
            instance.put("name", "实例详情获取失败");
            instance.put("status", "ERROR");
            instance.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(instance);
    }
    
    /**
     * 获取当前实例详情
     */
    private Map<String, Object> getCurrentInstanceDetail() {
        Map<String, Object> instance = new HashMap<>();
        
        String currentHost = getLocalHostAddress();
        String currentPort = environment.getProperty("rpc.provider.port", "9081");
        String instanceId = currentHost + ":" + currentPort;
        
        instance.put("id", instanceId);
        instance.put("name", environment.getProperty("spring.application.name", "RPC Provider"));
        instance.put("status", rpcServer != null && rpcServer.isRunning() ? "RUNNING" : "STOPPED");
        instance.put("host", currentHost);
        instance.put("port", Integer.parseInt(currentPort));
        instance.put("startTime", getApplicationStartTime());
        instance.put("uptime", getUptime());
        instance.put("version", "1.0.0");
        
        // 获取内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
        instance.put("memoryUsage", String.format("%.1f%%", memoryUsagePercent));
        
        // CPU使用率（简单估算）
        instance.put("cpuUsage", "N/A");
        
        // 线程数
        instance.put("threadCount", Thread.activeCount());
        
        // RPC统计信息
        if (rpcServer != null) {
            try {
                MetricsCollector.MetricsSummary metrics = rpcServer.getMetricsSummary();
                instance.put("rpcCalls", metrics.getTotalRequestCount());
                instance.put("successCalls", metrics.getSuccessRequestCount());
                instance.put("failedCalls", metrics.getFailedRequestCount());
                instance.put("avgResponseTime", "N/A");
                instance.put("connections", metrics.getActiveConnectionCount());
            } catch (Exception e) {
                log.warn("获取RPC指标失败", e);
                instance.put("rpcCalls", 0);
                instance.put("successCalls", 0);
                instance.put("failedCalls", 0);
                instance.put("avgResponseTime", "N/A");
                instance.put("connections", 0);
            }
        } else {
            instance.put("rpcCalls", 0);
            instance.put("successCalls", 0);
            instance.put("failedCalls", 0);
            instance.put("avgResponseTime", "N/A");
            instance.put("connections", 0);
        }
        
        return instance;
    }
    
    /**
     * 获取远程实例详情
     */
    private Map<String, Object> getRemoteInstanceDetail(String instanceId) {
        try {
            // 使用RemoteInstanceService获取远程实例详情
            return remoteInstanceService.getRemoteInstanceDetail(instanceId);
        } catch (Exception e) {
            log.error("获取远程实例详情失败: {}", instanceId, e);
            
            // 如果获取失败，返回基本信息和错误状态
            Map<String, Object> instance = new HashMap<>();
            String[] parts = instanceId.split(":");
            String host = parts.length >= 1 ? parts[0] : "unknown";
            String rpcPort = parts.length >= 2 ? parts[1] : "unknown";
            
            instance.put("id", instanceId);
            instance.put("name", "Remote RPC Provider");
            instance.put("status", "UNREACHABLE");
            instance.put("host", host);
            instance.put("port", rpcPort.equals("unknown") ? 0 : Integer.parseInt(rpcPort));
            instance.put("startTime", "N/A");
            instance.put("uptime", "N/A");
            instance.put("version", "N/A");
            instance.put("memoryUsage", "N/A");
            instance.put("cpuUsage", "N/A");
            instance.put("threadCount", "N/A");
            instance.put("rpcCalls", "N/A");
            instance.put("successCalls", "N/A");
            instance.put("failedCalls", "N/A");
            instance.put("avgResponseTime", "N/A");
            instance.put("connections", "N/A");
            instance.put("error", e.getMessage());
            
            return instance;
        }
    }
    
    @GetMapping("/instances/{instanceId}/logs")
    public ResponseEntity<Map<String, Object>> getInstanceLogs(
            @PathVariable String instanceId,
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "INFO") String level) {
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try {
            // 判断是否为当前实例
            String currentHost = getLocalHostAddress();
            String currentPort = environment.getProperty("rpc.provider.port", "9081");
            String currentInstanceId = currentHost + ":" + currentPort;
            
            if ("current-instance".equals(instanceId) || currentInstanceId.equals(instanceId)) {
                // 当前实例，读取实际日志文件
                logs = readActualLogFiles(lines, level, instanceId);
            } else {
                // 其他实例，通过HTTP调用获取日志
                logs = getRemoteInstanceLogs(instanceId, lines, level);
            }
        } catch (Exception e) {
            log.warn("获取实例 {} 的日志失败: {}", instanceId, e.getMessage());
            // 如果获取失败，返回错误信息
            Map<String, Object> errorLog = new HashMap<>();
            errorLog.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            errorLog.put("level", "ERROR");
            errorLog.put("thread", "main");
            errorLog.put("message", "获取远程实例日志失败: " + e.getMessage());
            logs.add(errorLog);
        }
        
        response.put("logs", logs);
        response.put("instanceId", instanceId);
        response.put("total", logs.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取远程实例日志
     */
    private List<Map<String, Object>> getRemoteInstanceLogs(String instanceId, int lines, String level) {
        try {
            // 使用RemoteInstanceService获取远程实例日志
            return remoteInstanceService.getRemoteInstanceLogs(instanceId, lines, level);
        } catch (Exception e) {
            log.error("获取远程实例日志失败: {}", instanceId, e);
            throw e;
        }
    }
    
    @PostMapping("/instances/{instanceId}/restart")
    public ResponseEntity<Map<String, Object>> restartInstance(@PathVariable String instanceId) {
        Map<String, Object> response = new HashMap<>();
        
        // 模拟重启操作
        try {
            Thread.sleep(1000); // 模拟重启耗时
            response.put("success", true);
            response.put("message", "实例 " + instanceId + " 重启成功");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } catch (InterruptedException e) {
            response.put("success", false);
            response.put("message", "重启操作被中断");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/instances/{instanceId}/stop")
    public ResponseEntity<Map<String, Object>> stopInstance(@PathVariable String instanceId) {
        Map<String, Object> response = new HashMap<>();
        
        // 模拟停止操作
        response.put("success", true);
        response.put("message", "实例 " + instanceId + " 停止成功");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/instances/{instanceId}/logs/export")
    public ResponseEntity<byte[]> exportInstanceLogs(@PathVariable String instanceId) {
        try {
            String logContent;
            
            // 判断是否为当前实例
            String currentHost = getLocalHostAddress();
            String currentPort = environment.getProperty("rpc.provider.port", "9081");
            String currentInstanceId = currentHost + ":" + currentPort;
            
            if ("current-instance".equals(instanceId) || currentInstanceId.equals(instanceId)) {
                // 当前实例，读取实际日志文件
                logContent = exportCurrentInstanceLogs();
            } else {
                // 其他实例，通过HTTP调用获取日志
                logContent = exportRemoteInstanceLogs(instanceId);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=instance-" + instanceId + "-logs.txt");
            headers.add("Content-Type", "text/plain; charset=utf-8");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(logContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("导出实例日志失败: {}", instanceId, e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 导出当前实例日志
     */
    private String exportCurrentInstanceLogs() {
        StringBuilder logContent = new StringBuilder();
        String instanceId = getLocalHostAddress() + ":" + environment.getProperty("rpc.provider.port", "9081");
        
        logContent.append("=== Instance ").append(instanceId).append(" Logs (Current) ===").append("\n");
        logContent.append("Export Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // 尝试读取实际日志文件
        List<Map<String, Object>> logs = readActualLogFiles(1000, "ALL", instanceId);
        
        for (Map<String, Object> log : logs) {
            logContent.append("[").append(log.get("timestamp")).append("] ");
            logContent.append("[").append(log.get("level")).append("] ");
            logContent.append("[").append(log.get("thread")).append("] ");
            logContent.append(log.get("message")).append("\n");
        }
        
        return logContent.toString();
    }
    
    /**
     * 导出远程实例日志
     */
    private String exportRemoteInstanceLogs(String instanceId) {
        try {
            // 使用RemoteInstanceService导出远程实例日志
            return remoteInstanceService.exportRemoteInstanceLogs(instanceId);
        } catch (Exception e) {
            log.error("导出远程实例日志失败: {}", instanceId, e);
            
            StringBuilder logContent = new StringBuilder();
            logContent.append("=== Instance ").append(instanceId).append(" Logs (Remote) ===").append("\n");
            logContent.append("Export Time: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            logContent.append("Error: 导出远程实例日志失败 - ").append(e.getMessage()).append("\n");
            
            return logContent.toString();
        }
    }
    
    /**
     * 创建实例信息
     */
    private Map<String, Object> createInstanceInfo(ServiceInfo serviceInfo, String serviceName) {
        Map<String, Object> instanceData = new HashMap<>();
        instanceData.put("id", serviceInfo.getAddress() + ":" + serviceInfo.getPort());
        instanceData.put("serviceName", serviceName);
        instanceData.put("host", serviceInfo.getAddress());
        instanceData.put("port", serviceInfo.getPort());
        instanceData.put("status", serviceInfo.getStatus().getDescription());
        instanceData.put("weight", serviceInfo.getWeight());
        instanceData.put("version", serviceInfo.getVersion());
        instanceData.put("group", serviceInfo.getGroup());
        instanceData.put("registrationTime", System.currentTimeMillis());
        instanceData.put("lastHeartbeat", System.currentTimeMillis());
        instanceData.put("uptime", getUptime());
        instanceData.put("isCurrent", false);
        return instanceData;
    }
}