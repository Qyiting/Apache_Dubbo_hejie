package com.rpc.serialization.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.rpc.serialization.Serializer;
import com.rpc.serialization.exception.SerializationException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class KryoSerializer implements Serializer {

    /**
     * 使用ThreadLocal确保Kryo实例的线程安全
     * Kryo不是线程安全的，每个线程需要独立的实例
     */
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // 设置引用检测，避免循环引用问题
        kryo.setReferences(true);
        return kryo;
    });
    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if(obj == null) {
            throw new SerializationException("序列化对象不能为null");
        }
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            Output output = new Output(byteArrayOutputStream);
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            kryo.writeObject(output, obj);
            output.flush();
            byte[] result = byteArrayOutputStream.toByteArray();
            log.info("序列化对象 {} 成功，大小：{} bytes", obj.getClass().getSimpleName(), result.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("序列化对象失败 %失败", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("序列化对象 %s 时发生未知异常", obj.getClass().getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);

        }
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        if(data == null || data.length == 0) {
            throw new SerializationException("反序列化数据不能为空");
        }
        if(clazz == null) {
            throw new SerializationException("目标类型不能为null");
        }
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            Input input = new Input(byteArrayInputStream);
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            T result = kryo.readObject(input, clazz);
            log.info("反序列化为 {} 成功，数据大小：{} bytes", clazz.getSimpleName(), data.length);
            return result;
        } catch (IOException e) {
            String errorMsg = String.format("反序列化为 %失败", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = String.format("反序列化为 %s 时发生未知异常", clazz.getSimpleName());
            log.error(errorMsg, e);
            throw new SerializationException(errorMsg, e);
        }
    }

    @Override
    public byte getType() {
        return SerializerType.KRYO;
    }

    @Override
    public String getName() {
        return "Kryo";
    }

    /**
     * 清理ThreadLocal资源
     * 在应用关闭时调用，避免内存泄漏
     */
    public static void clearThreadLocal() {
        KRYO_THREAD_LOCAL.remove();
        log.debug("清理KryoThreadLocal资源成功");
    }
}
