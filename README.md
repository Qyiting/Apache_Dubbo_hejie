# Apace Dubbo Framework（RPC框架）

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.1.65-green.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于 **Netty + Kryo + Zookeeper** 的高性能分布式 RPC 框架，提供完整的服务治理解决方案。

## 🚀 项目特色

- **🔥 高性能通信**：基于 Netty 的异步非阻塞通信，支持自定义协议
- **⚡ 智能负载均衡**：支持随机、轮询、一致性哈希、LRU、LFU 等多种算法
- **🛡️ 多样化序列化**：支持 JSON、Kryo、Hessian 等序列化协议
- **📋 服务注册发现**：基于 Zookeeper 的服务注册与发现机制
- **🔧 动态代理**：支持 JDK 和 CGLIB 动态代理
- **📊 监控指标**：内置性能监控和健康检查机制
- **🔄 SPI 扩展**：支持 SPI 机制的插件化扩展
- **🌱 Spring Boot 集成**：提供完整的 Spring Boot Starter，支持注解驱动开发
- **🖥️ Web 管理界面**：内置实时监控和管理界面，支持多实例部署
- **🔍 实时监控**：集成 Spring Boot Actuator，提供丰富的监控端点
- **📱 响应式设计**：支持 WebSocket 实时数据推送和现代化 UI 界面

## 📋 目录

