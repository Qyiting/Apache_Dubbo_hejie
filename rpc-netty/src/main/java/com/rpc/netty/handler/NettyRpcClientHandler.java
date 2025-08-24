package com.rpc.netty.handler;

import com.rpc.core.response.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class NettyRpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private final ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> pendingRequests;

    public NettyRpcClientHandler(ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        Long requestId = response.getRequestId();
        CompletableFuture<RpcResponse> future = pendingRequests.remove(requestId);
        if(future != null) {
            if(response.getException() != null) {
                future.completeExceptionally(response.getException());
            } else {
                future.complete(response);
            }
        } else {
            log.warn("收到未知请求ID的响应：{}", requestId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端处理异常", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("与服务器的连接已断开");
        // 完成所有待处理的请求并设置异常
        for(CompletableFuture<RpcResponse> future: pendingRequests.values()) {
            future.completeExceptionally(new RuntimeException("连接已断开"));
        }
        pendingRequests.clear();
    }
}
