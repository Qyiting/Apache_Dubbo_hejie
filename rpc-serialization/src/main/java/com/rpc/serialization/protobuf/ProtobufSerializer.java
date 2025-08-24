package com.rpc.serialization.protobuf;

import com.google.protobuf.Message;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 何杰
 * @version 1.0
 * @description Protobuf序列化器
 * 注意：使用此序列化器需要消息类继承com.google.protobuf.Message
 */
@Slf4j
public class ProtobufSerializer implements Serializer {

    private static final ConcurrentHashMap<Class<?>, Method> PARSE_FROM_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        
        if (!(obj instanceof Message)) {
            throw new SerializationException("Protobuf序列化器仅支持com.google.protobuf.Message类型的对象，实际类型：" + obj.getClass().getName());
        }
        
        try {
            Message message = (Message) obj;
            byte[] result = message.toByteArray();
            log.debug("Protobuf序列化对象 {} 成功，大小：{} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (Exception e) {
            String errorMsg = String.format("Protobuf序列化对象 %s 失败", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("反序列化数据不能为空");
        }
        if (clazz == null) {
            throw new SerializationException("目标类型不能为null");
        }
        
        if (!Message.class.isAssignableFrom(clazz)) {
            throw new SerializationException("Protobuf反序列化器仅支持com.google.protobuf.Message类型的类，实际类型：" + clazz.getName());
        }
        
        try {
            Method parseFromMethod = getParseFromMethod(clazz);
            T result = (T) parseFromMethod.invoke(null, (Object) data);
            log.debug("Protobuf反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return result;
        } catch (Exception e) {
            String errorMsg = String.format("Protobuf反序列化为 %s 失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    private Method getParseFromMethod(Class<?> clazz) throws NoSuchMethodException {
        return PARSE_FROM_CACHE.computeIfAbsent(clazz, key -> {
            try {
                return clazz.getMethod("parseFrom", byte[].class);
            } catch (NoSuchMethodException e) {
                throw new SerializationException("找不到parseFrom方法，请确保类 " + clazz.getName() + " 是有效的Protobuf消息类", e);
            }
        });
    }

    @Override
    public byte getType() {
        return SerializerType.PROTOBUF;
    }

    @Override
    public String getName() {
        return "Protobuf";
    }
}