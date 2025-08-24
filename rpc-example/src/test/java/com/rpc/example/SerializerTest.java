package com.rpc.example;

import com.rpc.example.entity.User;
import com.rpc.serialization.*;
import com.rpc.serialization.exception.SerializationException;
import com.rpc.serialization.factory.SerializerFactory;
import com.rpc.serialization.hessian.HessianSerializer;
import com.rpc.serialization.jdk.JdkSerializer;
import com.rpc.serialization.json.JsonSerializer;
import com.rpc.serialization.kryo.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author 何杰
 * @version 1.0
 * @description 序列化器测试类
 */
@Slf4j
public class SerializerTest {

    private static final User testUser = new User();

    @Before
    public void setup() {
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
        testUser.setCreateTime(LocalDateTime.now());
    }

    @Test
    public void testAllSerializers() {
        List<Serializer> serializers = Arrays.asList(
                new KryoSerializer(),
                new JsonSerializer(),
                new HessianSerializer(),
                new JdkSerializer()
        );

        for (Serializer serializer : serializers) {
            log.info("测试序列化器：{} (类型: {})", serializer.getName(), serializer.getType());
            testSerializer(serializer);
        }
    }

    private void testSerializer(Serializer serializer) {
        try {
            // 测试序列化
            byte[] serializedData = serializer.serialize(testUser);
            assertNotNull(serializedData);
            assertTrue(serializedData.length > 0);

            // 测试反序列化
            User deserializedUser = serializer.deserialize(serializedData, User.class);
            assertNotNull(deserializedUser);
            assertEquals(testUser.getId(), deserializedUser.getId());
            assertEquals(testUser.getUsername(), deserializedUser.getUsername());
            assertEquals(testUser.getEmail(), deserializedUser.getEmail());
            assertEquals(testUser.getAge(), deserializedUser.getAge());

            log.info("{} 序列化器测试通过，数据大小：{} bytes", serializer.getName(), serializedData.length);
        } catch (Exception e) {
            log.error("{} 序列化器测试失败", serializer.getName(), e);
            fail(serializer.getName() + " 序列化器测试失败: " + e.getMessage());
        }
    }

    @Test
    public void testSerializerFactory() {
        log.info("测试序列化器工厂...");
        
        // 测试获取所有支持的序列化器
        byte[] supportedTypes = SerializerFactory.getSupportedTypes();
        assertTrue(supportedTypes.length >= 5);
        
        // 测试每个序列化器
        for (byte type : supportedTypes) {
            Serializer serializer = SerializerFactory.getSerializer(type);
            assertNotNull(serializer);
            log.info("找到序列化器：{} (类型: {})", serializer.getName(), type);
        }
        
        log.info("序列化器工厂测试通过");
    }

    @Test
    public void testNullHandling() {
        Serializer serializer = new JsonSerializer();
        
        // 测试空对象
        try {
            serializer.serialize(null);
            fail("应该抛出SerializationException");
        } catch (SerializationException e) {
            // 期望的异常
        }
        
        // 测试空数据
        try {
            serializer.deserialize(null, User.class);
            fail("应该抛出SerializationException");
        } catch (SerializationException e) {
            // 期望的异常
        }
        
        try {
            serializer.deserialize(new byte[0], User.class);
            fail("应该抛出SerializationException");
        } catch (SerializationException e) {
            // 期望的异常
        }
        
        // 测试空类型
        try {
            serializer.deserialize(new byte[]{1}, null);
            fail("应该抛出SerializationException");
        } catch (SerializationException e) {
            // 期望的异常
        }
    }
}