# RPC Framework Infrastructure Setup

本目录包含了RPC框架所需的基础设施配置，包括MySQL主从复制和Redis集群。

## 架构概述

### MySQL 主从架构
- **主库 (Master)**: 端口 3306，负责写操作
- **从库 (Slave)**: 端口 3307，负责读操作
- **复制方式**: GTID-based 复制，确保数据一致性

### Redis 集群架构
- **6个节点**: 端口 7001-7006
- **3主3从**: 每个主节点有一个从节点
- **分片策略**: 哈希槽分片，支持高并发

## 快速启动

### 前置条件
- Docker 和 Docker Compose 已安装
- 端口 3306, 3307, 7001-7006 未被占用

### 启动步骤

#### Windows 系统
```bash
cd infrastructure
start-infrastructure.bat
```

#### Linux/Mac 系统
```bash
cd infrastructure
chmod +x start-infrastructure.sh
./start-infrastructure.sh
```

#### 手动启动
```bash
cd infrastructure
docker-compose up -d
```

## 服务配置

### MySQL 配置

#### 连接信息
- **主库地址**: localhost:3306
- **从库地址**: localhost:3307
- **数据库名**: rpc_auth
- **管理员**: root / root123
- **应用用户**: rpc_app / rpc_app_pass123

#### 数据库表结构
- `users`: 用户基本信息
- `roles`: 角色定义
- `user_roles`: 用户角色关联
- `permissions`: 权限定义
- `role_permissions`: 角色权限关联
- `user_sessions`: 用户会话记录
- `rpc_service_permissions`: RPC服务权限配置

#### 默认数据
- 管理员用户: admin / admin123
- 默认角色: ADMIN, USER, GUEST
- 基础权限: USER_READ, USER_WRITE, USER_DELETE, RPC_CALL, ADMIN_ACCESS

### Redis 配置

#### 集群信息
- **节点地址**: localhost:7001-7006
- **集群模式**: 启用
- **持久化**: AOF + RDB
- **内存策略**: allkeys-lru

#### 连接示例
```java
// Java 连接示例
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("localhost", 7001));
nodes.add(new HostAndPort("localhost", 7002));
nodes.add(new HostAndPort("localhost", 7003));
JedisCluster jedisCluster = new JedisCluster(nodes);
```

## 验证安装

### 验证MySQL主从复制
```sql
-- 在主库执行
SHOW MASTER STATUS;

-- 在从库执行
SHOW SLAVE STATUS\G
```

### 验证Redis集群
```bash
# 检查集群状态
docker exec redis-node-1 redis-cli -p 7001 cluster nodes

# 测试数据写入和读取
docker exec redis-node-1 redis-cli -p 7001 set test "hello"
docker exec redis-node-2 redis-cli -p 7002 get test
```

## 常用命令

### Docker Compose 命令
```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 查看服务状态
docker-compose ps

# 查看服务日志
docker-compose logs mysql-master
docker-compose logs redis-node-1

# 重启特定服务
docker-compose restart mysql-master
```

### MySQL 管理命令
```bash
# 连接到主库
docker exec -it mysql-master mysql -uroot -proot123

# 连接到从库
docker exec -it mysql-slave mysql -uroot -proot123

# 备份数据库
docker exec mysql-master mysqldump -uroot -proot123 rpc_auth > backup.sql
```

### Redis 管理命令
```bash
# 连接到Redis节点
docker exec -it redis-node-1 redis-cli -p 7001

# 查看集群信息
docker exec redis-node-1 redis-cli -p 7001 cluster info

# 查看节点信息
docker exec redis-node-1 redis-cli -p 7001 cluster nodes
```

## 故障排除

### MySQL 主从同步问题
1. 检查网络连接
2. 验证复制用户权限
3. 查看错误日志
```bash
docker logs mysql-master
docker logs mysql-slave
```

### Redis 集群问题
1. 检查所有节点是否启动
2. 验证集群配置
3. 重新创建集群
```bash
docker-compose down
docker-compose up -d
```

### 端口冲突
如果端口被占用，可以修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "3307:3306"  # 将MySQL主库映射到3307端口
```

## 生产环境注意事项

1. **安全配置**
   - 修改默认密码
   - 启用SSL/TLS
   - 配置防火墙规则

2. **性能优化**
   - 调整MySQL缓冲池大小
   - 配置Redis内存限制
   - 监控资源使用情况

3. **备份策略**
   - 定期备份MySQL数据
   - 配置Redis持久化
   - 测试恢复流程

4. **监控告警**
   - 监控主从延迟
   - 监控Redis集群健康状态
   - 设置资源使用告警

## 扩展配置

### 添加更多Redis节点
1. 在 `docker-compose.yml` 中添加新节点
2. 创建对应的配置文件
3. 重新平衡集群槽位

### MySQL 读写分离
应用程序中可以配置多数据源：
- 写操作连接主库 (3306)
- 读操作连接从库 (3307)

### 高可用配置
- MySQL: 可以配置MHA或Galera集群
- Redis: 可以配置Sentinel模式