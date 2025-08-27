# DistributedX 快速开始指南

## 环境要求

- **JDK**: 1.8+
- **Maven**: 3.6+
- **Zookeeper**: 3.6+
- **操作系统**: Windows/Linux/MacOS

## 快速体验

### 1. 项目结构

```
DistributedX/
├── dx-core/           # 核心接口和工具类
├── dx-client/         # 客户端实现
├── dx-server/         # 服务端实现
├── dx-registry/       # 注册中心实现
├── dx-serialization/  # 序列化实现
├── dx-netty/         # 网络通信实现
└── dx-example/       # 使用示例
```

### 2. 依赖配置

在你的项目中添加DistributedX依赖：

```xml
<dependencies>
    <!-- DistributedX 核心依赖 -->
    <dependency>
        <groupId>com.distributedx</groupId>
        <artifactId>dx-core</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- 客户端依赖 -->
    <dependency>
        <groupId>com.distributedx</groupId>
        <artifactId>dx-client</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- 服务端依赖 -->
    <dependency>
        <groupId>com.distributedx</groupId>
        <artifactId>dx-server</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## 服务提供者示例

### 1. 定义服务接口

```java
public interface UserService {
    User getUserById(Long id);
    List<User> getAllUsers();
    boolean saveUser(User user);
    boolean updateUser(User user);
    boolean deleteUser(Long id);
}
```

### 2. 实现服务接口

```java
@DxService(serviceName = "userService", version = "1.0")
public class UserServiceImpl implements UserService {
    
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    
    @Override
    public User getUserById(Long id) {
        log.info("获取用户信息，ID: {}", id);
        return userDatabase.get(id);
    }
    
    @Override
    public List<User> getAllUsers() {
        log.info("获取所有用户信息");
        return new ArrayList<>(userDatabase.values());
    }
    
    @Override
    public boolean saveUser(User user) {
        log.info("保存用户信息: {}", user);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userDatabase.put(user.getId(), user);
        return true;
    }
    
