# RPC Spring Boot Starter 配置参考

本文档详细介绍了RPC Spring Boot Starter的所有配置选项。

## 配置结构

```yaml
rpc:
  enabled: true                      # 全局开关
  registry:                          # 服务注册中心配置
    type: zookeeper
    address: localhost:2181
    session-timeout: 30000
    connection-timeout: 15000
    retry-times: 3
  provider:                          # 服务提供者配置
    enabled: true
    host: localhost
    port: 8081
    serializer: kryo
    worker-threads: 10
  consumer:                          # 服务消费者配置
    enabled: true
    timeout: 5000
    retry-count: 3
    connection-pool-size: 10
  load-balancer: round_robin         # 负载均衡算法
```

## 全局配置

### rpc.enabled

- **类型**: `boolean`
- **默认值**: `true`
- **描述**: 是否启用RPC框架
- **示例**:
  ```yaml
  rpc:
    enabled: true
  ```

## 服务注册中心配置 (rpc.registry)

### rpc.registry.type

- **类型**: `string`
- **默认值**: `zookeeper`
- **可选值**: `zookeeper`
- **描述**: 服务注册中心类型
- **示例**:
  ```yaml
  rpc:
    registry:
      type: zookeeper
  ```

### rpc.registry.address

- **类型**: `string`
- **默认值**: `localhost:2181`
- **描述**: 服务注册中心地址，支持多个地址用逗号分隔
- **示例**:
  ```yaml
  rpc:
    registry:
      address: localhost:2181
      # 或多个地址
      address: zk1:2181,zk2:2181,zk3:2181
  ```

### rpc.registry.session-timeout

- **类型**: `int`
- **默认值**: `30000`
- **单位**: 毫秒
- **描述**: Zookeeper会话超时时间
- **示例**:
  ```yaml
  rpc:
    registry:
      session-timeout: 30000
  ```

### rpc.registry.connection-timeout

- **类型**: `int`
- **默认值**: `15000`
- **单位**: 毫秒
- **描述**: Zookeeper连接超时时间
- **示例**:
  ```yaml
  rpc:
    registry:
      connection-timeout: 15000
  ```

### rpc.registry.retry-times

- **类型**: `int`
- **默认值**: `3`
- **描述**: 连接重试次数
- **示例**:
  ```yaml
  rpc:
    registry:
      retry-times: 3
  ```

## 服务提供者配置 (rpc.provider)

### rpc.provider.enabled

- **类型**: `boolean`
- **默认值**: `false`
- **描述**: 是否启用服务提供者功能
- **注意**: 只有启用后才会创建RpcServer相关Bean
- **示例**:
  ```yaml
  rpc:
    provider:
      enabled: true
  ```

### rpc.provider.host

- **类型**: `string`
- **默认值**: `localhost`
- **描述**: 服务提供者绑定的主机地址
- **注意**: 在生产环境中应设置为实际的服务器IP
- **示例**:
  ```yaml
  rpc:
    provider:
      host: 192.168.1.100
  ```

### rpc.provider.port

- **类型**: `int`
- **默认值**: `8081`
- **描述**: 服务提供者监听的端口
- **示例**:
  ```yaml
  rpc:
    provider:
      port: 8081
  ```

### rpc.provider.serializer

- **类型**: `string`
- **默认值**: `kryo`
- **可选值**: `kryo`, `protostuff`, `json`
- **描述**: 序列化器类型
- **性能对比**:
  - `kryo`: 高性能，推荐用于生产环境
  - `protostuff`: 基于Protobuf，性能良好
  - `json`: 便于调试，性能较低
- **示例**:
  ```yaml
  rpc:
    provider:
      serializer: kryo
  ```

### rpc.provider.worker-threads

- **类型**: `int`
- **默认值**: `10`
- **描述**: 工作线程池大小
- **建议**: 根据服务器CPU核心数和预期并发量调整
- **示例**:
  ```yaml
  rpc:
    provider:
      worker-threads: 20
  ```

## 服务消费者配置 (rpc.consumer)

### rpc.consumer.enabled

- **类型**: `boolean`
- **默认值**: `false`
- **描述**: 是否启用服务消费者功能
- **注意**: 只有启用后才会创建RpcClient相关Bean
- **示例**:
  ```yaml
  rpc:
    consumer:
      enabled: true
  ```

### rpc.consumer.timeout

- **类型**: `int`
- **默认值**: `5000`
- **单位**: 毫秒
- **描述**: 默认请求超时时间
- **注意**: 可以在@RpcReference注解中单独设置
- **示例**:
  ```yaml
  rpc:
    consumer:
      timeout: 10000
  ```

### rpc.consumer.retry-count

- **类型**: `int`
- **默认值**: `3`
- **描述**: 默认重试次数
- **注意**: 可以在@RpcReference注解中单独设置
- **示例**:
  ```yaml
  rpc:
    consumer:
      retry-count: 5
  ```

### rpc.consumer.connection-pool-size

- **类型**: `int`
- **默认值**: `10`
- **描述**: 连接池大小
- **建议**: 根据预期并发量调整
- **示例**:
  ```yaml
  rpc:
    consumer:
      connection-pool-size: 20
  ```

## 负载均衡配置 (rpc.load-balancer)

- **类型**: `string`
- **默认值**: `round_robin`
- **可选值**: 
  - `round_robin`: 轮询算法
  - `random`: 随机算法
  - `consistent_hash`: 一致性哈希算法
  - `lru`: 最近最少使用算法
  - `lfu`: 最少使用频率算法
