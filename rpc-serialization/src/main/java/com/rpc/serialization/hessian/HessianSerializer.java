package com.rpc.serialization.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import com.rpc.serialization.factory.Java8TimeSerializerFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 何杰
 * @version 1.0
 * @description Hessian序列化器
 * Hessian是一个高效的二进制序列化协议，支持跨语言
 * 
 * 注意：在Java 9+环境中使用Hessian时，需要添加JVM参数：
 * --add-opens java.base/java.lang=ALL-UNNAMED
 * --add-opens java.base/java.time=ALL-UNNAMED
 */
@Slf4j
public class HessianSerializer implements Serializer {
    
    private static final SerializerFactory SERIALIZER_FACTORY;
    
    static {
        // 注册 Java 8 时间序列化器
        SERIALIZER_FACTORY =  new Java8TimeSerializerFactory();
        SERIALIZER_FACTORY.setAllowNonSerializable(true);
    }

    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Hessian2Output hessian2Output = new Hessian2Output(byteArrayOutputStream);
            hessian2Output.setSerializerFactory(SERIALIZER_FACTORY);
            
            hessian2Output.writeObject(obj);
            hessian2Output.flush();
            byte[] result = byteArrayOutputStream.toByteArray();
            log.debug("Hessian序列化对象 {} 成功，大小：{} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("Hessian序列化对象 %s 失败", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Hessian序列化对象 %s 时发生未知异常", obj.getClass().getSimpleName());
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
        
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            Hessian2Input hessian2Input = new Hessian2Input(byteArrayInputStream);
            hessian2Input.setSerializerFactory(SERIALIZER_FACTORY);
            
            T result = (T) hessian2Input.readObject(clazz);
            log.debug("Hessian反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("Hessian反序列化为 %s 失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("Hessian反序列化为 %s 时发生未知异常", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    @Override
    public byte getType() {
        return SerializerType.HESSIAN;
    }

    @Override
    public String getName() {
        return "Hessian";
    }
}