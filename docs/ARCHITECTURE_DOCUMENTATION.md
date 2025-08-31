# Simple RPC Framework 架构说明文档

## 概述

Simple RPC Framework 是一个基于 Java 的分布式 RPC 框架，提供了完整的远程过程调用解决方案。框架采用模块化设计，支持多种序列化方式、负载均衡策略、服务注册与发现，并集成了完整的认证授权体系。

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                      │
├─────────────────────────────────────────────────────────────────┤
│                     RPC Spring Boot Starter                    │
├─────────────────────────────────────────────────────────────────┤
│  RPC Client  │  RPC Server  │  Auth Service  │  Load Balancer  │
├─────────────────────────────────────────────────────────────────┤
│              RPC Core (Protocol, Serialization)                │
├─────────────────────────────────────────────────────────────────┤
│                     Network Layer (Netty)                      │
├─────────────────────────────────────────────────────────────────┤
│  Service Registry  │     Session Store     │    Data Storage   │
│   (Zookeeper)      │      (Redis)          │     (MySQL)       │
└─────────────────────────────────────────────────────────────────┘
```

### 核心组件

1. **RPC Core**: 核心协议和序列化组件
2. **RPC Client**: 客户端调用组件
3. **RPC Server**: 服务端处理组件
4. **RPC Netty**: 网络通信层
5. **RPC Registry**: 服务注册与发现
6. **RPC Serialization**: 序列化组件
7. **RPC Auth Service**: 认证授权服务
8. **RPC Spring Boot Starter**: Spring Boot 集成组件

## 模块详细设计

### 1. RPC Core 模块

**职责**: 提供 RPC 框架的核心功能和协议定义

**主要组件**:
- **Protocol**: RPC 协议定义
- **Message**: 消息格式定义
- **Exception**: 异常处理
- **Retry Strategy**: 重试策略
- **Load Balancer**: 负载均衡器

**关键类**:
```java
// RPC 协议
public class RpcProtocol {
    private byte magicNumber;     // 魔数
    private byte version;         // 版本号
    private byte messageType;     // 消息类型
    private byte serializationType; // 序列化类型
    private int messageLength;    // 消息长度
}

// RPC 请求
public class RpcRequest {
    private String requestId;     // 请求ID
    private String interfaceName; // 接口名
    private String methodName;    // 方法名
    private Class<?>[] parameterTypes; // 参数类型
    private Object[] parameters;  // 参数值
}

// RPC 响应
public class RpcResponse {
    private String requestId;     // 请求ID
    private Object result;        // 返回结果
    private Throwable exception;  // 异常信息
}
```

**负载均衡策略**:
- **Random**: 随机选择
- **Round Robin**: 轮询
- **Weighted Round Robin**: 加权轮询
- **Least Connections**: 最少连接
- **Consistent Hash**: 一致性哈希
- **LRU**: 最近最少使用
- **LFU**: 最少使用频率

### 2. RPC Netty 模块

**职责**: 基于 Netty 实现高性能网络通信

**主要组件**:
- **NettyClient**: Netty 客户端
- **NettyServer**: Netty 服务端
- **Codec**: 编解码器
- **Handler**: 消息处理器

**网络架构**:
```
Client                          Server
┌─────────────┐                ┌─────────────┐
│ RpcClient   │                │ RpcServer   │
├─────────────┤                ├─────────────┤
│ NettyClient │◄──────────────►│ NettyServer │
├─────────────┤                ├─────────────┤
│   Encoder   │                │   Decoder   │
│   Decoder   │                │   Encoder   │
├─────────────┤                ├─────────────┤
│    Netty    │                │    Netty    │
│  EventLoop  │                │  EventLoop  │
└─────────────┘                └─────────────┘
```

**关键特性**:
- **异步非阻塞**: 基于 Netty 的异步 I/O
- **连接池**: 客户端连接池管理
- **心跳检测**: 定期心跳保持连接
- **断线重连**: 自动重连机制
- **流量控制**: 背压处理

### 3. RPC Serialization 模块

**职责**: 提供多种序列化方式支持

**支持的序列化方式**:
- **Kryo**: 高性能二进制序列化
- **Protobuf**: Google Protocol Buffers
- **JSON**: JSON 格式序列化
- **Hessian**: Hessian 二进制序列化
- **JDK**: Java 原生序列化

**序列化器接口**:
```java
public interface Serializer {
    byte[] serialize(Object obj) throws SerializationException;
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException;
    byte getSerializerType();
}
```

**性能对比**:
| 序列化方式 | 序列化速度 | 反序列化速度 | 数据大小 | 兼容性 |
|------------|------------|--------------|----------|--------|
| Kryo       | 很快       | 很快         | 小       | 中等   |
| Protobuf   | 快         | 快           | 很小     | 好     |
| JSON       | 中等       | 中等         | 大       | 很好   |
| Hessian    | 快         | 快           | 中等     | 好     |
| JDK        | 慢         | 慢           | 大       | 很好   |

### 4. RPC Registry 模块

**职责**: 服务注册与发现

**主要组件**:
- **ServiceRegistry**: 服务注册接口
- **ServiceDiscovery**: 服务发现接口
- **ZookeeperRegistry**: Zookeeper 实现
- **ConsulRegistry**: Consul 实现（扩展）
- **EtcdRegistry**: Etcd 实现（扩展）

**服务注册流程**:
```
1. 服务启动时注册到注册中心
2. 定期发送心跳保持注册状态
3. 服务下线时从注册中心移除
4. 客户端订阅服务变更通知
5. 动态更新本地服务列表
```

**Zookeeper 节点结构**:
```
/simple-rpc
├── services
│   ├── com.example.UserService
│   │   ├── providers
│   │   │   ├── 192.168.1.100:8080
│   │   │   └── 192.168.1.101:8080
│   │   └── consumers
│   │       ├── 192.168.1.200:9090
│   │       └── 192.168.1.201:9090
│   └── com.example.OrderService
│       └── providers
│           └── 192.168.1.102:8080
└── config
    ├── loadbalancer
    └── timeout
