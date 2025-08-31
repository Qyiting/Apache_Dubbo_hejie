# Simple RPC Framework 部署指南

## 概述

本文档提供了 Simple RPC Framework 的完整部署指南，包括环境准备、依赖安装、配置和启动步骤。

## 系统要求

### 硬件要求
- CPU: 2核心以上
- 内存: 4GB以上
- 磁盘: 20GB可用空间

### 软件要求
- Java: JDK 17 或更高版本
- Maven: 3.6.0 或更高版本
- Docker: 20.10 或更高版本（可选，用于容器化部署）
- Docker Compose: 1.29 或更高版本（可选）

## 环境准备

### 1. 安装 Java

```bash
# 检查 Java 版本
java -version

# 如果未安装，请下载并安装 JDK 17
# Windows: 从 Oracle 或 OpenJDK 官网下载安装包
# Linux: sudo apt-get install openjdk-17-jdk
# macOS: brew install openjdk@17
```

### 2. 安装 Maven

```bash
# 检查 Maven 版本
mvn -version

# 如果未安装，请从 Apache Maven 官网下载
# 配置环境变量 MAVEN_HOME 和 PATH
```

### 3. 安装 Docker（可选）

```bash
# 检查 Docker 版本
docker --version
docker-compose --version

# 如果未安装，请从 Docker 官网下载安装
```

## 基础设施部署

### 使用 Docker Compose（推荐）

1. 进入项目根目录：
```bash
cd simple-rpc-framework
```

2. 启动基础设施服务：
```bash
# Windows
cd infrastructure
.\start-infrastructure.bat

# Linux/macOS
cd infrastructure
./start-infrastructure.sh
```

这将启动以下服务：
- MySQL 主从复制集群（主库端口：3306，从库端口：3307）
- Redis 集群（端口：6379-6384）
- Zookeeper（端口：2181）

### 手动安装基础设施

#### MySQL 配置

1. 安装 MySQL 8.0+
2. 创建数据库和用户：

```sql
-- 创建数据库
CREATE DATABASE rpc_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'rpc_user'@'%' IDENTIFIED BY 'rpc_password';
GRANT ALL PRIVILEGES ON rpc_auth.* TO 'rpc_user'@'%';
FLUSH PRIVILEGES;
```

3. 执行数据库初始化脚本：
```bash
mysql -u rpc_user -p rpc_auth < infrastructure/mysql/init/01-create-tables.sql
mysql -u rpc_user -p rpc_auth < infrastructure/mysql/init/02-insert-data.sql
```

#### Redis 配置

1. 安装 Redis 6.0+
2. 配置 Redis 集群或单机模式
3. 确保 Redis 服务运行在端口 6379

#### Zookeeper 配置

1. 安装 Zookeeper 3.7+
2. 配置 Zookeeper 服务
3. 确保 Zookeeper 服务运行在端口 2181

## 应用部署

### 1. 编译项目

```bash
# 进入项目根目录
cd simple-rpc-framework

# 清理并编译项目
mvn clean compile

# 运行测试
mvn test

# 打包项目
mvn package -DskipTests
```

### 2. 部署认证服务

#### 配置文件

创建 `application.yml` 配置文件：

```yaml
server:
  port: 8080

spring:
  application:
    name: rpc-auth-service
  
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/rpc_auth?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: rpc_user
    password: rpc_password
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
  
  redis:
    host: localhost
    port: 6379
    password: 
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 2000ms

rpc:
  auth:
    jwt:
      secret: your-secret-key-here-must-be-at-least-256-bits
      expiration: 86400000  # 24 hours
      refresh-expiration: 604800000  # 7 days
    session:
      timeout: 1800  # 30 minutes
    password:
      min-length: 8
      require-uppercase: true
      require-lowercase: true
      require-digit: true
      require-special: true

logging:
  level:
    com.hejiexmu.rpc: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/rpc-auth-service.log
    max-size: 100MB
    max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 启动认证服务

```bash
# 方式1：使用 Maven 启动
cd rpc-auth-service
mvn spring-boot:run

# 方式2：使用 JAR 包启动
java -jar rpc-auth-service/target/rpc-auth-service-1.0.0.jar

