package com.hejiexmu.rpc.samples.consumer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpc.registry.ServiceRegistry;
import com.hejiexmu.rpc.spring.boot.config.RpcCompatibilityConfiguration.RpcProgrammaticHelper;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC Consumer 管理控制器
 * 提供服务发现、动态调用等REST API接口
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
    
    @Autowired(required = false)
    private ServiceRegistry serviceRegistry;
    
    @Autowired(required = false)
    private RpcProgrammaticHelper rpcHelper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 缓存已发现的服务信息
    private final Map<String, List<ServiceInfo>> serviceCache = new ConcurrentHashMap<>();
    
    // 缓存服务接口的方法信息
    private final Map<String, List<Map<String, Object>>> methodCache = new ConcurrentHashMap<>();

    /**
     * 获取服务概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getServiceOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 基本信息
        overview.put("serviceName", "RPC Consumer Service");
        overview.put("version", "1.0.0");
        overview.put("status", "RUNNING");
        overview.put("startTime", getStartTime());
        overview.put("uptime", getUptime());
        
        // 端口信息
        Map<String, Object> ports = new HashMap<>();
        ports.put("webPort", environment.getProperty("server.port", "8071"));
        ports.put("host", "localhost");
        overview.put("ports", ports);
        
        // 活跃配置文件
        overview.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        
        // 服务发现信息
        Map<String, Object> discoveryInfo = new HashMap<>();
        try {
            if (serviceRegistry != null) {
                // 这里可以添加服务发现相关的统计信息
                discoveryInfo.put("totalServices", 0);
                discoveryInfo.put("registryType", environment.getProperty("rpc.registry.type", "zookeeper"));
                discoveryInfo.put("registryAddress", environment.getProperty("rpc.registry.address", "localhost:2181"));
                discoveryInfo.put("status", "Service Registry Available");
            } else {
                discoveryInfo.put("totalServices", 0);
                discoveryInfo.put("status", "Service Registry Not Available");
            }
        } catch (Exception e) {
            log.error("获取服务发现信息失败", e);
            discoveryInfo.put("error", e.getMessage());
        }
        overview.put("discovery", discoveryInfo);
        
        return ResponseEntity.ok(overview);
    }

    /**
     * 获取所有可用服务列表
     */
    @GetMapping("/services")
    public ResponseEntity<List<Map<String, Object>>> getServices() {
        List<Map<String, Object>> services = new ArrayList<>();
        
        try {
            if (serviceRegistry != null) {
                log.info("开始获取服务列表，serviceRegistry类型: {}", serviceRegistry.getClass().getName());
                
                // 使用ServiceRegistry的getAllServiceNames方法获取所有已注册的服务名称
                List<String> allServiceNames = serviceRegistry.getAllServiceNames();
                log.info("从注册中心获取到服务名称列表: {}", allServiceNames);
                
                if (allServiceNames == null || allServiceNames.isEmpty()) {
                    log.warn("注册中心返回的服务名称列表为空");
                    return ResponseEntity.ok(services);
                }
                
                for (String serviceName : allServiceNames) {
                    log.info("正在处理服务: {}", serviceName);
                    try {
                        // 首先尝试直接发现服务
                        List<ServiceInfo> instances = serviceRegistry.discover(serviceName);
                        log.info("服务 {} 通过直接发现找到实例数: {}", serviceName, instances.size());
                        
                        // 如果直接发现没有结果，尝试使用常见的版本和分组组合
                        if (instances.isEmpty()) {
                            log.info("直接发现服务 {} 没有结果，尝试使用版本和分组发现", serviceName);
                            String[] versions = {"1.0.0", null};
                            String[] groups = {"default", null};
                            
                            for (String version : versions) {
                                for (String group : groups) {
                                    try {
                                        List<ServiceInfo> versionGroupInstances = serviceRegistry.discover(serviceName, version, group);
                                        if (!versionGroupInstances.isEmpty()) {
                                            log.info("服务 {} 使用版本:{} 分组:{} 找到实例数: {}", serviceName, version, group, versionGroupInstances.size());
                                            instances.addAll(versionGroupInstances);
                                        }
                                    } catch (Exception e) {
                                        log.debug("使用版本:{} 分组:{} 发现服务 {} 失败: {}", version, group, serviceName, e.getMessage());
                                    }
                                }
                            }
                        }
                        
                        if (!instances.isEmpty()) {
                            serviceCache.put(serviceName, instances);
                            
                            Map<String, Object> serviceInfo = new HashMap<>();
                            serviceInfo.put("serviceName", serviceName);
                            serviceInfo.put("instanceCount", instances.size());
                            serviceInfo.put("instances", instances);
                            
                            // 尝试获取服务接口信息
                            try {
                                String interfaceClassName = extractInterfaceClassName(serviceName);
                                serviceInfo.put("interfaceClass", interfaceClassName);
                                
                                // 获取方法信息
                                List<Map<String, Object>> methods = getInterfaceMethods(interfaceClassName);
                                serviceInfo.put("methods", methods);
                            } catch (Exception e) {
                                log.warn("无法获取服务接口信息: {}", serviceName, e);
                                serviceInfo.put("interfaceClass", serviceName);
                                serviceInfo.put("methods", new ArrayList<>());
                            }
                            
                            services.add(serviceInfo);
                            log.info("成功添加服务: {} 到结果列表", serviceName);
                        } else {
                            log.warn("服务 {} 没有找到任何实例，跳过显示", serviceName);
                        }
                    } catch (Exception e) {
                        log.error("获取服务实例失败: {}", serviceName, e);
                    }
                }
                
                log.info("最终返回服务数量: {}", services.size());
            } else {
                log.error("ServiceRegistry 为 null，无法获取服务列表");
            }
        } catch (Exception e) {
            log.error("获取服务列表失败", e);
            return ResponseEntity.status(500).body(services);
        }
        
        return ResponseEntity.ok(services);
    }

    /**
     * 获取指定服务的方法列表
     */
    @GetMapping("/services/{serviceName}/methods")
    public ResponseEntity<List<Map<String, Object>>> getServiceMethods(@PathVariable String serviceName) {
        try {
            String interfaceClassName = extractInterfaceClassName(serviceName);
            List<Map<String, Object>> methods = getInterfaceMethods(interfaceClassName);
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            log.error("获取服务方法失败: {}", serviceName, e);
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 动态调用服务方法
     */
    @PostMapping("/services/{serviceName}/invoke")
    public ResponseEntity<Map<String, Object>> invokeServiceMethod(
            @PathVariable String serviceName,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String methodName = (String) request.get("methodName");
            List<Object> parameters = (List<Object>) request.get("parameters");
            // 从URL路径中解析版本和分组信息
            String[] serviceNameParts = serviceName.split(":");
            String version = serviceNameParts.length > 1 ? serviceNameParts[1] : "1.0.0";
            String group = serviceNameParts.length > 2 ? serviceNameParts[2] : "default";
            
            log.info("调用服务方法: serviceName={}, methodName={}, version={}, group={}", serviceName, methodName, version, group);
            
            if (methodName == null) {
                response.put("success", false);
                response.put("error", "方法名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 提取接口类名
            String interfaceClassName = extractInterfaceClassName(serviceName);
            Class<?> interfaceClass = Class.forName(interfaceClassName);
            
            // 创建服务代理
            Object serviceProxy = rpcHelper.createServiceProxy(interfaceClass, version, group);
            
            // 查找匹配的方法
            Method targetMethod = findMatchingMethod(interfaceClass, methodName, parameters);
            if (targetMethod == null) {
                response.put("success", false);
                response.put("error", "未找到匹配的方法: " + methodName);
                return ResponseEntity.badRequest().body(response);
            }
            
            // 转换参数类型
            Object[] convertedParams = convertParameters(targetMethod, parameters);
            
            // 调用方法
            long startTime = System.currentTimeMillis();
            Object result = targetMethod.invoke(serviceProxy, convertedParams);
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("result", result);
            response.put("executionTime", endTime - startTime);
            response.put("timestamp", new Date());
            
        } catch (Exception e) {
            log.error("调用服务方法失败: {}", serviceName, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("stackTrace", getStackTrace(e));
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取健康检查信息
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // JVM信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("processors", runtime.availableProcessors());
        health.put("jvm", jvm);
        
        // 系统信息
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        health.put("system", system);
        
        // 服务发现状态
        Map<String, Object> discoveryStatus = new HashMap<>();
        if (serviceRegistry != null) {
            discoveryStatus.put("status", "UP");
            discoveryStatus.put("type", environment.getProperty("rpc.registry.type", "zookeeper"));
        } else {
            discoveryStatus.put("status", "DOWN");
            discoveryStatus.put("error", "Service Registry Not Available");
        }
        health.put("serviceRegistry", discoveryStatus);
        
        health.put("status", "UP");
        health.put("timestamp", new Date());
        
        return ResponseEntity.ok(health);
    }

    // 辅助方法
    
    private String getStartTime() {
        return new Date(System.currentTimeMillis() - getUptime()).toString();
    }
    
    private long getUptime() {
        return System.currentTimeMillis() - 1000000; // 简化实现
    }
    
    private String extractInterfaceClassName(String serviceName) {
        // 从服务名中提取接口类名
        // 格式: com.hejiexmu.rpc.samples.api.service.UserService:1.0.0:default
        if (serviceName.contains(":")) {
            return serviceName.split(":")[0];
        }
        return serviceName;
    }
    
    private List<Map<String, Object>> getInterfaceMethods(String interfaceClassName) {
        if (methodCache.containsKey(interfaceClassName)) {
            return methodCache.get(interfaceClassName);
        }
        
        List<Map<String, Object>> methods = new ArrayList<>();
        
        try {
            Class<?> interfaceClass = Class.forName(interfaceClassName);
            Method[] declaredMethods = interfaceClass.getDeclaredMethods();
            
            for (Method method : declaredMethods) {
                Map<String, Object> methodInfo = new HashMap<>();
                methodInfo.put("name", method.getName());
                methodInfo.put("returnType", method.getReturnType().getSimpleName());
                
                List<Map<String, Object>> params = new ArrayList<>();
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("name", parameters[i].getName());
                    paramInfo.put("type", parameters[i].getType().getSimpleName());
                    paramInfo.put("fullType", parameters[i].getType().getName());
                    paramInfo.put("index", i);
                    params.add(paramInfo);
                }
                methodInfo.put("parameters", params);
                
                methods.add(methodInfo);
            }
            
            methodCache.put(interfaceClassName, methods);
        } catch (ClassNotFoundException e) {
            log.error("找不到接口类: {}", interfaceClassName, e);
        }
        
        return methods;
    }
    
    private Method findMatchingMethod(Class<?> interfaceClass, String methodName, List<Object> parameters) {
        Method[] methods = interfaceClass.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == (parameters != null ? parameters.size() : 0)) {
                    return method;
                }
            }
        }
        
        return null;
    }
    
    private Object[] convertParameters(Method method, List<Object> parameters) throws Exception {
        if (parameters == null || parameters.isEmpty()) {
            return new Object[0];
        }
        
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] convertedParams = new Object[parameters.size()];
        
        log.debug("方法: {}, 期望参数类型: {}", method.getName(), Arrays.toString(paramTypes));
        log.debug("传入参数: {}", parameters);
        
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            Class<?> targetType = paramTypes[i];
            
            log.debug("参数[{}]: 值={}, 实际类型={}, 期望类型={}", i, param, 
                     param != null ? param.getClass().getName() : "null", targetType.getName());
            
            if (param == null) {
                convertedParams[i] = null;
            } else if (targetType.isAssignableFrom(param.getClass())) {
                convertedParams[i] = param;
                log.debug("参数[{}]: 类型匹配，直接使用", i);
            } else {
                // 尝试类型转换
                convertedParams[i] = convertParameter(param, targetType);
                log.debug("参数[{}]: 转换后类型={}", i, convertedParams[i] != null ? convertedParams[i].getClass().getName() : "null");
            }
        }
        
        return convertedParams;
    }
    
    private Object convertParameter(Object param, Class<?> targetType) throws Exception {
        log.debug("开始参数转换: {} -> {}", param.getClass().getName(), targetType.getName());
        
        if (param instanceof Map && !Map.class.isAssignableFrom(targetType)) {
            // 将Map转换为目标对象
            String json = objectMapper.writeValueAsString(param);
            Object result = objectMapper.readValue(json, targetType);
            log.debug("Map转换完成: {}", result);
            return result;
        } else if (param instanceof String) {
            String str = (String) param;
            if (targetType == Long.class || targetType == long.class) {
                Long result = Long.parseLong(str);
                log.debug("String转Long: {} -> {}", str, result);
                return result;
            } else if (targetType == Integer.class || targetType == int.class) {
                Integer result = Integer.parseInt(str);
                log.debug("String转Integer: {} -> {}", str, result);
                return result;
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                Boolean result = Boolean.parseBoolean(str);
                log.debug("String转Boolean: {} -> {}", str, result);
                return result;
            }
        } else if (param instanceof Number && (targetType == Long.class || targetType == long.class)) {
            Long result = ((Number) param).longValue();
            log.debug("Number转Long: {} -> {}", param, result);
            return result;
        } else if (param instanceof Number && (targetType == Integer.class || targetType == int.class)) {
            Integer result = ((Number) param).intValue();
            log.debug("Number转Integer: {} -> {}", param, result);
            return result;
        }
        
        log.debug("无法转换，返回原值: {}", param);
        return param;
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}