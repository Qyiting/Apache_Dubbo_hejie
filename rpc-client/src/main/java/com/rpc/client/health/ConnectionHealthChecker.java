package com.rpc.client.health;

import com.rpc.core.metric.MetricsCollector;
import com.rpc.netty.client.NettyRpcClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 连接健康检查器
 * 定期检查连接池中的连接健康状态，自动移除不健康的连接
 *
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class ConnectionHealthChecker {
    /** 健康检查间隔（毫秒） */
    private final long checkInterval;
    /** 连接超时时间（毫秒） */
    private final long connectionTimeout;
    /** 最大重试次数 */
    private final int maxRetries;
    /** 健康检查线程池 */
    private final ScheduledExecutorService healthCheckExecutor;
    /** 是否正在运行 */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /** 健康检查任务 */
    private ScheduledFuture<?> healthCheckTask;
    /** 监控指标收集器 */
    private final MetricsCollector metricsCollector = MetricsCollector.getInstance();

    /**
     * 构造函数
     *
     * @param checkInterval 检查间隔（毫秒）
     * @param connectionTimeout 连接超时时间（毫秒）
     * @param maxRetries 最大重试次数
     */
    public ConnectionHealthChecker(long checkInterval, long connectionTimeout, int maxRetries) {
        this.checkInterval = checkInterval;
        this.connectionTimeout = connectionTimeout;
        this.maxRetries = maxRetries;
        this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "connection-health-checker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 启动健康检查
     *
     * @param clientPool 客户端连接池
     * @param healthCallback 健康状态回调
     */
    public void start(Map<String, NettyRpcClient> clientPool, HealthCallback healthCallback) {
        if (running.compareAndSet(false, true)) {
            log.info("启动连接健康检查器，检查间隔：{}ms", checkInterval);
            healthCheckTask = healthCheckExecutor.scheduleWithFixedDelay(
                    () -> performHealthCheck(clientPool, healthCallback),
                    checkInterval,
                    checkInterval,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 停止健康检查
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("停止连接健康检查器");
            if (healthCheckTask != null) {
                healthCheckTask.cancel(true);
            }
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 执行健康检查
     *
     * @param clientPool 客户端连接池
     * @param healthCallback 健康状态回调
     */
    private void performHealthCheck(Map<String, NettyRpcClient> clientPool, HealthCallback healthCallback) {
        if (!running.get()) {
            return;
        }

        log.debug("开始执行连接健康检查，连接池大小：{}", clientPool.size());
        
        for (Map.Entry<String, NettyRpcClient> entry : clientPool.entrySet()) {
            String clientKey = entry.getKey();
            NettyRpcClient client = entry.getValue();
            
            try {
                boolean isHealthy = checkConnectionHealth(client);
                // 记录健康检查指标
                metricsCollector.recordHealthCheck(isHealthy);
                
                if (!isHealthy) {
                    log.warn("检测到不健康的连接：{}", clientKey);
                    healthCallback.onUnhealthyConnection(clientKey, client);
                } else {
                    log.debug("连接健康：{}", clientKey);
                    healthCallback.onHealthyConnection(clientKey, client);
                }
            } catch (Exception e) {
                log.error("健康检查异常：{}", clientKey, e);
                // 记录健康检查失败
                metricsCollector.recordHealthCheck(false);
                healthCallback.onUnhealthyConnection(clientKey, client);
            }
        }
        
        log.debug("连接健康检查完成");
    }

    /**
     * 检查单个连接的健康状态
     *
     * @param client 客户端连接
     * @return 是否健康
     */
    private boolean checkConnectionHealth(NettyRpcClient client) {
        if (client == null) {
            return false;
        }

        // 检查连接是否活跃
        if (!client.isActive()) {
            log.debug("连接不活跃");
            return false;
        }

        // 检查连接是否可写
        if (!client.isWritable()) {
            log.debug("连接不可写");
            return false;
        }

        // 可以添加更多健康检查逻辑，比如发送心跳包
        return true;
    }

    /**
     * 检查连接是否超时
     *
     * @param client 客户端连接
     * @return 是否超时
     */
    public boolean isConnectionTimeout(NettyRpcClient client) {
        if (client == null) {
            return true;
        }
        
        long lastActiveTime = client.getLastActiveTime();
        return System.currentTimeMillis() - lastActiveTime > connectionTimeout;
    }

    /**
     * 健康状态回调接口
     */
    public interface HealthCallback {
        /**
         * 连接健康时的回调
         *
         * @param clientKey 客户端键
         * @param client 客户端连接
         */
        void onHealthyConnection(String clientKey, NettyRpcClient client);

        /**
         * 连接不健康时的回调
         *
         * @param clientKey 客户端键
         * @param client 客户端连接
         */
        void onUnhealthyConnection(String clientKey, NettyRpcClient client);
    }

    /**
     * 获取健康检查器状态
     *
     * @return 是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取检查间隔
     *
     * @return 检查间隔（毫秒）
     */
    public long getCheckInterval() {
        return checkInterval;
    }

    /**
     * 获取连接超时时间
     *
     * @return 连接超时时间（毫秒）
     */
    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }
}