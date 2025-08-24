package com.rpc.netty.client;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.netty.handler.NettyRpcClientHandler;
import com.rpc.netty.decoder.RpcDecoder;
import com.rpc.netty.encoder.RpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class NettyRpcClient {
    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;
    private final ConcurrentHashMap<Long, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();
    /** 最后活跃时间 */
    private volatile long lastActiveTime = System.currentTimeMillis();

    public NettyRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 连接到服务器
     */
    public void connect() throws Exception{
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加长度字段处理器，协议头20字节，数据长度字段在偏移16位置，长度4字节
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(RpcProtocol.MAX_FRAME_LENGTH, 16, 4, 0, 0));
                        pipeline.addLast(new RpcEncoder());
                        pipeline.addLast(new RpcDecoder());
                        pipeline.addLast(new NettyRpcClientHandler(pendingRequests));
                    }
                });
        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
        updateLastActiveTime();
        log.info("连接到RPC服务器：{}:{}", host, port);
    }

    /**
     * 连接到服务器的重载方法
     */
    public void connect(byte serializationType) throws Exception{
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加长度字段处理器，协议头20字节，数据长度字段在偏移16位置，长度4字节
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(RpcProtocol.MAX_FRAME_LENGTH, 16, 4, 0, 0));
                        pipeline.addLast(new RpcEncoder(serializationType));
                        pipeline.addLast(new RpcDecoder());
                        pipeline.addLast(new NettyRpcClientHandler(pendingRequests));
                    }
                });
        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
        updateLastActiveTime();
        log.info("连接到RPC服务器：{}:{}", host, port);
    }

    /**
     * 发送RPC请求
     */
    public RpcResponse sendRequest(RpcRequest request, long timeout) throws Exception {
        if(!isActive()) {
            throw new RuntimeException("客户带连接不活跃");
        }
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);
        channel.writeAndFlush(request).addListener((ChannelFutureListener) channelFuture -> {
            if(!channelFuture.isSuccess()) {
                pendingRequests.remove(request.getRequestId());
                future.completeExceptionally(channelFuture.cause());
            } else {
                updateLastActiveTime();
            }
        });
        return future.get(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查连接是否活跃
     */
    public boolean isActive() {
        return channel != null && channel.isActive();
    }
    
    /**
     * 检查连接是否可写
     */
    public boolean isWritable() {
        return channel != null && channel.isWritable();
    }
    
    /**
     * 获取最后活跃时间
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }
    
    /**
     * 更新最后活跃时间
     */
    private void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
    
    /**
     * 关闭客户端
     */
    public void close() throws Exception {
        if(channel != null) {
            channel.close().sync();
        }
        if(group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        log.info("RPC客户端已关闭：{}：{}", host, port);
    }

}
