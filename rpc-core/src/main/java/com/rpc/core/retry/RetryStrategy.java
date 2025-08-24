package com.rpc.core.retry;

import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 重试策略组件
 * 针对不同类型的异常采用不同的重试策略
 *
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RetryStrategy {
    
    /**
     * 重试配置
     */
    public static class RetryConfig {
        private final int maxRetries;
        private final long baseDelayMs;
        private final long maxDelayMs;
        private final double backoffMultiplier;
        private final boolean enableJitter;
        
        public RetryConfig(int maxRetries, long baseDelayMs, long maxDelayMs, double backoffMultiplier, boolean enableJitter) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.backoffMultiplier = backoffMultiplier;
            this.enableJitter = enableJitter;
        }
        
        public int getMaxRetries() { return maxRetries; }
        public long getBaseDelayMs() { return baseDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public boolean isEnableJitter() { return enableJitter; }
    }
    
    /**
     * 异常类型枚举
     */
    public enum ExceptionType {
        CONNECTION_EXCEPTION,    // 连接异常
        TIMEOUT_EXCEPTION,      // 超时异常
        BUSINESS_EXCEPTION,     // 业务异常
        UNKNOWN_EXCEPTION       // 未知异常
    }
    
    // 不同异常类型的重试配置
    private static final RetryConfig CONNECTION_RETRY_CONFIG = new RetryConfig(3, 1000, 8000, 2.0, true);
    private static final RetryConfig TIMEOUT_RETRY_CONFIG = new RetryConfig(2, 500, 2000, 1.5, false);
    private static final RetryConfig BUSINESS_RETRY_CONFIG = new RetryConfig(1, 100, 100, 1.0, false);
    private static final RetryConfig UNKNOWN_RETRY_CONFIG = new RetryConfig(2, 1000, 4000, 2.0, true);
    
    /**
     * 根据异常类型获取重试配置
     *
     * @param exceptionType 异常类型
     * @return 重试配置
     */
    public static RetryConfig getRetryConfig(ExceptionType exceptionType) {
        switch (exceptionType) {
            case CONNECTION_EXCEPTION:
                return CONNECTION_RETRY_CONFIG;
            case TIMEOUT_EXCEPTION:
                return TIMEOUT_RETRY_CONFIG;
            case BUSINESS_EXCEPTION:
                return BUSINESS_RETRY_CONFIG;
            case UNKNOWN_EXCEPTION:
            default:
                return UNKNOWN_RETRY_CONFIG;
        }
    }
    
    /**
     * 判断异常类型
     *
     * @param exception 异常
     * @return 异常类型
     */
    public static ExceptionType classifyException(Throwable exception) {
        if (exception instanceof ConnectException ||
            exception instanceof ClosedChannelException ||
            exception.getMessage() != null && exception.getMessage().contains("连接")) {
            return ExceptionType.CONNECTION_EXCEPTION;
        }
        
        if (exception instanceof TimeoutException ||
            exception instanceof SocketTimeoutException ||
            exception.getMessage() != null && exception.getMessage().contains("超时")) {
            return ExceptionType.TIMEOUT_EXCEPTION;
        }
        
        // 业务异常通常是RPC调用成功但业务逻辑返回异常
        if (exception.getClass().getPackage() != null &&
            exception.getClass().getPackage().getName().startsWith("com.rpc") &&
            !(exception instanceof RuntimeException)) {
            return ExceptionType.BUSINESS_EXCEPTION;
        }
        
        return ExceptionType.UNKNOWN_EXCEPTION;
    }
    
    /**
     * 执行带重试的操作
     *
     * @param operation 要执行的操作
     * @param serviceName 服务名称（用于日志）
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 最终异常
     */
    public static <T> T executeWithRetry(Supplier<T> operation, String serviceName) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= getMaxRetries(); attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt == getMaxRetries()) {
                    log.error("重试{}次后仍然失败，服务：{}", getMaxRetries(), serviceName, e);
                    break;
                }
                
                ExceptionType exceptionType = classifyException(e);
                RetryConfig config = getRetryConfig(exceptionType);
                
                if (attempt >= config.getMaxRetries()) {
                    log.warn("达到{}类型异常的最大重试次数{}，服务：{}", exceptionType, config.getMaxRetries(), serviceName);
                    break;
                }
                
                long delay = calculateDelay(config, attempt);
                log.warn("第{}次重试失败，异常类型：{}，{}ms后重试，服务：{}", 
                        attempt + 1, exceptionType, delay, serviceName, e);
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试被中断", ie);
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * 计算延迟时间
     *
     * @param config 重试配置
     * @param attempt 当前重试次数
     * @return 延迟时间（毫秒）
     */
    private static long calculateDelay(RetryConfig config, int attempt) {
        long delay = (long) (config.getBaseDelayMs() * Math.pow(config.getBackoffMultiplier(), attempt));
        delay = Math.min(delay, config.getMaxDelayMs());
        
        if (config.isEnableJitter()) {
            // 添加随机抖动，避免雪崩效应
            double jitter = 0.1 * Math.random(); // 10%的随机抖动
            delay = (long) (delay * (1 + jitter));
        }
        
        return delay;
    }
    
    /**
     * 获取全局最大重试次数
     *
     * @return 最大重试次数
     */
    private static int getMaxRetries() {
        return Math.max(Math.max(CONNECTION_RETRY_CONFIG.getMaxRetries(), TIMEOUT_RETRY_CONFIG.getMaxRetries()),
                       Math.max(BUSINESS_RETRY_CONFIG.getMaxRetries(), UNKNOWN_RETRY_CONFIG.getMaxRetries()));
    }
    
    /**
     * 判断异常是否可重试
     *
     * @param exception 异常
     * @return 是否可重试
     */
    public static boolean isRetryable(Throwable exception) {
        ExceptionType type = classifyException(exception);
        RetryConfig config = getRetryConfig(type);
        return config.getMaxRetries() > 0;
    }
}