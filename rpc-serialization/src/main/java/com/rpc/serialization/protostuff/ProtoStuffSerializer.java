package com.rpc.serialization.protostuff;

import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 何杰
 * @version 1.0
 * @description ProtoStuff序列化器
 * ProtoStuff是一个高性能的序列化库，基于Google Protobuf但无需编写.proto文件
 * 支持动态Schema生成，性能优异，序列化后的数据体积小
 * 
 * 特点：
 * 1. 高性能：比JSON快很多，接近Kryo的性能
 * 2. 跨语言：基于Protobuf协议，支持多种语言
 * 3. 无需IDL：不需要编写.proto文件，直接序列化POJO
 * 4. 向后兼容：支持字段的增加和删除
 */
@Slf4j
public class ProtoStuffSerializer implements Serializer {
    
    /**
     * Schema缓存，避免重复创建Schema
     * key: 类的Class对象
     * value: 对应的Schema
     */
    private static final ConcurrentHashMap<Class<?>, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 线程本地缓冲区，避免频繁创建LinkedBuffer
     * LinkedBuffer用于序列化时的缓冲
     */
    private static final ThreadLocal<LinkedBuffer> BUFFER_THREAD_LOCAL = ThreadLocal.withInitial(() -> 
        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE)
    );
    
    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) obj.getClass();
            Schema<T> schema = getSchema(clazz);
            LinkedBuffer buffer = BUFFER_THREAD_LOCAL.get();
            
            try {
                byte[] result = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
                log.info("ProtoStuff序列化对象 {} 成功，大小：{} bytes", clazz.getSimpleName(), result.length);
                return result;
            } finally {
                // 清理缓冲区以便重用
                buffer.clear();
            }
        } catch (Exception e) {
            String errorMsg = String.format("ProtoStuff序列化对象 %s 失败", obj.getClass().getSimpleName());
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
            Schema<T> schema = getSchema(clazz);
            T obj = schema.newMessage();
            ProtostuffIOUtil.mergeFrom(data, obj, schema);
            log.info("ProtoStuff反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return obj;
        } catch (Exception e) {
            String errorMsg = String.format("ProtoStuff反序列化为 %s 失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }
    
    @Override
    public byte getType() {
        return SerializerType.PROTOSTUFF;
    }
    
    @Override
    public String getName() {
        return "ProtoStuff";
    }
    
    /**
     * 获取指定类的Schema，使用缓存避免重复创建
     * 
     * @param clazz 目标类
     * @param <T> 类型参数
     * @return Schema实例
     */
    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        return (Schema<T>) SCHEMA_CACHE.computeIfAbsent(clazz, RuntimeSchema::createFrom);
    }
    
    /**
     * 清理线程本地资源
     * 在应用关闭时调用
     */
    public static void clearThreadLocal() {
        BUFFER_THREAD_LOCAL.remove();
        log.debug("ProtoStuff序列化器线程本地资源已清理");
    }
    
    /**
     * 清理Schema缓存
     * 在应用关闭时调用
     */
    public static void clearSchemaCache() {
        SCHEMA_CACHE.clear();
        log.debug("ProtoStuff序列化器Schema缓存已清理");
    }
    
    /**
     * 获取缓存的Schema数量
     * 用于监控和调试
     * 
     * @return 缓存的Schema数量
     */
    public static int getCachedSchemaCount() {
        return SCHEMA_CACHE.size();
    }
}