# RPC Spring Boot Starter

一个基于Spring Boot的RPC框架自动配置启动器，提供简单易用的分布式服务调用能力。

## 特性

- 🚀 **开箱即用**：基于Spring Boot自动配置，零配置启动
- 🎯 **注解驱动**：使用`@RpcService`和`@RpcReference`注解简化开发
- 🔄 **API兼容**：同时支持注解式和编程式两种使用方式
- 🌐 **服务发现**：支持Zookeeper服务注册与发现
- ⚖️ **负载均衡**：内置多种负载均衡算法
- 🔧 **高度可配置**：丰富的配置选项，满足不同场景需求
- 📊 **监控支持**：内置指标收集和健康检查

## 快速开始

### 1. 添加依赖

在你的Spring Boot项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.hejiexmu</groupId>
    <artifactId>rpc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

在`application.yml`中添加RPC配置：

```yaml
rpc:
  enabled: true
  
  # 服务注册中心配置
  registry:
    type: zookeeper
    address: localhost:2181
    session-timeout: 30000
    connection-timeout: 15000
    retry-times: 3
  
  # 服务提供者配置
  provider:
    enabled: true
    host: localhost
    port: 8081
    serializer: kryo
    worker-threads: 10
  
  # 服务消费者配置
  consumer:
    enabled: true
    timeout: 5000
    retry-count: 3
    connection-pool-size: 10
  
  # 负载均衡算法
  load-balancer: round_robin
```

### 3. 服务提供者

#### 3.1 定义服务接口

```java
public interface UserService {
    User getUserById(Long id);
    List<User> getAllUsers();
    User createUser(User user);
}
```

#### 3.2 实现服务

```java
@RpcService(version = "1.0.0", group = "default")
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public User getUserById(Long id) {
        // 业务逻辑实现
        return new User(id, "user" + id);
    }
    
    @Override
    public List<User> getAllUsers() {
        // 业务逻辑实现
        return Arrays.asList(
            new User(1L, "user1"),
            new User(2L, "user2")
        );
    }
    
    @Override
    public User createUser(User user) {
        // 业务逻辑实现
        return user;
    }
}
```

#### 3.3 启动类配置

```java
@SpringBootApplication
@RpcProvider(port = 8081, host = "localhost", enabled = true)
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```

### 4. 服务消费者

#### 4.1 注入服务引用

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @RpcReference(version = "1.0.0", group = "default")
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }
}
```

#### 4.2 启动类

```java
@SpringBootApplication
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
```

## 高级用法

### 编程式API

除了注解式API，框架还支持编程式API，适用于需要动态创建服务代理的场景：

```java
@Service
public class DynamicServiceCaller {
    
    @Autowired
    private RpcProgrammaticHelper rpcHelper;
    
    public void callDifferentVersions() {
        // 调用默认版本服务
        UserService defaultService = rpcHelper.createServiceProxy(UserService.class);
        List<User> users1 = defaultService.getAllUsers();
        
        // 调用指定版本和分组的服务
        UserService testService = rpcHelper.createServiceProxy(
            UserService.class, "2.0.0", "test"
        );
        List<User> users2 = testService.getAllUsers();
    }
}
```

### 混合使用

你可以在同一个应用中同时使用注解式和编程式API：

```java
@RestController
public class MixedController {
    
    // 注解式：自动注入默认版本服务
    @RpcReference
    private UserService userService;
    
    // 编程式：动态创建不同版本服务代理
    @Autowired
    private RpcProgrammaticHelper rpcHelper;
    
    @GetMapping("/users/default")
    public List<User> getDefaultUsers() {
        return userService.getAllUsers();
    }
    
