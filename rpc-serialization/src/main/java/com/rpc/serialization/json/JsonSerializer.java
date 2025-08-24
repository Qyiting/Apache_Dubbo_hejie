package com.rpc.serialization.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class JsonSerializer implements Serializer {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_LONG_FOR_INTS, true);
    }

    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        try {
            byte[] result = OBJECT_MAPPER.writeValueAsBytes(obj);
            log.debug("JSON序列化对象 {} 成功，大小：{} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JSON序列化对象 %s 失败", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("JSON序列化对象 %s 时发生未知异常", obj.getClass().getSimpleName());
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
        try {
            T result = OBJECT_MAPPER.readValue(data, clazz);
            log.debug("JSON反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JSON反序列化为 %s 失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("JSON反序列化为 %s 时发生未知异常", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    @Override
    public byte getType() {
        return SerializerType.JSON;
    }

    @Override
    public String getName() {
        return "JSON";
    }
}