```

### 5. RPC Auth Service 模块

**职责**: 提供完整的认证授权解决方案

**架构设计**:
```
┌─────────────────────────────────────────────────────────────┐
│                    Auth Service Layer                      │
├─────────────────────────────────────────────────────────────┤
│  AuthController  │  UserController  │  PermissionController │
├─────────────────────────────────────────────────────────────┤
│   AuthService    │   UserService    │   PermissionService   │
├─────────────────────────────────────────────────────────────┤
│  JWT Service  │  Session Service  │  Password Service      │
├─────────────────────────────────────────────────────────────┤
│     Data Access Layer (JPA/MyBatis)                        │
├─────────────────────────────────────────────────────────────┤
│  MySQL (Master/Slave)  │        Redis Cluster              │
└─────────────────────────────────────────────────────────────┘
```

**核心功能**:
- **用户管理**: 注册、登录、信息管理
- **JWT 认证**: 基于 JWT 的无状态认证
- **会话管理**: Redis 分布式会话存储
- **权限控制**: RBAC 权限模型
- **密码安全**: BCrypt 加密存储
- **多端登录**: 支持多设备同时登录

**数据库设计**:
```sql
-- 用户表
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
CREATE TABLE permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(100),
    action VARCHAR(50),
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 角色权限关联表
CREATE TABLE role_permissions (
    role_id BIGINT,
    permission_id BIGINT,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

-- 会话表
CREATE TABLE user_sessions (
    session_id VARCHAR(128) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 6. RPC Spring Boot Starter 模块

**职责**: 提供 Spring Boot 自动配置和集成

**主要组件**:
- **AutoConfiguration**: 自动配置类
- **Properties**: 配置属性
- **Annotations**: 注解定义
- **BeanPostProcessor**: Bean 后处理器

**自动配置**:
```java
@Configuration
@EnableConfigurationProperties(RpcProperties.class)
@ConditionalOnProperty(prefix = "rpc", name = "enabled", havingValue = "true")
public class RpcAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistry serviceRegistry(RpcProperties properties) {
        return new ZookeeperServiceRegistry(properties.getRegistry());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancer loadBalancer(RpcProperties properties) {
        return LoadBalancerFactory.create(properties.getLoadBalancer().getType());
    }
    
    @Bean
    @ConditionalOnMissingBean
    public Serializer serializer(RpcProperties properties) {
        return SerializerFactory.create(properties.getSerialization().getType());
    }
}
```

**配置属性**:
```yaml
rpc:
  enabled: true
  server:
    port: 8080
    host: 0.0.0.0
  client:
    timeout: 5000
    retries: 3
  registry:
    type: zookeeper
    address: localhost:2181
    timeout: 10000
  serialization:
    type: kryo
  loadbalancer:
    type: round_robin
  auth:
    enabled: true
    jwt:
      secret: your-secret-key
      expiration: 86400000
```

## 技术选型

### 核心技术栈

| 技术领域 | 选择 | 版本 | 说明 |
|----------|------|------|------|
| 开发语言 | Java | 17+ | 现代 Java 特性支持 |
| 构建工具 | Maven | 3.6+ | 依赖管理和构建 |
| 网络框架 | Netty | 4.1.x | 高性能异步网络框架 |
| 序列化 | Kryo | 5.4.x | 高性能序列化框架 |
| 服务注册 | Zookeeper | 3.7.x | 分布式协调服务 |
| 缓存存储 | Redis | 6.2+ | 内存数据库 |
| 关系数据库 | MySQL | 8.0+ | 关系型数据库 |
| ORM 框架 | Spring Data JPA | 2.7.x | 数据访问层 |
| Web 框架 | Spring Boot | 2.7.x | 微服务框架 |
| 安全框架 | Spring Security | 5.7.x | 安全认证框架 |
| 监控指标 | Micrometer | 1.9.x | 应用监控 |
| 日志框架 | Logback | 1.2.x | 日志记录 |

### 技术选型理由

**Netty**:
- 高性能异步 I/O
- 成熟稳定的网络框架
- 丰富的编解码器支持
- 优秀的内存管理

**Kryo**:
- 序列化性能优异
- 生成的字节码体积小
- 支持循环引用
- 无需实现 Serializable 接口

**Zookeeper**:
- 强一致性保证
- 成熟的分布式协调服务
- 丰富的客户端支持
- 良好的社区生态

**Redis**:
- 高性能内存数据库
- 丰富的数据结构支持
- 集群模式支持
- 持久化机制

**Spring Boot**:
- 快速开发和部署
- 丰富的自动配置
- 强大的生态系统
- 优秀的监控和管理功能

## 性能优化

### 1. 网络层优化

**连接池管理**:
- 客户端连接池复用
- 连接预热机制
- 空闲连接回收
- 连接健康检查

**I/O 优化**:
- 零拷贝技术
- 直接内存使用
- 批量读写
- 异步非阻塞 I/O

### 2. 序列化优化

**Kryo 优化**:
- 预注册类信息
- 使用 Unsafe 操作
- 对象池复用
- 压缩算法集成

**缓存策略**:
- 序列化结果缓存
- 类元信息缓存
- 反射信息缓存

### 3. 负载均衡优化

**算法优化**:
- 一致性哈希环
- 加权轮询算法
- 最少连接数统计
- 响应时间权重

**健康检查**:
- 定期健康探测
- 故障节点隔离
- 自动恢复机制
- 熔断器模式

### 4. 缓存优化

**多级缓存**:
- 本地缓存（Caffeine）
- 分布式缓存（Redis）
- 数据库查询缓存
- 结果集缓存

**缓存策略**:
- LRU 淘汰策略
- 过期时间设置
- 缓存预热
- 缓存穿透防护

## 安全设计

### 1. 认证安全

**JWT 安全**:
- 强密钥算法（HS256/RS256）
- 短期访问令牌
- 刷新令牌机制
- 令牌黑名单

**密码安全**:
- BCrypt 哈希算法
- 盐值随机生成
- 密码强度检查
- 密码历史记录

### 2. 传输安全

**TLS 加密**:
- TLS 1.2+ 协议
- 证书验证
- 密钥交换
- 数据完整性校验

**消息签名**:
- HMAC 消息认证
- 时间戳防重放
- 请求 ID 唯一性
- 参数完整性校验

### 3. 访问控制

**RBAC 模型**:
- 用户-角色-权限
- 细粒度权限控制
- 动态权限分配
- 权限继承机制

**API 安全**:
- 接口访问控制
- 请求频率限制
- IP 白名单
- 异常访问监控

## 监控和运维

### 1. 应用监控

**性能指标**:
- QPS/TPS 统计
- 响应时间分布
- 错误率统计
- 资源使用率

**业务指标**:
- 用户活跃度
- 接口调用量
- 服务可用性
- 数据一致性

### 2. 日志管理

**日志分类**:
- 访问日志
- 错误日志
- 安全日志
- 性能日志

**日志格式**:
```json
{
  "timestamp": "2024-01-20T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.hejiexmu.rpc.auth.AuthService",
  "thread": "http-nio-8080-exec-1",
  "message": "User login successful",
  "userId": 12345,
  "username": "testuser",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "requestId": "req_1234567890",
  "duration": 150
}
```

### 3. 告警机制

**告警规则**:
- 错误率超过阈值
- 响应时间过长
- 服务不可用
- 资源使用率过高

**告警方式**:
- 邮件通知
- 短信告警
- 钉钉/企业微信
- 监控大屏

## 扩展性设计

### 1. 水平扩展

**服务扩展**:
- 无状态服务设计
- 负载均衡支持
- 自动服务发现
- 弹性伸缩

**数据扩展**:
- 数据库分库分表
- Redis 集群模式
- 读写分离
- 数据同步机制

### 2. 功能扩展

**插件机制**:
- SPI 服务发现
- 自定义序列化器
- 自定义负载均衡器
- 自定义拦截器

**协议扩展**:
- 多协议支持
- 协议版本兼容
- 自定义协议
- 协议转换

## 部署架构

### 1. 单机部署

```
┌─────────────────────────────────────┐
│              Application            │
├─────────────────────────────────────┤
│  Auth Service  │  Business Service  │
├─────────────────────────────────────┤
│     MySQL      │      Redis         │
├─────────────────────────────────────┤
│            Zookeeper                │
└─────────────────────────────────────┘
```

### 2. 集群部署

```
┌─────────────────────────────────────────────────────────────┐
│                      Load Balancer                         │
├─────────────────────────────────────────────────────────────┤
│  Auth Service  │  Auth Service  │  Auth Service            │
│   Instance 1   │   Instance 2   │   Instance 3             │
├─────────────────────────────────────────────────────────────┤
│  Business Svc  │  Business Svc  │  Business Svc            │
│   Instance 1   │   Instance 2   │   Instance 3             │
├─────────────────────────────────────────────────────────────┤
│ MySQL Master   │ MySQL Slave 1  │ MySQL Slave 2            │
├─────────────────────────────────────────────────────────────┤
│ Redis Node 1   │ Redis Node 2   │ Redis Node 3             │
│ Redis Node 4   │ Redis Node 5   │ Redis Node 6             │
├─────────────────────────────────────────────────────────────┤
│ Zookeeper 1    │ Zookeeper 2    │ Zookeeper 3              │
└─────────────────────────────────────────────────────────────┘
```

### 3. 微服务部署

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway                             │
├─────────────────────────────────────────────────────────────┤
│ Auth Service │ User Service │ Order Service │ Pay Service  │
├─────────────────────────────────────────────────────────────┤
│              Service Mesh (Istio/Linkerd)                  │
├─────────────────────────────────────────────────────────────┤
│                  Kubernetes Cluster                        │
├─────────────────────────────────────────────────────────────┤
│  MySQL Cluster  │  Redis Cluster  │  Zookeeper Cluster    │
└─────────────────────────────────────────────────────────────┘
```

## 最佳实践

### 1. 开发规范

**代码规范**:
- 遵循 Google Java Style
- 使用 CheckStyle 检查
- 代码覆盖率 > 80%
- 定期代码审查

**接口设计**:
- RESTful API 设计
- 版本化管理
- 向后兼容
- 文档完整

### 2. 运维规范

**部署规范**:
- 蓝绿部署
- 滚动更新
- 回滚机制
- 健康检查

**监控规范**:
- 全链路监控
- 实时告警
- 性能分析
- 容量规划

### 3. 安全规范

**开发安全**:
- 安全编码规范
- 依赖安全扫描
- 代码安全审计
- 漏洞修复流程

**运维安全**:
- 访问权限控制
- 数据备份加密
- 网络安全防护
- 安全事件响应

## 未来规划

### 1. 功能增强

**短期目标**:
- 支持 gRPC 协议
- 集成 OpenTracing
- 支持多数据中心
- 增强监控能力

**长期目标**:
- 支持 GraphQL
- 机器学习集成
- 智能负载均衡
- 自适应限流

### 2. 性能优化

**网络优化**:
- HTTP/2 支持
- QUIC 协议集成
- 边缘计算支持
- CDN 集成

**计算优化**:
- GraalVM 原生镜像
- 响应式编程
- 异步处理优化
- 内存使用优化

### 3. 生态建设

**工具链**:
- IDE 插件开发
- 代码生成工具
- 性能测试工具
- 运维管理平台

**社区建设**:
- 开源贡献
- 文档完善
- 示例项目
- 技术分享

---

## 总结

Simple RPC Framework 采用模块化、可扩展的架构设计，提供了完整的分布式 RPC 解决方案。框架在性能、安全、可用性等方面都有充分考虑，能够满足企业级应用的需求。

通过合理的技术选型和架构设计，框架具备了良好的扩展性和维护性，为后续的功能增强和性能优化奠定了坚实的基础。

**关键优势**:
- 高性能网络通信
- 多种序列化支持
- 完整的认证授权
- 灵活的负载均衡
- 强大的监控能力
- 易于集成和使用

**适用场景**:
- 微服务架构
- 分布式系统
- 高并发应用
- 企业级应用
- 云原生应用

---

**注意**: 本架构文档适用于 Simple RPC Framework v1.0.0，随着版本迭代，架构可能会有所调整和优化。