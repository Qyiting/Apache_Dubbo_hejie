package com.hejiexmu.rpc.spring.boot.config;

import com.rpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RPC服务器自动启动器
 * 在Spring Boot应用启动完成后自动启动RpcServer
 * 
 * @author hejiexmu
 */
@Slf4j
public class RpcServerAutoStarter implements ApplicationRunner {
    
    private final RpcServer rpcServer;
    
    public RpcServerAutoStarter(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("RpcServerAutoStarter开始执行...");
        if (rpcServer != null) {
            try {
                log.info("正在自动启动RPC服务器...");
                rpcServer.start();
                log.info("RPC服务器自动启动成功！");
            } catch (Exception e) {
                log.error("RPC服务器自动启动失败", e);
                throw e;
            }
        } else {
            log.warn("RpcServer bean未找到，跳过自动启动");
        }
        log.info("RpcServerAutoStarter执行完成");
    }
}