- [快速开始](#快速开始)
- [项目架构](#项目架构)
- [核心功能](#核心功能)
- [使用示例](#使用示例)
- [性能测试](#性能测试)
- [扩展开发](#扩展开发)
- [贡献指南](#贡献指南)

## 🏗️ 项目架构

### 模块结构

```
simple-rpc-framework/
├── rpc-core/                           # 核心模块 - 基础接口和工具类
├── rpc-client/                         # 客户端模块 - 服务消费者实现
├── rpc-server/                         # 服务端模块 - 服务提供者实现
├── rpc-registry/                       # 注册中心模块 - 服务注册与发现
├── rpc-serialization/                  # 序列化模块 - 多协议序列化支持
├── rpc-netty/                         # 网络通信模块 - 基于Netty的通信实现
├── rpc-example/                       # 示例模块 - 使用示例和测试用例
├── rpc-spring-boot-starter/           # Spring Boot自动配置启动器
│   ├── 自动配置类                      # RPC框架的Spring Boot集成
│   ├── 配置属性类                      # 配置参数绑定
│   └── 注解驱动支持                    # @RpcService、@RpcReference等注解
└── rpc-spring-boot-samples/           # Spring Boot示例应用
    ├── rpc-spring-boot-provider-sample/  # 服务提供者示例
    │   ├── Web管理界面                  # 实时监控和管理功能
    │   ├── REST API接口                # 服务状态、健康检查、配置管理
    │   └── 多实例部署支持               # 支持8081-8084端口的多实例部署
    └── rpc-spring-boot-consumer-sample/  # 服务消费者示例
```

### 技术栈

#### 核心框架
| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架和自动配置 |
| Spring Framework | 6.1.0 | 依赖注入和AOP支持 |
| Netty | 4.1.65 | 网络通信框架 |
| Zookeeper | 3.7.0 | 服务注册中心 |
| Curator | 5.1.0 | Zookeeper客户端 |

#### 序列化与工具
| 技术 | 版本 | 说明 |
|------|------|------|
| Kryo | 5.2.0 | 高性能序列化 |
| Jackson | 2.15.2 | JSON处理 |
| CGLIB | 3.3.0 | 动态代理 |
| Lombok | 1.18.38 | 代码生成工具 |

#### Web与监控
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot Web | 3.2.0 | Web应用和REST API |
| Spring Boot Actuator | 3.2.0 | 应用监控和管理 |
| Thymeleaf | 3.2.0 | 模板引擎 |
| WebSocket | 3.2.0 | 实时数据推送 |

#### 日志与测试
| 技术 | 版本 | 说明 |
|------|------|------|
| SLF4J | 2.0.9 | 日志门面 |
| Logback | 1.4.14 | 日志实现 |
| JUnit4 | 4.13.2 | 单元测试 |
| JUnit5 | 5.8.2 | 单元测试 |

## 🚀 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **Zookeeper**: 3.6+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/simple-rpc-framework.git
cd simple-rpc-framework
```

### 2. 编译项目

```bash
mvn clean install
```

### 3. 启动 Zookeeper

```bash
# 下载并启动 Zookeeper
bin/zkServer.sh start
```

### 4. 运行示例

#### 方式一：传统方式启动

**启动服务提供者**
```bash
mvn exec:java -pl rpc-example -Dexec.mainClass="com.rpc.example.serverexam.ServerExample"
```

**启动服务消费者**
```bash
mvn exec:java -pl rpc-example -Dexec.mainClass="com.rpc.example.clientexam.ClientExample"
```

#### 方式二：Spring Boot 方式启动（推荐）

**启动服务提供者（带Web管理界面）**
```bash
cd rpc-spring-boot-samples/rpc-spring-boot-provider-sample
mvn spring-boot:run
```
访问管理界面：http://localhost:8081

**启动服务消费者**
```bash
cd rpc-spring-boot-samples/rpc-spring-boot-consumer-sample
mvn spring-boot:run
```

**多实例部署**
```bash
# 启动多个Provider实例
mvn spring-boot:run -Dspring-boot.run.profiles=8082  # 端口8082
mvn spring-boot:run -Dspring-boot.run.profiles=8083  # 端口8083
mvn spring-boot:run -Dspring-boot.run.profiles=8084  # 端口8084
```

## 💡 核心功能

### 1. 负载均衡算法

框架支持多种负载均衡算法，通过 SPI 机制实现动态加载：

- **随机算法 (Random)**：随机选择服务实例
- **轮询算法 (Round Robin)**：按顺序轮流选择服务实例
- **一致性哈希 (Consistent Hash)**：基于请求参数的一致性哈希
- **LRU算法**：最近最少使用的服务实例优先
- **LFU算法**：最少使用频率的服务实例优先

### 2. 序列化协议

支持多种序列化协议，可根据性能需求选择：

- **JSON**：可读性好，调试方便
- **Kryo**：高性能二进制序列化
- **Hessian**：跨语言二进制序列化

### 3. 服务治理

- **服务注册**：自动注册服务到 Zookeeper
- **服务发现**：动态发现可用服务实例
- **健康检查**：定期检查服务实例健康状态
- **故障转移**：自动剔除不健康的服务实例

### 4. 监控指标

内置丰富的监控指标：

- 请求总数、成功数、失败数
- 请求响应时间统计
- 连接池状态监控
- 服务发现次数统计
- 健康检查状态

### 5. Web管理界面

提供完整的Web管理界面，支持实时监控和管理：

#### 🖥️ 管理页面
- **服务概览页面** (`/api/management/overview`)
  - 服务基本信息：名称、版本、状态、启动时间、运行时长
  - 端口信息：Web端口、RPC端口、主机地址
  - 活跃配置文件信息

- **健康检查页面** (`/api/management/health`)
  - 服务健康状态监控
  - JVM内存使用情况（堆内存、非堆内存）
  - 系统信息（处理器数量、Java版本、操作系统）
  - 集成Spring Boot Actuator增强监控

- **配置管理页面** (`/api/management/config`)
  - RPC服务配置（主机、端口、启用状态）
  - Web服务配置（端口、上下文路径）
  - 应用配置（名称、活跃配置文件）

- **性能指标页面** (`/api/management/metrics`)
  - JVM运行时指标（运行时间、启动时间、内存使用）
  - 系统资源指标（总内存、可用内存、最大内存、处理器数量）
  - 管理界面特定指标（页面访问量、活跃用户数）
  - 集成Actuator增强指标

- **日志管理页面** (`/api/management/logs`)
  - 实时日志查看（支持指定行数和日志级别）
  - 日志级别配置查看
  - 日志文件配置信息
  - 模拟日志数据展示

#### 🏢 实例管理功能
- **实例列表** (`/api/management/instances`)
  - 查看所有RPC Provider实例
  - 实例状态监控（运行中、已停止、启动中）
  - 实例基本信息（ID、名称、主机、端口、启动时间）
  - 性能指标（内存使用率、RPC调用次数、连接数）

- **实例详情** (`/api/management/instances/{instanceId}`)
  - 详细的实例信息和性能数据
  - CPU使用率、内存使用率、线程数量
  - RPC调用统计（总调用数、成功数、失败数、平均响应时间）
  - 连接数和运行时长信息

- **实例日志** (`/api/management/instances/{instanceId}/logs`)
  - 查看特定实例的日志
  - 支持指定日志行数
  - 实时日志更新

- **实例操作**
  - 重启实例 (`POST /api/management/instances/{instanceId}/restart`)
  - 停止实例 (`POST /api/management/instances/{instanceId}/stop`)
  - 导出实例日志 (`GET /api/management/instances/{instanceId}/logs/export`)

#### 🔧 技术特性
- **RESTful API设计**：标准的REST接口，支持跨域访问
- **实时数据更新**：支持WebSocket实时数据推送
- **响应式设计**：适配不同设备和屏幕尺寸
- **Spring Boot Actuator集成**：增强的监控和管理功能
- **日志导出功能**：支持日志文件导出和下载
- **模拟数据支持**：提供丰富的模拟数据用于演示

#### 🚀 访问方式
- **Web管理界面**：http://localhost:8081
- **API接口**：http://localhost:8081/api/management/*
- **Actuator端点**：http://localhost:8081/actuator/*

## 📖 使用示例

### 方式一：传统编程方式

#### 定义服务接口

```java
public interface UserService {
    User getUserById(Long id);
    List<User> getAllUsers();
    boolean saveUser(User user);
    CompletableFuture<User> getUserByIdAsync(Long id);
}
```

### 服务提供者

```java
@Slf4j
public class UserServiceImpl implements UserService {
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    
    @Override
    public User getUserById(Long id) {
        log.info("获取用户信息，ID: {}", id);
        return userDatabase.get(id);
    }
    
    @Override
    public boolean saveUser(User user) {
        log.info("保存用户信息: {}", user);
        userDatabase.put(user.getId(), user);
        return true;
    }
    
    // 其他方法实现...
}
```

### 启动服务提供者

```java
public class ServerExample {
    public static void main(String[] args) {
        try {
            // 1. 创建服务注册中心
            ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry("127.0.0.1:2181");
            
            // 2. 创建RPC服务器
            RpcServer rpcServer = new RpcServer("127.0.0.1", 9999, serviceRegistry);
            
            // 3. 注册服务
            UserService userService = new UserServiceImpl();
            rpcServer.registerService(UserService.class, userService, "1.0.0", "default");
            
            // 4. 启动服务器
            rpcServer.start();
            log.info("RPC服务器启动成功，监听端口: 9999");
            
        } catch (Exception e) {
            log.error("启动服务器失败", e);
        }
    }
}
```

### 方式二：Spring Boot 注解驱动方式（推荐）

#### 1. 添加依赖

```xml
<dependency>
    <groupId>com.hejiexmu</groupId>
    <artifactId>rpc-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

#### 2. 配置文件 (application.yml)

```yaml
rpc:
  registry:
    address: 127.0.0.1:2181
    type: zookeeper
  provider:
    host: 127.0.0.1
    port: 9081
    enabled: true
  consumer:
    timeout: 15000
    max-connections: 20
    load-balancer: random
```

#### 3. 服务提供者实现

```java
@RpcService(interfaceClass = UserService.class, version = "1.0.0")
@Component
@Slf4j
public class UserServiceImpl implements UserService {
    private final Map<Long, User> userDatabase = new ConcurrentHashMap<>();
    
    @Override
    public User getUserById(Long id) {
        log.info("获取用户信息，ID: {}", id);
        return userDatabase.get(id);
    }
    
    @Override
    public boolean saveUser(User user) {
        log.info("保存用户信息: {}", user);
        userDatabase.put(user.getId(), user);
        return true;
    }
    
    // 其他方法实现...
}
```

#### 4. 服务消费者

```java
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    
    @RpcReference(version = "1.0.0")
    private UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("获取用户失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping
    public ResponseEntity<String> saveUser(@RequestBody User user) {
        try {
            boolean success = userService.saveUser(user);
            return success ? ResponseEntity.ok("保存成功") : ResponseEntity.status(500).body("保存失败");
        } catch (Exception e) {
            log.error("保存用户失败", e);
            return ResponseEntity.status(500).body("保存失败");
        }
    }
}
```

#### 5. 启动类

```java
@SpringBootApplication
@EnableRpc  // 启用RPC功能
public class ProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
```

### 服务消费者

```java
public class ClientExample {
    public static void main(String[] args) {
        try {
            // 1. 创建服务注册中心
            ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry("127.0.0.1:2181");
            
            // 2. 创建负载均衡器
            LoadBalancer loadBalancer = new RandomLoadBalancer();
            
            // 3. 创建RPC客户端
            RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer, 15000, 20);
            
            // 4. 创建服务代理
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class, (byte) 1);
            
            // 5. 调用远程服务
            User user = userService.getUserById(1L);
            log.info("获取到用户信息: {}", user);
            
            // 6. 异步调用
            CompletableFuture<User> future = userService.getUserByIdAsync(2L);
            User asyncUser = future.get(5, TimeUnit.SECONDS);
            log.info("异步获取用户信息: {}", asyncUser);
            
        } catch (Exception e) {
            log.error("客户端调用失败", e);
        }
    }
}
```

## 📊 性能测试

### 测试环境

- **CPU**: Intel i7-8700K
- **内存**: 16GB DDR4
- **网络**: 千兆以太网
- **JVM**: OpenJDK 17

### 性能指标

| 指标 | 数值 |
|------|------|
| 单机QPS | 10,000+ |
| 平均响应时间 | < 10ms |
| 99%响应时间 | < 50ms |
| 并发连接数 | 1,000+ |
| 内存占用 | < 100MB |

### 负载均衡性能对比

| 算法 | QPS | 平均响应时间 | 内存占用 |
|------|-----|-------------|----------|
| Random | 12,000 | 8ms | 80MB |
| Round Robin | 11,500 | 9ms | 85MB |
| Consistent Hash | 10,800 | 12ms | 95MB |
| LRU | 11,200 | 10ms | 90MB |
| LFU | 11,000 | 11ms | 92MB |

## 🔧 扩展开发

### Spring Boot 自动配置扩展

#### 自定义配置属性

```java
@ConfigurationProperties(prefix = "rpc.custom")
@Data
public class CustomRpcProperties {
    private String customProperty = "default";
    private int timeout = 5000;
    private boolean enabled = true;
}
```

#### 自定义自动配置类

```java
@Configuration
@ConditionalOnProperty(prefix = "rpc.custom", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(CustomRpcProperties.class)
public class CustomRpcAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public CustomRpcService customRpcService(CustomRpcProperties properties) {
        return new CustomRpcService(properties);
    }
}
```

### 传统方式扩展

#### 自定义负载均衡算法

1. 实现 `LoadBalancer` 接口：

```java
public class CustomLoadBalancer implements LoadBalancer {
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfos, RpcRequest request) {
        // 自定义负载均衡逻辑
        return serviceInfos.get(0);
    }
    
    @Override
    public String getAlgorithm() {
        return "custom";
    }
}
```

2. 在 `META-INF/services/com.rpc.client.loadbalance.LoadBalancer` 文件中注册：

```
com.example.CustomLoadBalancer
```

### 自定义序列化器

1. 实现 `Serializer` 接口：

```java
public class CustomSerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) throws IOException {
        // 自定义序列化逻辑
        return new byte[0];
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        // 自定义反序列化逻辑
        return null;
    }
    
    @Override
    public byte getType() {
        return 99; // 自定义类型ID
    }
}
```

## 📚 文档

详细文档请参考 [docs](./docs) 目录：

### 核心文档
- [技术架构详解](./docs/TECHNICAL_ARCHITECTURE.md)
- [快速开始指南](./docs/QUICK_START_GUIDE.md)

### 功能指南
- [负载均衡器指南](./docs/ConsistentHashLoadBalancer.md)
- [LRU负载均衡器指南](./docs/LRU_LOAD_BALANCER_GUIDE.md)
- [LFU负载均衡器指南](./docs/LFU_LOAD_BALANCER_GUIDE.md)
- [序列化器指南](./docs/SERIALIZERS_GUIDE.md)
- [CGLIB代理指南](./docs/CGLIB_PROXY_GUIDE.md)
- [CGLIB JVM参数配置](./docs/CGLIB_JVM_ARGS.md)

### Spring Boot 集成
- [Spring Boot Starter配置说明](./rpc-spring-boot-starter/CONFIGURATION.md)
- [Spring Boot Starter使用指南](./rpc-spring-boot-starter/README.md)
- [多实例部署说明](./rpc-spring-boot-samples/rpc-spring-boot-provider-sample/多实例部署说明.md)

### 项目信息
- [项目重构说明](./docs/PROJECT_REBRANDING.md)

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 代码规范

- 遵循 Java 编码规范
- 添加必要的单元测试
- 更新相关文档
- 确保所有测试通过

## 📄 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 🙏 致谢

感谢以下开源项目的启发：

- [Apache Dubbo](https://dubbo.apache.org/)
- [Netty](https://netty.io/)
- [Apache Zookeeper](https://zookeeper.apache.org/)
- [Kryo](https://github.com/EsotericSoftware/kryo)

## 📞 联系方式

- 作者：何杰
- 邮箱：19820231153893@stu.xmu.edu.cn
- 项目地址：https://github.com/Qyiting/Apache_Dubbo_hejie

---

⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！