    @GetMapping("/users/test")
    public List<User> getTestUsers() {
        UserService testService = rpcHelper.createServiceProxy(
            UserService.class, "1.0.0", "test"
        );
        return testService.getAllUsers();
    }
}
```

## 配置详解

### 完整配置示例

```yaml
rpc:
  # 是否启用RPC框架
  enabled: true
  
  # 服务注册中心配置
  registry:
    type: zookeeper                    # 注册中心类型，目前支持zookeeper
    address: localhost:2181            # 注册中心地址
    session-timeout: 30000             # 会话超时时间（毫秒）
    connection-timeout: 15000          # 连接超时时间（毫秒）
    retry-times: 3                     # 重试次数
  
  # 服务提供者配置
  provider:
    enabled: true                      # 是否启用服务提供者
    host: localhost                    # 服务主机地址
    port: 8081                         # 服务端口
    serializer: kryo                   # 序列化器类型：kryo, protostuff, json
    worker-threads: 10                 # 工作线程数
  
  # 服务消费者配置
  consumer:
    enabled: true                      # 是否启用服务消费者
    timeout: 5000                      # 请求超时时间（毫秒）
    retry-count: 3                     # 重试次数
    connection-pool-size: 10           # 连接池大小
  
  # 负载均衡算法
  load-balancer: round_robin           # 负载均衡算法：round_robin, random, consistent_hash, lru, lfu
```

### 注解配置

#### @RpcService

用于标记服务提供者：

```java
@RpcService(
    version = "1.0.0",                 // 服务版本，默认"1.0.0"
    group = "default",                 // 服务分组，默认"default"
    weight = 100                       // 服务权重，默认100
)
```

#### @RpcReference

用于注入服务引用：

```java
@RpcReference(
    version = "1.0.0",                 // 服务版本，默认"1.0.0"
    group = "default",                 // 服务分组，默认"default"
    timeout = 5000,                    // 请求超时时间，默认5000ms
    async = false,                     // 是否异步调用，默认false
    retryCount = 3                     // 重试次数，默认3
)
```

#### @RpcProvider

用于配置服务提供者应用：

```java
@RpcProvider(
    host = "localhost",                // 服务主机，默认localhost
    port = 8081,                       // 服务端口，默认8081
    enabled = true                     // 是否启用，默认true
)
```

## 负载均衡

框架支持多种负载均衡算法：

- **round_robin**：轮询算法，默认选择
- **random**：随机算法
- **consistent_hash**：一致性哈希算法
- **lru**：最近最少使用算法
- **lfu**：最少使用频率算法

配置方式：

```yaml
rpc:
  load-balancer: consistent_hash
```

## 序列化

支持多种序列化方式：

- **kryo**：高性能序列化框架（推荐）
- **protostuff**：基于Protobuf的序列化框架
- **json**：JSON序列化，便于调试

配置方式：

```yaml
rpc:
  provider:
    serializer: kryo
```

## 监控和健康检查

框架内置了监控指标收集和健康检查功能：

```yaml
# Spring Boot Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

访问监控端点：
- 健康检查：`http://localhost:8080/actuator/health`
- 应用信息：`http://localhost:8080/actuator/info`
- 监控指标：`http://localhost:8080/actuator/metrics`

## 示例项目

完整的示例项目位于`rpc-spring-boot-samples`目录：

- `rpc-spring-boot-provider-sample`：服务提供者示例
- `rpc-spring-boot-consumer-sample`：服务消费者示例

运行示例：

1. 启动Zookeeper服务
2. 运行Provider示例：`mvn spring-boot:run -pl rpc-spring-boot-provider-sample`
3. 运行Consumer示例：`mvn spring-boot:run -pl rpc-spring-boot-consumer-sample`
4. 访问：`http://localhost:8082/api/users`

## 故障排除

### 常见问题

1. **服务注册失败**
   - 检查Zookeeper是否正常运行
   - 确认注册中心地址配置正确
   - 查看网络连接是否正常

2. **服务调用超时**
   - 增加超时时间配置
   - 检查服务提供者是否正常运行
   - 确认网络延迟情况

3. **序列化异常**
   - 确保服务接口在提供者和消费者端一致
   - 检查实体类是否实现了Serializable接口
   - 尝试更换序列化器类型

### 日志配置

启用详细日志以便调试：

```yaml
logging:
  level:
    com.hejiexmu.rpc: DEBUG
    com.rpc: DEBUG
```

## 版本兼容性

- Spring Boot 2.x+
- Java 8+
- Zookeeper 3.4+

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 更新日志

### v1.0.0
- 初始版本发布
- 支持基于注解的服务注册和发现
- 集成Spring Boot自动配置
- 支持多种负载均衡算法
- 支持编程式和注解式API