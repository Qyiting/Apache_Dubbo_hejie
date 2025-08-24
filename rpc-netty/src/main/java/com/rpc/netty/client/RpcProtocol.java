package com.rpc.netty.client;

/**
 * @author 何杰
 * @version 1.0
 * RPC协议常量定义
 * 定义了RPC通信协议的格式和常量
 *
 * 协议格式:
 * +-------+--------+--------+--------+--------+--------+--------+--------+--------+
 * | 魔数  | 版本号 | 序列化 | 消息类型| 状态码 | 请求ID |  数据长度 |      数据内容      |
 * | 4byte | 1byte  | 1byte  | 1byte  | 1byte  | 8byte  |  4byte   |     variable      |
 * +-------+--------+--------+--------+--------+--------+--------+--------+--------+
 *
 */
public class RpcProtocol {
    /**
     * 协议魔数，用于识别RPC协议
     * 0x52504300 = "RPC\0"
     */
    public static final int MAGIC_NUMBER = 0x52504300;

    /**
     * 协议版本号
     */
    public static final byte VERSION = 1;

    /**
     * 协议头长度（固定部分）
     * 魔数(4) + 版本(1) + 序列化类型(1) + 消息类型(1) + 状态码(1) + 请求ID(8) + 数据长度(4) = 20字节
     */
    public static final int HEADER_LENGTH = 20;

    /**
     * 最大帧长度（16MB）
     */
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;

    /**
     * 消息类型
     */
    public static final class MessageType {
        /** 请求消息 */
        public static final byte REQUEST = 1;
        /** 响应消息 */
        public static final byte RESPONSE = 2;
        /** 心跳请求消息 */
        public static final byte HEARTBEAT_REQUEST = 3;
        /** 心跳响应消息 */
        public static final byte HEARTBEAT_RESPONSE = 4;
    }

    /**
     * 心跳相关常量
     */
    public static final class Heartbeat {
        /** 心跳间隔（秒） */
        public static final int HEARTBEAT_INTERVAL = 30;
        /** 读超时时间（秒） */
        public static final int READ_TIMEOUT = 60;
        /** 写超时时间（秒） */
        public static final int WRITE_TIMEOUT = 30;
        /** 心跳请求数据 */
        public static final String HEARTBEAT_REQUEST_DATA = "PING";
        /** 心跳响应数据 */
        public static final String HEARTBEAT_RESPONSE_DATA = "PONG";
    }

    /**
     * 网络相关常量
     */
    public static final class Network {
        /** 默认连接超时时间（毫秒） */
        public static final int DEFAULT_CONNECT_TIMEOUT = 5000;
        /** 默认读超时时间（毫秒） */
        public static final int DEFAULT_REQUEST_TIMEOUT = 10000;
        /** 默认服务端口 */
        public static final int DEFAULT_PORT = 9999;
        /** 默认IO线程数 */
        public static final int DEFAULT_TO_THREADS = Runtime.getRuntime().availableProcessors() * 2;
        /** 默认工作线程数 */
        public static final int DEFAULT_WORKER_THREADS = 200;
    }

    /**
     * 私有构造函数，防止实例化
     */
    private RpcProtocol() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }

    /**
     * 检查魔数是否有效
     *
     * @param magicNumber 魔数
     * @return 是否有效
     */
    public static boolean isValidMagicNumber(int magicNumber) {
        return MAGIC_NUMBER == magicNumber;
    }

    /**
     * 检查版本是否支持
     *
     * @param version 版本号
     * @return 是否支持
     */
    public static boolean isSupportedVersion(byte version) {
        return VERSION == version;
    }

    /**
     * 检查消息类型是否有效
     *
     * @param messageType 消息类型
     * @return 是否有效
     */
    public static boolean isValidMessageType(byte messageType) {
        return messageType == MessageType.REQUEST || messageType == MessageType.RESPONSE ||
                messageType == MessageType.HEARTBEAT_REQUEST || messageType == MessageType.HEARTBEAT_RESPONSE;
    }

    /**
     * 检查是否为心跳消息
     *
     * @param messageType 消息类型
     * @return 是否为心跳消息
     */
    public static boolean isHeartbeatMessage(byte messageType) {
        return messageType == MessageType.HEARTBEAT_REQUEST ||
                messageType == MessageType.HEARTBEAT_RESPONSE;
    }
}
