# RPC框架序列化器使用指南

## 概述

本框架提供了多种序列化器实现，支持不同的序列化协议，可以根据具体需求选择合适的序列化器。

## 支持的序列化器

| 序列化器 | 类型值 | 特点 | 适用场景 |
|---------|--------|------|----------|
| **Kryo** | 1 | 高性能二进制序列化，速度快 | 默认选择，通用场景 |
| **JSON** | 2 | 文本格式，可读性好 | 调试、日志、跨语言通信 |
| **Protobuf** | 3 | 高效二进制，跨语言支持 | 需要IDL定义，高性能场景 |
| **Hessian** | 4 | 二进制序列化，跨语言支持 | 兼容Hessian协议的系统 |
| **JDK** | 5 | Java原生序列化 | 简单场景，兼容性要求 |
| **ProtoStuff** | 6 | 高性能二进制，无需IDL | 高性能场景，无需proto文件 |

## 使用方法

### 1. 自动序列化协商（推荐）

框架现在支持自动序列化协商机制，客户端会自动从服务发现获取服务端的序列化类型：

```java
// 创建RPC客户端，无需指定序列化器
RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer);

// 获取服务代理，框架会自动从服务发现获取序列化类型
UserService userService = rpcClient.createService(UserService.class, "1.0");

// 如果服务端序列化类型不匹配，框架会自动尝试其他序列化类型
User user = userService.getUserById(1L);
```

### 2. 配置默认序列化器（兼容模式）

在客户端或服务端启动时设置默认序列化器：

```java
// 设置默认序列化器为JSON
SerializerFactory.setDefaultSerializerType(Serializer.SerializerType.JSON);

// 获取当前默认序列化器
Serializer defaultSerializer = SerializerFactory.getDefaultSerializer();
System.out.println("当前默认序列化器：" + defaultSerializer.getName());
```

### 3. 创建指定序列化器的RPC客户端（兼容模式）

```java
// 创建使用JSON序列化的RPC客户端
Serializer jsonSerializer = SerializerFactory.getSerializer(Serializer.SerializerType.JSON);
RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer, jsonSerializer);

// 或者使用默认序列化器
RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer);
```

### 4. 创建指定序列化器的RPC服务端

```java
// 创建使用Hessian序列化的RPC服务端
Serializer hessianSerializer = SerializerFactory.getSerializer(Serializer.SerializerType.HESSIAN);
RpcServer rpcServer = new RpcServer("localhost", 8080, serviceProvider, serviceRegistry, hessianSerializer);
```

### 5. 运行时切换序列化器

```java
// 获取所有支持的序列化器类型
byte[] supportedTypes = SerializerFactory.getSupportedTypes();
System.out.println("支持的序列化器：" + Arrays.toString(supportedTypes));

// 根据类型获取序列化器
Serializer serializer = SerializerFactory.getSerializer(Serializer.SerializerType.JSON);
```

## 性能对比

以下是各序列化器的大致性能对比（仅供参考）：

| 序列化器 | 序列化速度 | 反序列化速度 | 序列化大小 |
|---------|------------|--------------|------------|
| Kryo    | 非常快     | 非常快       | 小         |
| ProtoStuff| 非常快   | 非常快       | 很小       |
| Protobuf| 非常快     | 非常快       | 很小       |
| Hessian | 快         | 快           | 中等       |
| JSON    | 中等       | 中等         | 大         |
| JDK     | 慢         | 慢           | 大         |

## 序列化协商机制

框架内置了智能的序列化协商机制，能够自动处理客户端和服务端序列化类型不匹配的情况：

### 工作原理

1. **服务注册时**：服务端会将其使用的序列化类型注册到服务发现中心
2. **服务发现时**：客户端从服务发现中心获取服务端的序列化类型
3. **自动协商**：如果首次调用失败，框架会自动尝试其他序列化类型
4. **协商顺序**：KRYO → JSON → HESSIAN → PROTOSTUFF

### 协商日志