# 方式3：使用配置文件启动
java -jar rpc-auth-service/target/rpc-auth-service-1.0.0.jar --spring.config.location=classpath:/application.yml
```

### 3. 部署示例应用

#### 启动服务提供者

```bash
cd rpc-spring-boot-samples/rpc-spring-boot-provider-sample
mvn spring-boot:run
```

#### 启动服务消费者

```bash
cd rpc-spring-boot-samples/rpc-spring-boot-consumer-sample
mvn spring-boot:run
```

## 配置说明

### 环境变量配置

可以通过环境变量覆盖配置：

```bash
# 数据库配置
export SPRING_DATASOURCE_URL=jdbc:mysql://your-mysql-host:3306/rpc_auth
export SPRING_DATASOURCE_USERNAME=your-username
export SPRING_DATASOURCE_PASSWORD=your-password

# Redis 配置
export SPRING_REDIS_HOST=your-redis-host
export SPRING_REDIS_PORT=6379
export SPRING_REDIS_PASSWORD=your-redis-password

# JWT 配置
export RPC_AUTH_JWT_SECRET=your-jwt-secret-key
export RPC_AUTH_JWT_EXPIRATION=86400000
```

### 生产环境配置建议

1. **安全配置**：
   - 使用强密码和复杂的 JWT 密钥
   - 启用 HTTPS
   - 配置防火墙规则

2. **性能配置**：
   - 调整数据库连接池大小
   - 配置 Redis 集群
   - 启用应用监控

3. **高可用配置**：
   - 部署多个认证服务实例
   - 配置负载均衡器
   - 设置数据库主从复制

## 验证部署

### 1. 健康检查

```bash
# 检查认证服务健康状态
curl http://localhost:8080/actuator/health

# 检查应用信息
curl http://localhost:8080/actuator/info
```

### 2. 功能测试

```bash
# 用户注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!",
    "email": "test@example.com"
  }'

# 用户登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接字符串和凭据
   - 检查网络连接和防火墙设置

2. **Redis 连接失败**
   - 检查 Redis 服务是否启动
   - 验证 Redis 配置
   - 检查网络连接

3. **应用启动失败**
   - 检查 Java 版本
   - 查看应用日志
   - 验证配置文件格式

### 日志查看

```bash
# 查看应用日志
tail -f logs/rpc-auth-service.log

# 查看 Docker 容器日志
docker-compose logs -f mysql-master
docker-compose logs -f redis-node-1
```

## 监控和维护

### 监控指标

- 应用健康状态：`/actuator/health`
- 性能指标：`/actuator/metrics`
- Prometheus 指标：`/actuator/prometheus`

### 日志管理

- 应用日志：`logs/rpc-auth-service.log`
- 日志轮转：自动按大小和时间轮转
- 日志级别：可通过配置文件调整

### 备份策略

1. **数据库备份**：
   ```bash
   mysqldump -u rpc_user -p rpc_auth > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Redis 备份**：
   ```bash
   redis-cli BGSAVE
   ```

## 扩展部署

### 集群部署

1. 部署多个认证服务实例
2. 配置负载均衡器（如 Nginx）
3. 使用共享的 Redis 集群
4. 配置数据库读写分离

### 容器化部署

```dockerfile
# Dockerfile 示例
FROM openjdk:17-jre-slim
VOLUME /tmp
COPY target/rpc-auth-service-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
# 构建镜像
docker build -t rpc-auth-service:1.0.0 .

# 运行容器
docker run -p 8080:8080 rpc-auth-service:1.0.0
```

## 安全建议

1. **网络安全**：
   - 使用 HTTPS
   - 配置防火墙
   - 限制数据库访问

2. **应用安全**：
   - 定期更新依赖
   - 使用强密码策略
   - 启用审计日志

3. **运维安全**：
   - 定期备份数据
   - 监控异常访问
   - 及时应用安全补丁

## 联系支持

如果在部署过程中遇到问题，请：

1. 查看项目文档：`docs/` 目录
2. 检查 GitHub Issues
3. 联系开发团队

---

**注意**：本部署指南适用于 Simple RPC Framework v1.0.0，不同版本可能存在差异，请参考对应版本的文档。