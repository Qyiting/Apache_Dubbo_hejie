package com.rpc.netty.decoder;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.netty.client.RpcProtocol;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.factory.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的字节来读取协议头
        if(in.readableBytes() < RpcProtocol.HEADER_LENGTH) {
            return; // 等待更多数据
        }
        // 标记当前读取位置
        in.markReaderIndex();
        try {
            // 读取协议头但不移动读取位置
            int magicNumber = in.readInt();
            byte version = in.readByte();
            byte serializationType = in.readByte();
            ctx.channel().attr(AttributeKey.valueOf("serializationType")).set(serializationType);
            byte messageType = in.readByte();
            byte statusCode = in.readByte();
            long requestId = in.readLong();
            int dataLength = in.readInt();
            log.info("接收到协议头: 魔数=0x{}, 版本={}, 序列化类型={}, 消息类型={}, 状态码={}, 请求ID={}, 数据长度={}", 
                    Integer.toHexString(magicNumber), version, serializationType, messageType, statusCode, requestId, dataLength);
            // 检查是否有足够的数据
            if(in.readableBytes() < dataLength) {
                in.resetReaderIndex(); // 重置读取位置
                return; // 等待更多数据
            }
            // 重置读取位置，重新读取完整帧
            in.resetReaderIndex();
            ByteBuf frame = in.readBytes(RpcProtocol.HEADER_LENGTH + dataLength);
            // 解码帧数据
            Object decoded = decodeFrame(frame);
            if(decoded != null) {
                out.add(decoded);
            }
        } catch (Exception e) {
            in.resetReaderIndex();
            log.error("解码RPC消息失败", e);
            throw e;
        }
    }

    /**
     * 解码帧数据
     *
     * @param frame 帧数据
     * @return 解码后的对象
     * @throws Exception 解码异常
     */
    protected Object decodeFrame(ByteBuf frame) throws Exception {
        // 检查帧长度
        if(frame.readableBytes() < RpcProtocol.HEADER_LENGTH) {
            throw new IllegalArgumentException("帧长度不足，无法解析协议头");
        }
        // 读取协议头
        int magicNumber = frame.readInt();
        byte version = frame.readByte();
        byte serializationType = frame.readByte();
        byte messageType = frame.readByte();
        byte statusCode = frame.readByte();
        long requestId = frame.readLong();
        int dataLength = frame.readInt();
        // 验证协议头
        validateProtocolHeader(magicNumber, version, messageType, dataLength, frame.readableBytes());
        // 处理心跳消息
        if(RpcProtocol.isHeartbeatMessage(messageType)) {
            return createHeartbeatMessage(messageType, requestId, frame, dataLength);
        }
        // 读取数据内容
        byte[] data = new byte[dataLength];
        frame.readBytes(data);
        // 获取反序列化器并反序列化
        Serializer serializer = SerializerFactory.getSerializer(serializationType);
        if(serializer == null) {
            throw new IllegalArgumentException("不支持的序列化类型：" + serializationType);
        }
        // 根据消息类型反序列化
        if(messageType == RpcProtocol.MessageType.REQUEST) {
            RpcRequest request = serializer.deserialize(data, RpcRequest.class);
            request.setRequestId(requestId);
            return request;
        } else if(messageType == RpcProtocol.MessageType.RESPONSE) {
            RpcResponse response = serializer.deserialize(data, RpcResponse.class);
            response.setRequestId(requestId);
            // 根据状态码设置响应状态
            if(statusCode == RpcResponse.StatusCode.SUCCESS.getCode()) {
                response.setStatusCode(RpcResponse.StatusCode.SUCCESS);
            } else {
                response.setStatusCode(RpcResponse.StatusCode.FAIL);
            }
            return response;
        } else {
            throw new IllegalArgumentException("未知的消息类型："+ messageType);
        }
    }

    /**
     * 验证协议头
     *
     * @param magicNumber 魔数
     * @param version 版本号
     * @param messageType 消息类型
     * @param dataLength 数据长度
     * @param readableBytes 可读字节数
     * @throws IllegalArgumentException 验证失败异常
     */
    protected void validateProtocolHeader(int magicNumber, byte version, byte messageType, int dataLength, int readableBytes) throws IllegalArgumentException {
        // 检查魔数
        if(!RpcProtocol.isValidMagicNumber(magicNumber)) {
            throw new IllegalArgumentException(String.format("无效的魔数：0x%08x，期望：0x%08x", magicNumber, RpcProtocol.MAGIC_NUMBER));
        }
        // 检查版本号
        if(!RpcProtocol.isSupportedVersion(version)) {
            throw new IllegalArgumentException(String.format("不支持的协议版本：%d，期望：%d", version, RpcProtocol.VERSION));
        }
        // 检查消息类型
        if(!RpcProtocol.isValidMessageType(messageType)) {
            throw new IllegalArgumentException("无效的消息类型：" + messageType);
        }
        // 验证数据长度
        if(dataLength < 0) {
            throw new IllegalArgumentException("数据长度不能为负数：" + dataLength);
        }
        if(dataLength > readableBytes) {
            throw new IllegalArgumentException(String.format("数据长度(%d)超过可读字节数(%d)", dataLength, readableBytes));
        }
    }

    /**
     * 创建心跳消息
     *
     * @param messageType 消息类型
     * @param requestId 请求ID
     * @param frame 帧数据
     * @param dataLength 数据长度
     * @return 心跳消息对象
     */
    protected Object createHeartbeatMessage(byte messageType, long requestId, ByteBuf frame, int dataLength) {
        // 读取心跳数据（如果有）
        String heartbeatData = null;
        if(dataLength > 0) {
            byte[] data = new byte[dataLength];
            frame.readBytes(data);
            heartbeatData = new String(data);
        }
        if(messageType == RpcProtocol.MessageType.HEARTBEAT_REQUEST) {
            // 创建心跳请求
            RpcRequest heartbeatRequest = new RpcRequest();
            heartbeatRequest.setRequestId(requestId);
            heartbeatRequest.setInterfaceName("HEARTBEAT");
            heartbeatRequest.setMethodName("ping");
            heartbeatRequest.setParameters(heartbeatData != null?new Object[]{heartbeatData}:new Object[0]);
            return heartbeatRequest;
        } else {
            // 创建心跳响应
            RpcResponse heartbeatResponse = RpcResponse.success(requestId, heartbeatData);
            return heartbeatResponse;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("RPC解码器发生异常，关闭连接: {}", ctx.channel().remoteAddress(), cause);
        // 对于协议解析异常，直接关闭连接
        ctx.close();
    }
}
