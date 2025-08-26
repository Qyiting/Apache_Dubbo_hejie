package com.hejiexmu.rpc.samples.provider.config;

import com.hejiexmu.rpc.samples.provider.handler.RealTimeMonitoringHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 用于实时数据推送功能
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private RealTimeMonitoringHandler realTimeMonitoringHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册实时监控WebSocket处理器
        registry.addHandler(realTimeMonitoringHandler, "/ws/monitoring")
                .setAllowedOrigins("*"); // 在生产环境中应该限制允许的源
    }
}