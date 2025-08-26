package com.hejiexmu.rpc.samples.provider.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpc.server.RpcServer;
import com.rpc.core.metric.MetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 实时监控WebSocket处理器
 * 负责向客户端推送实时监控数据
 */
@Component
@Slf4j
public class RealTimeMonitoringHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static boolean isSchedulerStarted = false;
    
    @Autowired(required = false)
    private RpcServer rpcServer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("WebSocket连接建立: {}", session.getId());
        
        // 启动定时任务（如果还没有启动）
        synchronized (RealTimeMonitoringHandler.class) {
            if (!isSchedulerStarted) {
                startMonitoringScheduler();
                isSchedulerStarted = true;
            }
        }
        
        // 立即发送一次数据（添加异常处理）
        try {
            sendMonitoringData();
        } catch (Exception e) {
            log.error("发送初始监控数据失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket连接关闭: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        sessions.remove(session);
    }

    /**
     * 启动监控数据定时推送
     */
    private void startMonitoringScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendMonitoringData();
            } catch (Exception e) {
                System.err.println("发送监控数据失败: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS); // 每2秒推送一次数据
    }

    /**
     * 向所有连接的客户端发送监控数据
     */
    private void sendMonitoringData() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            Map<String, Object> monitoringData = collectMonitoringData();
            String jsonData = objectMapper.writeValueAsString(monitoringData);
            TextMessage message = new TextMessage(jsonData);

            // 移除已关闭的会话并发送数据
            Iterator<WebSocketSession> iterator = sessions.iterator();
            while (iterator.hasNext()) {
                WebSocketSession session = iterator.next();
                try {
                    if (session.isOpen()) {
                        // 使用同步发送，避免WebSocket状态冲突
                        synchronized (session) {
                            if (session.isOpen()) {
                                session.sendMessage(message);
                            } else {
                                iterator.remove();
                            }
                        }
                    } else {
                        iterator.remove();
                    }
                } catch (IOException e) {
                    log.error("发送消息失败: {}", e.getMessage());
                    iterator.remove();
                } catch (IllegalStateException e) {
                    // 处理WebSocket状态异常（如TEXT_PARTIAL_WRITING状态）
                    log.error("WebSocket状态异常，跳过此次发送: {}", e.getMessage());
                    // 不移除session，下次重试
                } catch (Exception e) {
                    log.error("发送消息时发生未知错误: {}", e.getMessage());
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            log.error("收集监控数据失败: {}", e.getMessage());
        }
    }

    /**
     * 收集监控数据
     */
    private Map<String, Object> collectMonitoringData() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", new Date());
        data.put("type", "monitoring");

        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", runtime.totalMemory());
        memory.put("free", runtime.freeMemory());
        memory.put("used", runtime.totalMemory() - runtime.freeMemory());
        memory.put("max", runtime.maxMemory());
        memory.put("usagePercent", (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100);
        data.put("memory", memory);

        // 线程信息
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Map<String, Object> threads = new HashMap<>();
        threads.put("count", threadBean.getThreadCount());
        threads.put("peak", threadBean.getPeakThreadCount());
        threads.put("daemon", threadBean.getDaemonThreadCount());
        data.put("threads", threads);

        // 系统信息
        Map<String, Object> system = new HashMap<>();
        system.put("processors", runtime.availableProcessors());
        system.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        system.put("loadAverage", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
        data.put("system", system);

        // 真实RPC调用统计
        Map<String, Object> rpcStats = new HashMap<>();
        if (rpcServer != null) {
            try {
                MetricsCollector.MetricsSummary metrics = rpcServer.getMetricsSummary();
                rpcStats.put("totalCalls", metrics.getTotalRequestCount());
                rpcStats.put("successCalls", metrics.getSuccessRequestCount());
                rpcStats.put("failedCalls", metrics.getFailedRequestCount());
                rpcStats.put("avgResponseTime", 0); // 暂时设为0，因为MetricsSummary没有直接提供平均响应时间
                rpcStats.put("currentConnections", metrics.getActiveConnectionCount());
                rpcStats.put("serviceRegistrations", metrics.getServiceRegistrationCount());
                rpcStats.put("serviceDiscoveries", metrics.getServiceDiscoveryCount());
                rpcStats.put("healthChecks", metrics.getHealthCheckCount());
            } catch (Exception e) {
                log.error("获取RPC监控指标失败: {}", e.getMessage());
                // 如果获取真实指标失败，使用默认值
                rpcStats.put("totalCalls", 0);
                rpcStats.put("successCalls", 0);
                rpcStats.put("failedCalls", 0);
                rpcStats.put("avgResponseTime", 0);
                rpcStats.put("currentConnections", 0);
                rpcStats.put("serviceRegistrations", 0);
                rpcStats.put("serviceDiscoveries", 0);
                rpcStats.put("healthChecks", 0);
            }
        } else {
            // RpcServer未注入时的默认值
            rpcStats.put("totalCalls", 0);
            rpcStats.put("successCalls", 0);
            rpcStats.put("failedCalls", 0);
            rpcStats.put("avgResponseTime", 0);
            rpcStats.put("currentConnections", 0);
            rpcStats.put("serviceRegistrations", 0);
            rpcStats.put("serviceDiscoveries", 0);
            rpcStats.put("healthChecks", 0);
        }
        data.put("rpc", rpcStats);

        // 健康状态
        Map<String, Object> health = new HashMap<>();
        double memoryUsage = (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory();
        health.put("status", memoryUsage < 0.9 ? "UP" : "WARNING");
        health.put("memoryStatus", memoryUsage < 0.8 ? "GOOD" : memoryUsage < 0.9 ? "WARNING" : "CRITICAL");
        health.put("rpcStatus", "UP");
        health.put("webStatus", "UP");
        data.put("health", health);

        return data;
    }

    /**
     * 获取当前连接数
     */
    public static int getActiveConnectionCount() {
        return sessions.size();
    }

    /**
     * 关闭调度器（应用关闭时调用）
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}