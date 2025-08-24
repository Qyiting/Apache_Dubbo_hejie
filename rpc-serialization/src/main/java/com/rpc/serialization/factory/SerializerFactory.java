package com.rpc.serialization.factory;

import com.rpc.serialization.*;
import com.rpc.serialization.hessian.HessianSerializer;
import com.rpc.serialization.jdk.JdkSerializer;
import com.rpc.serialization.json.JsonSerializer;
import com.rpc.serialization.kryo.KryoSerializer;
import com.rpc.serialization.protobuf.ProtobufSerializer;
import com.rpc.serialization.protostuff.ProtoStuffSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class SerializerFactory {

    /**
     * 序列化器缓存
     * key: 序列化器类型
     * value: 序列化器实例
     */
    private static final ConcurrentHashMap<Byte, Serializer> SERIALIZER_MAP = new ConcurrentHashMap<>();

    /**
     * 默认序列化器类型
     */
    private static volatile byte defaultSerializerType = Serializer.SerializerType.KRYO;

    static {
        registerSerializer(new KryoSerializer());
        registerSerializer(new JsonSerializer());
        registerSerializer(new ProtobufSerializer());
        registerSerializer(new HessianSerializer());
        registerSerializer(new JdkSerializer());
        registerSerializer(new ProtoStuffSerializer());
        log.info("序列化器工厂初始化完成，默认序列化器：{}", getDefaultSerializer().getName());
    }

    /**
     * 注册序列化器
     *
     * @param serializer 序列化器实例
     * @throws IllegalArgumentException 如果序列化器为null或类型已存在
     */
    public static void registerSerializer(Serializer serializer) {
        if(serializer == null) {
            throw new IllegalArgumentException("序列化器不能为null");
        }
        byte type = serializer.getType();
        Serializer existingSerializer = SERIALIZER_MAP.putIfAbsent(type, serializer);
        if(existingSerializer != null) {
            log.warn("序列化器类型 {} 已存在， 忽略注册：{}", type, serializer.getName());
        } else {
            log.info("注册序列化器成功： {} 类型： {}", serializer.getName(), type);
        }
    }

    /**
     * 根据类型获取序列化器
     *
     * @param type 序列化器类型
     * @return 序列化器实例
     * @throws IllegalArgumentException 如果找不到对应类型的序列化器
     */
    public static Serializer getSerializer(byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if(serializer == null) {
            throw new IllegalArgumentException("未找到类型为 " + type + " 的序列化器");
        }
        return serializer;
    }

    /**
     * 获取默认序列化器
     *
     * @return 默认序列化器实例
     */

    public static Serializer getDefaultSerializer() {
        return getSerializer(defaultSerializerType);
    }

    /**
     * 设置默认序列化器类型
     *
     * @param type 序列化器类型
     * @throws IllegalArgumentException 如果找不到对应类型的序列化器
     */
    public static void setDefaultSerializerType(byte type) {
        getSerializer(type);
        defaultSerializerType = type;
        log.info("设置默认序列化器类型：{}", type);
    }

    /**
     * 获取默认序列化器类型
     *
     * @return 默认序列化器类型
     */
    public static byte getDefaultSerializerType() {
        return defaultSerializerType;
    }

    /**
     * 检查是否支持指定类型的序列化器
     *
     * @param type 序列化器类型
     * @return 是否支持
     */
    public static boolean isSupported(byte type) {
        return SERIALIZER_MAP.containsKey(type);
    }

    /**
     * 获取所有已注册的序列化器类型
     *
     * @return 序列化器类型数组
     */
    public static byte[] getSupportedTypes() {
        return SERIALIZER_MAP.keySet().stream()
                .mapToInt(Byte::intValue)
                .sorted()
                .collect(() -> new byte[SERIALIZER_MAP.size()],
                        (array, value) -> {
                            for (int i = 0; i < array.length; i++) {
                                if (array[i] == 0) {
                                    array[i] = (byte) value;
                                    break;
                                }
                            }
                        },
                        (array1, array2) -> {});
    }

    /**
     * 清理所有序列化器资源
     * 在应用关闭时调用
     */
    public static void clear() {
        KryoSerializer.clearThreadLocal();
        ProtoStuffSerializer.clearThreadLocal();
        ProtoStuffSerializer.clearSchemaCache();
        SERIALIZER_MAP.clear();
        log.info("序列化器工厂资源清理完成");
    }

    public static void cleanup() {
        clear();
    }

    public static String getSerializerInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("已注册的序列化器:\n");
        SERIALIZER_MAP.forEach((type, serializer) -> {
            sb.append(String.format(" 类型: %d, 名称: %s, 类: %s%s\n",
                    type, serializer.getName(), serializer.getClass().getSimpleName(), type==defaultSerializerType?" (默认)":""));
        });
        return sb.toString();
    }
}