```
[INFO] 从服务发现获取序列化类型：服务=com.example.UserService，版本=1.0.0，分组=default，序列化类型=1
[WARN] 序列化类型 2 失败，尝试协商其他序列化类型: 序列化异常
[INFO] 尝试使用序列化类型: 1
[INFO] 序列化协商成功，使用类型: 1
```

## 注意事项

### 1. 序列化协商
- 框架会自动处理序列化类型不匹配的情况
- 建议服务端和客户端使用相同的序列化类型以获得最佳性能
- 协商过程可能会增加首次调用的延迟

### 2. Protobuf使用限制
Protobuf序列化器要求消息类必须继承`com.google.protobuf.Message`，并且需要通过`.proto`文件定义消息格式后生成对应的Java类。

### 3. JDK序列化限制
JDK序列化器要求所有被序列化的类必须实现`java.io.Serializable`接口。

### 4. ProtoStuff使用说明
ProtoStuff序列化器无需编写`.proto`文件，可以直接序列化POJO对象。但要求被序列化的类必须有默认构造函数，并且字段应该有getter/setter方法。

### 5. 兼容性考虑
- **跨语言通信**：建议使用JSON或Protobuf
- **高性能要求**：建议使用Kryo、ProtoStuff或Protobuf
- **调试方便**：建议使用JSON
- **无需IDL定义**：建议使用ProtoStuff或Kryo

### 6. 线程安全
所有序列化器实现都是线程安全的，可以在多线程环境中共享使用。

## 示例代码

### 完整示例

```java
public class SerializationExample {
    public static void main(String[] args) {
        // 1. 设置序列化器类型
        SerializerFactory.setDefaultSerializerType(Serializer.SerializerType.JSON);
        
        // 2. 创建服务注册中心
        ServiceRegistry registry = new ZookeeperServiceRegistry("localhost:2181");
        
        // 3. 创建负载均衡器
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
        
        // 4. 创建RPC客户端
        RpcClient client = new RpcClient(registry, loadBalancer);
        
        // 5. 获取服务代理
        UserService userService = client.createService(UserService.class, "1.0");
        
        // 6. 调用服务
        User user = userService.getUserById(1L);
        System.out.println("获取用户：" + user);
    }
}
```

### 测试序列化器

```java
public class SerializerTest {
    public static void main(String[] args) {
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        
        // 测试所有序列化器
        Serializer[] serializers = {
            new KryoSerializer(),
            new JsonSerializer(),
            new ProtoStuffSerializer(),
            new HessianSerializer(),
            new JdkSerializer()
        };
        
        for (Serializer serializer : serializers) {
            System.out.println("测试 " + serializer.getName() + " 序列化器：");
            
            byte[] data = serializer.serialize(user);
            System.out.println("  序列化大小：" + data.length + " bytes");
            
            User deserialized = serializer.deserialize(data, User.class);
            System.out.println("  反序列化结果：" + deserialized.getUsername());
        }
    }
}
```

## 故障排除

### 常见问题

1. **找不到序列化器**
   - 确保已注册对应的序列化器
   - 检查类型值是否正确

2. **序列化失败**
   - 检查对象是否实现了必要的接口
   - 检查对象是否包含不可序列化的字段

3. **反序列化失败**
   - 确保目标类有默认构造函数
   - 检查类路径是否一致

### 日志调试

启用调试日志查看序列化过程：

```properties
# logback.xml
<logger name="com.rpc.serialization" level="DEBUG"/>
```

## 扩展指南

### 添加自定义序列化器

1. 实现`Serializer`接口
2. 在`SerializerFactory`中注册
3. 添加对应的类型常量

```java
public class CustomSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T obj) {
        // 自定义序列化逻辑
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        // 自定义反序列化逻辑
    }
    
    @Override
    public byte getType() {
        return 6; // 自定义类型值
    }
}

// 注册自定义序列化器
SerializerFactory.registerSerializer(new CustomSerializer());
```