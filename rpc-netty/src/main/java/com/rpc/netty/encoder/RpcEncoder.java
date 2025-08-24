package com.rpc.netty.encoder;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.netty.client.RpcProtocol;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeoutException;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RpcEncoder extends MessageToByteEncoder<Object> {

    /**默认序列化类型 */
    private final byte serializationType;

    /**
     * 构造函数
     *
     * @param serializationType 序列化类型
     */
    public RpcEncoder(byte serializationType) {
        this.serializationType = serializationType;
    }
    /**
     * 默认构造函数，使用Kryo序列化
     */
    public RpcEncoder() {
        this.serializationType = 1;// 默认使用Kryo序列化
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            if(msg instanceof RpcRequest) {
                encodeRequest(ctx, (RpcRequest) msg, out);
            } else if(msg instanceof RpcResponse) {
                encodeResponse(ctx, (RpcResponse) msg, out);
            } else {
                throw new IllegalArgumentException("不支持的消息类型：" +msg.getClass().getName());
            }
        } catch (Exception e) {
            log.error("编码RPC消息失败", e);
            throw e;
        }
    }

    /**
     * 编码RPC请求
     *
     * @param ctx 通道上下文
     * @param request RPC请求
     * @param out 输出缓冲区
     * @throws Exception 编码异常
     */
    private void encodeRequest(ChannelHandlerContext ctx, RpcRequest request, ByteBuf out) throws Exception {
        // 判断是否为心跳请求
        boolean isHeartbeat = isHeartbeatRequest(request);
        byte messageType = isHeartbeat? RpcProtocol.MessageType.HEARTBEAT_REQUEST:RpcProtocol.MessageType.REQUEST;
        byte[] data;
        if(isHeartbeat) {
            // 心跳请求使用简单的字符串数据
            String heartbeatData = request.getParameters() != null && request.getParameters().length > 0?request.getParameters()[0].toString():
                    RpcProtocol.Heartbeat.HEARTBEAT_REQUEST_DATA;
            data = heartbeatData.getBytes();
        } else {
            // 普通请求进行序列化
            Serializer serializer = getSerializer();
            data = serializer.serialize(request);
        }
        // 写入协议头和数据
        writeProtocolHeader(out, messageType, RpcResponse.StatusCode.SUCCESS.getCode(), request.getRequestId(), data.length);
        out.writeBytes(data);
        if(log.isDebugEnabled()) {
            log.debug("编辑RPC请求完成：requesId={}，messageType={}，dataLength={}", request.getRequestId(), messageType, data.length);
        }
    }

    /**
     * 编码RPC响应
     *
     * @param ctx 通道上下文
     * @param response RPC响应
     * @param out 输出缓冲区
     * @throws Exception 编码异常
     */
    private void encodeResponse(ChannelHandlerContext ctx, RpcResponse response, ByteBuf out) throws Exception {
        // 判断是否为心跳响应
        boolean isHeartbeat = isHeartbeatResponse(response);
        byte messageType = isHeartbeat ? RpcProtocol.MessageType.HEARTBEAT_RESPONSE : RpcProtocol.MessageType.RESPONSE;
        // 序列化数据
        byte[] data;
        if(isHeartbeat) {
            // 心跳响应使用简单的字符串数据
            String heartbeatData = response.getResult() != null ? response.getResult().toString() : RpcProtocol.Heartbeat.HEARTBEAT_RESPONSE_DATA;
            data = heartbeatData.getBytes();
        } else {
            // 普通响应进行序列化
            Serializer serializer = getSerializer();
            data = serializer.serialize(response);
        }
        // 确定状态码
        byte statusCode = determineStatusCode(response);
        // 写入协议头和数据
        writeProtocolHeader(out, messageType, statusCode, response.getRequestId(), data.length);
        out.writeBytes(data);
        if(log.isDebugEnabled()) {
            log.debug("编码RPC响应完成：requestId={}，messageType={}，statusCode={}，dataLength={}",
                    response.getRequestId(), messageType, statusCode, data.length);
        }
    }

    /**
     * 写入协议头
     *
     * @param out 输出缓冲区
     * @param messageType 消息类型
     * @param statusCode 状态码
     * @param requestId 请求ID
     * @param dataLength 数据长度
     */
    private void writeProtocolHeader(ByteBuf out, byte messageType, byte statusCode, long requestId, int dataLength) {
        // 魔数（4字节）
        out.writeInt(RpcProtocol.MAGIC_NUMBER);
        // 版本号（1字节）
        out.writeByte(1);
        // 序列化类型（1字节）
        out.writeByte(serializationType);
        // 消息类型（1字节）
        out.writeByte(messageType);
        // 状态码（1字节）
        out.writeByte(statusCode);
        // 请求ID（8字节）
        out.writeLong(requestId);
        // 数据长度（4字节）
        out.writeInt(dataLength);
        
        log.info("发送协议头: 魔数=0x{}, 版本={}, 序列化类型={}, 消息类型={}, 状态码={}, 请求ID={}, 数据长度={}", 
                Integer.toHexString(RpcProtocol.MAGIC_NUMBER), 1, serializationType, messageType, statusCode, requestId, dataLength);
    }

    /**
     * 获取序列化器
     *
     * @return 序列化器
     * @throws IllegalStateException 序列化器不可用异常
     */
    private Serializer getSerializer() {
        Serializer serializer = SerializerFactory.getSerializer(serializationType);
        if(serializer == null) {
            throw new IllegalStateException("序列化器不可用，类型：" + serializationType);
        }
        return serializer;
    }

    /**
     * 判断是否为心跳请求
     *
     * @param request RPC请求
     * @return 是否为心跳请求
     */
    private boolean isHeartbeatRequest(RpcRequest request) {
        return "HEARTBEAT".equals(request.getInterfaceName()) && "ping".equals(request.getMethodName());
    }

    /**
     * 判断是否为心跳响应
     *
     * @param response RPC响应
     * @return 是否为心跳响应
     */
    private boolean isHeartbeatResponse(RpcResponse response) {
        return response.getResult() instanceof String && (RpcProtocol.Heartbeat.HEARTBEAT_REQUEST_DATA.equals(response.getResult()) ||
                "PONG".equals(response.getResult()));
    }

    /**
     * 从requestId字符串中提取long值
     * requestId格式：RPC_时间戳_递增数字
     *
     * @param requestId 请求ID字符串
     * @return 提取的long值
     */
    private long extractRequestIdLong(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return 0L;
        }
        try {
            // 如果requestId是纯数字，直接解析
            return Long.parseLong(requestId);
        } catch (NumberFormatException e) {
            // 如果不是纯数字，尝试从格式化字符串中提取
            // 格式：RPC_时间戳_递增数字
            String[] parts = requestId.split("_");
            if (parts.length >= 2) {
                try {
                    // 使用时间戳作为requestId
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ex) {
                    // 如果时间戳解析失败，使用hashCode
                    return Math.abs(requestId.hashCode());
                }
            }
            // 最后的备选方案：使用hashCode
            return Math.abs(requestId.hashCode());
        }
    }

    /**
     * 确定响应状态码
     *
     * @param response RPC响应
     * @return 状态码
     */
    private byte determineStatusCode(RpcResponse response) {
        if(response.getStatusCode() != null) {
            // 如果响应已经设置了状态码，直接使用
            return (byte) response.getStatusCode().getCode();
        }
        // 根据响应状态自动确定状态码
        if(response.hasException()) {
            if(response.getException() instanceof TimeoutException) {
                return RpcResponse.StatusCode.TIMEOUT.getCode();
            } else if(response.getException() instanceof IllegalArgumentException) {
                return RpcResponse.StatusCode.CLIENT_ERROR.getCode();
            } else {
                return RpcResponse.StatusCode.SERVER_ERROR.getCode();
            }
        }
        // 默认成功状态
        return RpcResponse.StatusCode.SUCCESS.getCode();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("RPC编码器发生异常：{}", ctx.channel().remoteAddress(), cause);
        // 不立即关闭连接，让上层处理器决定是否关闭
        ctx.fireExceptionCaught(cause);
    }
}