    @Override
    public boolean updateUser(User user) {
        log.info("更新用户信息: {}", user);
        User existingUser = userDatabase.get(user.getId());
        if (existingUser != null) {
            user.setUpdateTime(LocalDateTime.now());
            userDatabase.put(user.getId(), user);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean deleteUser(Long id) {
        log.info("删除用户，ID: {}", id);
        return userDatabase.remove(id) != null;
    }
}
```

### 3. 启动服务提供者

```java
@SpringBootApplication
@EnableDxServer
public class ProviderApplication {
    
    public static void main(String[] args) {
        // 配置服务器
        DxServerConfig config = DxServerConfig.builder()
            .host("localhost")
            .port(9999)
            .registryAddress("127.0.0.1:2181")
            .serializer(SerializerType.KRYO)
            .build();
            
        // 创建并启动服务器
        DxServer server = new DxNettyServer(config);
        
        // 注册服务
        server.publishService(new UserServiceImpl(), UserService.class);
        
        // 启动服务器
        server.start();
        
        log.info("DistributedX服务提供者启动成功，端口: {}", config.getPort());
    }
}
```

## 服务消费者示例

### 1. 配置客户端

```java
@Configuration
@EnableDxClient
public class DxClientConfig {
    
    @Bean
    public DxClientConfiguration dxClientConfiguration() {
        return DxClientConfiguration.builder()
            .registryAddress("127.0.0.1:2181")
            // 注意：不再需要配置序列化器，框架会自动从服务发现获取
            // .serializer(SerializerType.KRYO)  // 已废弃
            .loadBalancer(LoadBalancerType.CONSISTENT_HASH)
            .timeout(5000)
            .retryCount(3)
            .build();
    }
}
```

### 2. 注入和使用服务

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @DxReference(serviceName = "userService", version = "1.0")
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("获取用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) {
        try {
            boolean success = userService.saveUser(user);
            return success ? ResponseEntity.ok("用户创建成功") : 
                           ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("用户创建失败");
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("创建用户失败");
        }
    }
}
```

### 3. 启动服务消费者

```java
@SpringBootApplication
public class ConsumerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
        log.info("DistributedX服务消费者启动成功");
    }
}
```

## 配置说明

### 1. 服务端配置

```yaml
# application.yml
distributedx:
  server:
    host: localhost
    port: 9999
    registry:
      address: 127.0.0.1:2181
      timeout: 5000
    serializer: kryo  # json, kryo, hessian
    thread-pool:
      core-size: 10
      max-size: 200
      queue-capacity: 1000
```

### 2. 客户端配置

```yaml
# application.yml
distributedx:
  client:
    registry:
      address: 127.0.0.1:2181
      timeout: 5000
    # 注意：客户端不再需要配置序列化器，框架会自动从服务发现获取
    # serializer: kryo  # 已废弃，框架支持自动序列化协商
    load-balancer: consistent_hash  # random, round_robin, consistent_hash
    timeout: 5000
    retry-count: 3
    connection-pool:
      max-connections: 50
      max-idle-time: 300000
```

### 3. 自动序列化协商（推荐）

框架现在支持智能序列化协商机制：

- **服务端**：只需配置序列化器类型，框架会自动注册到服务发现
- **客户端**：无需配置序列化器，框架会自动从服务发现获取服务端的序列化类型
- **协商机制**：如果客户端和服务端序列化类型不匹配，框架会自动尝试多种序列化方式

```java
// 客户端代码 - 无需关心序列化类型
@DxReference(serviceName = "userService", version = "1.0")
private UserService userService; // 框架会自动处理序列化协商
```

## 高级特性

### 1. 自定义负载均衡策略

```java
@Component
public class WeightedRandomLoadBalancer implements LoadBalancer {
    
    @Override
    public String getType() {
        return "weighted_random";
    }
    
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        // 实现加权随机负载均衡逻辑
        Map<InetSocketAddress, Integer> weights = getServiceWeights(addresses);
        return selectByWeight(weights);
    }
    
    private InetSocketAddress selectByWeight(Map<InetSocketAddress, Integer> weights) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        
        int currentWeight = 0;
        for (Map.Entry<InetSocketAddress, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }
}
```

### 2. 自定义序列化器

```java
@Component
public class ProtobufSerializer implements Serializer {
    
    @Override
    public byte getType() {
        return SerializerType.PROTOBUF;
    }
    
    @Override
    public String getName() {
        return "Protobuf";
    }
    
    @Override
    public <T> byte[] serialize(T obj) throws SerializationException {
        if (obj instanceof MessageLite) {
            return ((MessageLite) obj).toByteArray();
        }
        throw new SerializationException("Object must implement MessageLite");
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) throws SerializationException {
        try {
            Method parseFromMethod = clazz.getMethod("parseFrom", byte[].class);
            return (T) parseFromMethod.invoke(null, data);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize protobuf message", e);
        }
    }
}
```

### 3. 服务监控和指标

```java
@Component
public class DxMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    public DxMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounter = Counter.builder("dx.requests")
            .description("Total number of requests")
            .register(meterRegistry);
        this.requestTimer = Timer.builder("dx.request.duration")
            .description("Request processing time")
            .register(meterRegistry);
    }
    
    public void recordRequest(String serviceName, String methodName, long duration, boolean success) {
        requestCounter.increment(
            Tags.of(
                "service", serviceName,
                "method", methodName,
                "status", success ? "success" : "error"
            )
        );
        
        requestTimer.record(duration, TimeUnit.MILLISECONDS,
            Tags.of(
                "service", serviceName,
                "method", methodName
            )
        );
    }
}
```

## 性能调优建议

### 1. 网络层优化
- 调整Netty的线程池大小
- 启用TCP_NODELAY选项
- 合理设置SO_RCVBUF和SO_SNDBUF

### 2. 序列化优化
- 根据数据特点选择合适的序列化协议
- 对于大对象考虑使用压缩
- 避免序列化不必要的字段

### 3. 连接池优化
- 合理设置连接池大小
- 配置连接空闲超时时间
- 启用连接健康检查

### 4. JVM调优
```bash
# 推荐的JVM参数
-Xms2g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+UseZGC  # Java 11+
```

## 故障排查

### 1. 常见问题

**问题1**: 服务注册失败
```
解决方案:
1. 检查Zookeeper连接状态
2. 确认网络连通性
3. 检查防火墙设置
```

**问题2**: 序列化异常
```
解决方案:
1. 确保客户端和服务端使用相同的序列化协议
2. 检查类路径中是否包含所需的依赖
3. 对于Kryo，确保类注册一致
```

**问题3**: 连接超时
```
解决方案:
1. 调整超时时间配置
2. 检查网络延迟
3. 增加重试次数
```

### 2. 日志配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- DistributedX框架日志 -->
    <logger name="com.distributedx" level="DEBUG" additivity="false">
        <appender-ref ref="STDOUT"/>
    </logger>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

## 最佳实践

1. **服务接口设计**
   - 保持接口简洁，避免过度设计
   - 使用版本控制管理接口变更
   - 合理设计参数和返回值

2. **异常处理**
   - 定义统一的异常处理机制
   - 提供有意义的错误信息
   - 实现优雅的降级策略

3. **性能监控**
   - 监控关键性能指标
   - 设置合理的告警阈值
   - 定期进行性能测试

4. **安全考虑**
   - 实现服务认证和授权
   - 对敏感数据进行加密
   - 防止恶意请求攻击

---

*通过本指南，你可以快速上手DistributedX框架，构建高性能的分布式服务系统。*