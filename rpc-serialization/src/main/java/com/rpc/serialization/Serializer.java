package com.rpc.serialization;

import com.rpc.serialization.exception.SerializationException;

/**
 * @author 何杰
 * @version 1.0
 */
public interface Serializer {

    /**
     * 序列化对象
     * 将Java对象转换为字节数组
     *
     * @param obj 待序列化的对象
     * @param <T> 对象类型
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    <T> byte[] serialize(T obj) throws SerializationException;

    /**
     * 反序列化对象
     * 将字节数组转换为Java对象
     *
     * @param data 序列化的字节数组
     * @param clazz 目标对象类型
     * @param <T> 对象类型
     * @return 反序列化后的对象
     * @throws SerializationException 反序列化异常
     */
    <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException;

    /**
     * 获取序列化器类型
     * 用于标识不同的序列化实现
     *
     * @return 序列化器类型标识
     */
    byte getType();

    /**
     * 获取序列化器名称
     *
     * @return 序列化器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 序列化器类型常量
     */
    interface SerializerType {
        byte KRYO = 1;
        byte JSON = 2;
        byte PROTOBUF = 3;
        byte HESSIAN = 4;
        byte JDK = 5;
        byte PROTOSTUFF = 6;
    }
}
