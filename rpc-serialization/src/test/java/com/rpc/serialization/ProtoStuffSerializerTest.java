package com.rpc.serialization;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.response.RpcResponse;
import com.rpc.serialization.exception.SerializationException;
import com.rpc.serialization.protostuff.ProtoStuffSerializer;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * ProtoStuff序列化器测试
 */
public class ProtoStuffSerializerTest {

    private ProtoStuffSerializer serializer;
    
    public ProtoStuffSerializerTest() {
        // 默认构造函数
    }

    @Before
    public void setUp() {
        serializer = new ProtoStuffSerializer();
    }

    @Test
    public void testSerializeAndDeserializeRpcRequest() throws IOException {
        // 创建测试对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(123L);
        request.setInterfaceName("com.example.TestService");
        request.setMethodName("testMethod");
        request.setParameterTypes(new Class[]{String.class, Integer.class});
        request.setParameters(new Object[]{"hello", 42});
        request.setVersion("1.0");
        request.setGroup("default");

        // 序列化
        byte[] bytes = serializer.serialize(request);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // 反序列化
        RpcRequest deserializedRequest = serializer.deserialize(bytes, RpcRequest.class);
        assertNotNull(deserializedRequest);
        assertEquals(request.getRequestId(), deserializedRequest.getRequestId());
        assertEquals(request.getInterfaceName(), deserializedRequest.getInterfaceName());
        assertEquals(request.getMethodName(), deserializedRequest.getMethodName());
        assertEquals(request.getVersion(), deserializedRequest.getVersion());
        assertEquals(request.getGroup(), deserializedRequest.getGroup());
        assertArrayEquals(request.getParameterTypes(), deserializedRequest.getParameterTypes());
        assertArrayEquals(request.getParameters(), deserializedRequest.getParameters());
    }

    @Test
    public void testSerializeAndDeserializeRpcResponse() throws IOException {
        // 创建测试对象
        RpcResponse response = new RpcResponse();
        response.setRequestId(123L);
        response.setStatusCode(RpcResponse.StatusCode.SUCCESS);
        response.setMessage("Success");
        response.setResult("Hello World");

        // 序列化
        byte[] bytes = serializer.serialize(response);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // 反序列化
        @SuppressWarnings("unchecked")
        RpcResponse deserializedResponse = serializer.deserialize(bytes, RpcResponse.class);
        assertNotNull(deserializedResponse);
        assertEquals(Long.valueOf(123L), deserializedResponse.getRequestId());
        assertEquals(response.getStatusCode(), deserializedResponse.getStatusCode());
        assertEquals(response.getMessage(), deserializedResponse.getMessage());
        assertEquals(response.getResult(), deserializedResponse.getResult());
    }

    @Test
    public void testSerializeAndDeserializeSimpleObject() throws IOException {
        // 创建简单测试对象
        TestObject obj = new TestObject();
        obj.setId(123L);
        obj.setName("Test Name");
        obj.setActive(true);
        obj.setTags(Arrays.asList("tag1", "tag2", "tag3"));
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", 42);
        obj.setProperties(properties);

        // 序列化
        byte[] bytes = serializer.serialize(obj);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // 反序列化
        TestObject deserializedObj = serializer.deserialize(bytes, TestObject.class);
        assertNotNull(deserializedObj);
        assertEquals(obj.getId(), deserializedObj.getId());
        assertEquals(obj.getName(), deserializedObj.getName());
        assertEquals(obj.isActive(), deserializedObj.isActive());
        assertEquals(obj.getTags(), deserializedObj.getTags());
        assertEquals(obj.getProperties(), deserializedObj.getProperties());
    }

    @Test(expected = SerializationException.class)
    public void testSerializeNull() throws IOException {
        serializer.serialize(null);
    }

    @Test(expected = SerializationException.class)
    public void testDeserializeNullBytes() throws IOException {
        serializer.deserialize(null, String.class);
    }

    @Test(expected = SerializationException.class)
    public void testDeserializeEmptyBytes() throws IOException {
        serializer.deserialize(new byte[0], String.class);
    }

    @Test
    public void testGetType() {
        assertEquals(6, serializer.getType());
    }

    @Test
    public void testGetName() {
        assertEquals("ProtoStuff", serializer.getName());
    }

    @Test
    public void testPerformance() throws IOException {
        // 创建测试对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(999L);
        request.setInterfaceName("com.example.PerformanceTestService");
        request.setMethodName("performanceTest");
        request.setParameterTypes(new Class[]{String.class});
        request.setParameters(new Object[]{"performance test data"});
        request.setVersion("1.0");
        request.setGroup("default");

        // 性能测试
        long startTime = System.currentTimeMillis();
        int iterations = 10000;
        
        for (int i = 0; i < iterations; i++) {
            byte[] bytes = serializer.serialize(request);
            RpcRequest deserializedRequest = serializer.deserialize(bytes, RpcRequest.class);
            assertNotNull(deserializedRequest);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("ProtoStuff序列化性能测试:");
        System.out.println("迭代次数: " + iterations);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均耗时: " + (duration * 1.0 / iterations) + "ms/次");
        
        // 性能应该在合理范围内（这里设置一个宽松的限制）
        assertTrue("ProtoStuff序列化性能测试超时", duration < 5000);
    }

    @Test
    public void testSchemaCache() throws IOException {
        // 测试Schema缓存功能
        TestObject obj1 = new TestObject();
        obj1.setId(1L);
        obj1.setName("Object 1");
        
        TestObject obj2 = new TestObject();
        obj2.setId(2L);
        obj2.setName("Object 2");

        // 第一次序列化，会创建Schema
        byte[] bytes1 = serializer.serialize(obj1);
        TestObject result1 = serializer.deserialize(bytes1, TestObject.class);
        assertEquals(obj1.getId(), result1.getId());
        assertEquals(obj1.getName(), result1.getName());

        // 第二次序列化同类型对象，应该使用缓存的Schema
        byte[] bytes2 = serializer.serialize(obj2);
        TestObject result2 = serializer.deserialize(bytes2, TestObject.class);
        assertEquals(obj2.getId(), result2.getId());
        assertEquals(obj2.getName(), result2.getName());

        // 验证Schema缓存数量
        assertTrue(serializer.getCachedSchemaCount() > 0);
    }

    @Test
    public void testCleanup() throws IOException {
        // 测试资源清理
        TestObject obj = new TestObject();
        obj.setId(1L);
        obj.setName("Test");
        
        // 执行序列化操作
        byte[] bytes = serializer.serialize(obj);
        TestObject result = serializer.deserialize(bytes, TestObject.class);
        assertNotNull(result);
        
        // 清理资源
        ProtoStuffSerializer.clearThreadLocal();
        ProtoStuffSerializer.clearSchemaCache();
        
        // 清理后应该仍然能正常工作
        byte[] bytes2 = serializer.serialize(obj);
        TestObject result2 = serializer.deserialize(bytes2, TestObject.class);
        assertEquals(obj.getId(), result2.getId());
        assertEquals(obj.getName(), result2.getName());
    }
}