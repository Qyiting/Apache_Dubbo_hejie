# 一致性哈希负载均衡器使用指南

## 概述

一致性哈希负载均衡器（ConsistentHashLoadBalancer）是一种基于一致性哈希算法的负载均衡实现，它能够在服务实例变化时保持请求路由的相对稳定性，减少缓存失效和数据重新分布的影响。

## 特性

- **一致性路由**：相同的请求总是路由到相同的服务实例
- **虚拟节点**：通过虚拟节点提高负载分布的均匀性
- **动态扩缩容**：服务实例的增减只影响相邻节点的负载
- **高性能**：基于TreeMap实现的高效哈希环查找
- **可配置**：支持自定义虚拟节点数量

## 算法原理

### 1. 哈希环构建
- 将服务实例通过哈希函数映射到一个环形空间
- 每个服务实例创建多个虚拟节点分布在环上
- 使用MD5哈希算法确保分布的随机性

### 2. 请求路由
- 对请求计算哈希值
- 在哈希环上顺时针查找第一个大于等于请求哈希值的虚拟节点
- 返回该虚拟节点对应的真实服务实例

### 3. 负载均衡
- 通过虚拟节点数量控制负载分布的均匀性
- 默认每个服务实例创建160个虚拟节点
- 支持动态调整虚拟节点数量

## 使用方法

### 1. 基本使用

```java
// 创建一致性哈希负载均衡器
LoadBalancer loadBalancer = new ConsistentHashLoadBalancer();

// 创建RPC客户端
RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer);
```

### 2. 自定义虚拟节点数量

```java
// 创建一致性哈希负载均衡器，设置虚拟节点数量为200
ConsistentHashLoadBalancer loadBalancer = new ConsistentHashLoadBalancer(200);
```

### 3. 完整示例

```java
public class ConsistentHashExample {
    public static void main(String[] args) {
        try {
            // 1. 创建服务注册中心
            ServiceRegistry serviceRegistry = new ZookeeperServiceRegistry("localhost:2181");
            
            // 2. 创建一致性哈希负载均衡器
            LoadBalancer loadBalancer = new ConsistentHashLoadBalancer();
            
            // 3. 创建RPC客户端
            RpcClient rpcClient = new RpcClient(serviceRegistry, loadBalancer);
            
            // 4. 创建服务代理
            RpcProxyFactory proxyFactory = new RpcProxyFactory(rpcClient);
            UserService userService = proxyFactory.createProxy(UserService.class);
            
            // 5. 调用服务
            User user = userService.getUserById(1L);
            System.out.println("查询结果：" + user);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| virtualNodeCount | int | 160 | 每个服务实例的虚拟节点数量 |

## 性能特点

### 时间复杂度
- 服务选择：O(log n)，其中n为虚拟节点总数
- 虚拟节点环构建：O(m * k)，其中m为服务实例数，k为虚拟节点数

### 空间复杂度
- O(m * k)，其中m为服务实例数，k为虚拟节点数

### 负载均衡效果
- 虚拟节点数量越多，负载分布越均匀
- 推荐虚拟节点数量：100-200
- 服务实例变化时，只有相邻节点的负载会受影响

## 适用场景

### 适合使用的场景
1. **有状态服务**：需要将相同用户的请求路由到同一服务实例
2. **缓存场景**：减少因服务实例变化导致的缓存失效
3. **会话保持**：需要保持用户会话的一致性
4. **数据分片**：按照某种规则将数据分布到不同的服务实例

### 不适合使用的场景
1. **无状态服务**：服务实例间完全无状态，使用轮询或随机更简单
2. **负载差异大**：服务实例处理能力差异很大时，可能导致负载不均
3. **实时性要求高**：哈希计算会带来微小的性能开销

## 监控和调试

### 日志输出
一致性哈希负载均衡器会输出详细的调试日志：

```
[DEBUG] 一致性哈希负载均衡选择服务实例：192.168.1.1:8080 (哈希键：user123, 哈希值：1234567890)
[INFO] 一致性哈希负载均衡器初始化完成，虚拟节点数量：160
[WARN] 没有活跃的服务实例可用于负载均衡选择
```

### 状态查询

```java
ConsistentHashLoadBalancer loadBalancer = new ConsistentHashLoadBalancer();

// 获取虚拟节点数量
int virtualNodeCount = loadBalancer.getVirtualNodeCount();

// 获取当前虚拟节点环大小
int ringSize = loadBalancer.getVirtualNodeRingSize();

// 清空虚拟节点环（用于测试）
loadBalancer.clearVirtualNodeRing();
```

## 最佳实践

1. **合理设置虚拟节点数量**：根据服务实例数量和负载均衡要求调整
2. **监控负载分布**：定期检查各服务实例的负载分布情况
3. **渐进式扩缩容**：避免同时大量增减服务实例
4. **请求标识选择**：选择合适的请求标识作为哈希键
5. **异常处理**：处理服务实例不可用的情况

## 与其他负载均衡算法的比较

| 算法 | 一致性 | 负载均衡 | 实现复杂度 | 适用场景 |
|------|--------|----------|------------|----------|
| 轮询 | 无 | 优秀 | 简单 | 无状态服务 |
| 随机 | 无 | 良好 | 简单 | 无状态服务 |
| 一致性哈希 | 优秀 | 良好 | 中等 | 有状态服务、缓存 |
| 加权轮询 | 无 | 优秀 | 中等 | 服务能力不同 |
| 最少连接 | 无 | 优秀 | 中等 | 长连接服务 |

## 故障排除

### 常见问题

1. **负载不均匀**
   - 检查虚拟节点数量是否足够
   - 确认服务实例数量和分布
   - 检查哈希键的选择是否合理

2. **请求路由不一致**
   - 确认请求标识的生成逻辑
   - 检查服务实例列表是否稳定
   - 验证哈希算法的实现

3. **性能问题**
   - 检查虚拟节点数量是否过多
   - 监控哈希计算的耗时
   - 考虑使用缓存优化

### 调试技巧

1. 启用DEBUG日志查看详细的路由信息
2. 使用单元测试验证算法的正确性
3. 监控服务实例的请求分布情况
4. 使用压力测试验证性能表现