- **描述**: 负载均衡算法选择
- **示例**:
  ```yaml
  rpc:
    load-balancer: consistent_hash
  ```

## 环境特定配置

### 开发环境配置

```yaml
# application-dev.yml
rpc:
  enabled: true
  registry:
    address: localhost:2181
  provider:
    enabled: true
    host: localhost
    port: 8081
    serializer: json          # 便于调试
    worker-threads: 5
  consumer:
    enabled: true
    timeout: 10000            # 开发环境可以设置更长超时
    connection-pool-size: 5
  load-balancer: round_robin

# 开启详细日志
logging:
  level:
    com.hejiexmu.rpc: DEBUG
    com.rpc: DEBUG
```

### 测试环境配置

```yaml
# application-test.yml
rpc:
  enabled: true
  registry:
    address: test-zk:2181
  provider:
    enabled: true
    host: ${server.address:localhost}
    port: ${rpc.port:8081}
    serializer: kryo
    worker-threads: 10
  consumer:
    enabled: true
    timeout: 8000
    retry-count: 2
    connection-pool-size: 10
  load-balancer: random
```

### 生产环境配置

```yaml
# application-prod.yml
rpc:
  enabled: true
  registry:
    address: zk1:2181,zk2:2181,zk3:2181
    session-timeout: 60000
    connection-timeout: 30000
    retry-times: 5
  provider:
    enabled: true
    host: ${HOST_IP}          # 从环境变量获取
    port: ${RPC_PORT:8081}
    serializer: kryo
    worker-threads: ${CPU_CORES:20}
  consumer:
    enabled: true
    timeout: 5000
    retry-count: 3
    connection-pool-size: 50
  load-balancer: consistent_hash

# 生产环境日志配置
logging:
  level:
    com.hejiexmu.rpc: INFO
    com.rpc: INFO
```

## 条件配置

### 基于Profile的配置

```yaml
# 通用配置
rpc:
  enabled: true
  registry:
    type: zookeeper

---
# 开发环境
spring:
  profiles: dev
rpc:
  registry:
    address: localhost:2181
  provider:
    enabled: true
    serializer: json

---
# 生产环境
spring:
  profiles: prod
rpc:
  registry:
    address: prod-zk-cluster:2181
  provider:
    enabled: true
    serializer: kryo
```

### 基于条件的Bean创建

框架使用以下条件注解控制Bean的创建：

- `@ConditionalOnProperty(prefix = "rpc", name = "enabled", havingValue = "true")`
- `@ConditionalOnProperty(prefix = "rpc.provider", name = "enabled", havingValue = "true")`
- `@ConditionalOnProperty(prefix = "rpc.consumer", name = "enabled", havingValue = "true")`

## 配置验证

### 必需配置检查

启动时会检查以下必需配置：

1. `rpc.registry.address` - 注册中心地址
2. `rpc.provider.port` - 提供者端口（当provider.enabled=true时）

### 配置冲突检查

- 同一应用不能同时在相同端口启动多个RPC服务器
- 序列化器类型必须在提供者和消费者之间保持一致

## 配置最佳实践

### 1. 环境隔离

```yaml
# 使用不同的注册中心命名空间
rpc:
  registry:
    address: zk:2181/rpc-dev    # 开发环境
    # address: zk:2181/rpc-prod  # 生产环境
```

### 2. 性能优化

```yaml
rpc:
  provider:
    serializer: kryo              # 高性能序列化
    worker-threads: 20            # 根据CPU核心数调整
  consumer:
    connection-pool-size: 50      # 根据并发量调整
  load-balancer: consistent_hash  # 适合有状态服务
```

### 3. 可靠性配置

```yaml
rpc:
  registry:
    session-timeout: 60000        # 增加会话超时时间
    retry-times: 5                # 增加重试次数
  consumer:
    timeout: 10000                # 适当增加超时时间
    retry-count: 3                # 启用重试
```

### 4. 监控配置

```yaml
# 启用Spring Boot Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,rpc
  endpoint:
    health:
      show-details: always

# RPC特定监控配置
rpc:
  metrics:
    enabled: true
    export-interval: 30s
```

## 故障排除

### 配置问题诊断

1. **启动失败**
   ```bash
   # 检查配置是否正确
   java -jar app.jar --debug
   ```

2. **服务注册失败**
   ```yaml
   logging:
     level:
       com.rpc.registry: DEBUG
   ```

3. **连接问题**
   ```yaml
   rpc:
     registry:
       connection-timeout: 30000  # 增加连接超时
     consumer:
       timeout: 10000            # 增加请求超时
   ```

### 常见配置错误

1. **端口冲突**
   ```yaml
   # 错误：与Spring Boot Web端口冲突
   server:
     port: 8080
   rpc:
     provider:
       port: 8080  # 应该使用不同端口
   ```

2. **序列化器不匹配**
   ```yaml
   # 提供者使用kryo，消费者使用json会导致序列化错误
   # 确保两端使用相同的序列化器
   ```

3. **注册中心地址错误**
   ```yaml
   # 错误：使用了不存在的主机名
   rpc:
     registry:
       address: nonexistent-host:2181
   ```

## 配置迁移

### 从1.0.0升级

目前为初始版本，无需迁移。

### 配置兼容性

- 支持Spring Boot 2.x配置格式
- 向后兼容旧版本配置（如果有）

## 参考资源

- [Spring Boot配置文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Zookeeper配置参考](https://zookeeper.apache.org/doc/current/zookeeperAdmin.html)
- [Netty配置优化](https://netty.io/wiki/reference-counted-objects.html)