package com.rpc.serialization.jdk;

import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author 何杰
 * @version 1.0
 * @description JDK原生序列化器
 * 使用Java内置的序列化机制，兼容性好但性能较低
 */
@Slf4j
public class JdkSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        
        if (!(obj instanceof Serializable)) {
            throw new SerializationException("JDK序列化器要求对象必须实现java.io.Serializable接口，实际类型：" + obj.getClass().getName());
        }
        
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            byte[] result = byteArrayOutputStream.toByteArray();
            log.debug("JDK序列化对象 {} 成功，大小：{} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JDK序列化对象 %s 失败", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("JDK序列化对象 %s 时发生未知异常", obj.getClass().getSimpleName());
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
        
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            
            Object obj = objectInputStream.readObject();
            
            if (!clazz.isInstance(obj)) {
                throw new SerializationException(String.format("反序列化后的对象类型不匹配，期望类型：%s，实际类型：%s", 
                        clazz.getName(), obj.getClass().getName()));
            }
            
            T result = clazz.cast(obj);
            log.debug("JDK反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("JDK反序列化为 %s 失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (ClassNotFoundException e) {
            String errorMsg = String.format("JDK反序列化时找不到类 %s", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("JDK反序列化为 %s 时发生未知异常", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    @Override
    public byte getType() {
        return SerializerType.JDK;
    }

    @Override
    public String getName() {
        return "JDK";
    }
}