# LFU负载均衡器使用指南

## 概述

LFU（Least Frequently Used）负载均衡器是一种基于访问频率的负载均衡策略，它会优先选择访问频率最低的服务实例。这种策略特别适用于需要均匀分布请求负载的场景。

## 核心特性

### 1. 频率跟踪
- **精确统计**：记录每个服务实例的访问次数
- **时间戳记录**：跟踪最后访问时间，用于淘汰决策
- **实时更新**：每次服务选择后立即更新频率统计

### 2. 智能选择
- **最低频率优先**：始终选择访问频率最低的服务
- **随机打散**：频率相同时随机选择，避免热点
- **新服务优先**：未跟踪的服务被视为频率为0，优先选择

### 3. 服务淘汰
- **容量限制**：可配置最大跟踪服务数量
- **LFU淘汰策略**：超出容量时淘汰最少使用的服务
- **时间因子**：频率相同时淘汰最久未访问的服务

### 4. 并发安全
- **读写锁**：使用ReentrantReadWriteLock保证线程安全
- **原子操作**：频率更新和服务选择的原子性
- **无锁读取**：频率查询操作无需加锁

## 使用方法

### 基本使用

```java
// 创建LFU负载均衡器，最大跟踪10个服务
LFULoadBalancer loadBalancer = new LFULoadBalancer(10);

// 准备服务列表
List<ServiceInfo> services = Arrays.asList(
    createService("192.168.1.1", 8001),
    createService("192.168.1.2", 8002),
    createService("192.168.1.3", 8003)
);

// 创建RPC请求
RpcRequest request = new RpcRequest();
request.setServiceName("UserService");
request.setMethodName("getUserById");

// 选择服务实例
ServiceInfo selectedService = loadBalancer.select(services, request);
```

### 配置参数

```java
// 默认构造函数，最大跟踪100个服务
LFULoadBalancer loadBalancer = new LFULoadBalancer();

// 自定义最大跟踪数量
LFULoadBalancer loadBalancer = new LFULoadBalancer(50);
```

### 频率统计查询

```java
// 获取所有服务的频率统计
Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();

// 获取特定服务的频率
int frequency = loadBalancer.getServiceFrequency(serviceInfo);

// 获取当前跟踪的服务数量
int count = loadBalancer.getCurrentServiceCount();

// 获取最小频率值
int minFreq = loadBalancer.getMinFrequency();
```

### 统计清理

```java
// 清空所有频率统计
loadBalancer.clearFrequencyStats();
```

## 算法原理

### 1. 数据结构

```
frequencyMap: Map<String, FrequencyNode>
├── serviceKey -> FrequencyNode(frequency, lastAccessTime)

frequencyBuckets: Map<Integer, Set<String>>
├── frequency -> Set<serviceKey>

minFrequency: int
├── 当前最小频率值
```

### 2. 选择流程

```
1. 获取所有活跃服务
2. 计算每个服务的当前频率（未跟踪=0）
3. 找出最小频率值
4. 从最小频率服务中随机选择一个
5. 更新选中服务的频率统计
```

### 3. 淘汰流程

```
1. 检查是否超出最大跟踪数量
2. 找出最小频率的所有服务
3. 从中选择最久未访问的服务
4. 从所有数据结构中移除该服务
5. 更新最小频率值
```

## 适用场景

### ✅ 适合的场景

1. **负载均衡要求高**：需要尽可能均匀分布请求
2. **服务性能差异小**：各服务实例处理能力相近
3. **请求模式稳定**：请求频率相对稳定，没有突发流量
4. **长期运行**：系统长期运行，有足够时间建立频率统计

### ❌ 不适合的场景

1. **服务性能差异大**：不同服务实例处理能力差异很大
2. **突发流量**：有大量突发请求，需要快速响应
3. **短期任务**：任务执行时间短，无法建立有效的频率统计
4. **实时性要求极高**：对响应时间有极严格要求

## 性能特征

### 时间复杂度
- **服务选择**：O(n)，n为活跃服务数量
- **频率更新**：O(1)
- **服务淘汰**：O(k)，k为最小频率服务数量

### 空间复杂度
- **内存占用**：O(m)，m为最大跟踪服务数量
- **额外开销**：每个服务约32字节（频率+时间戳+键值）

### 并发性能
- **读操作**：支持高并发读取
- **写操作**：写操作需要加锁，但执行时间很短
- **扩展性**：随服务数量线性扩展

## 最佳实践

### 1. 容量配置

```java
// 根据实际服务数量设置，建议为服务数量的1.5-2倍
int maxServices = actualServiceCount * 2;
LFULoadBalancer loadBalancer = new LFULoadBalancer(maxServices);
```

### 2. 监控统计

```java
// 定期监控频率分布
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    Map<String, Integer> frequencies = loadBalancer.getServiceFrequencies();
    log.info("当前频率统计：{}", frequencies);
    
    // 检查负载均衡效果
    int maxFreq = frequencies.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    int minFreq = loadBalancer.getMinFrequency();
    double balance = (double) minFreq / maxFreq;
    log.info("负载均衡度：{}", String.format("%.2f", balance));
}, 0, 60, TimeUnit.SECONDS);
```

### 3. 异常处理

```java
try {
    ServiceInfo service = loadBalancer.select(services, request);
    if (service == null) {
        // 处理无可用服务的情况
        throw new NoAvailableServiceException("没有可用的服务实例");
    }
    // 使用选中的服务
} catch (Exception e) {
    log.error("负载均衡选择失败", e);
    // 降级处理
}
```

### 4. 定期清理

```java
// 在适当的时机清理统计数据，如系统维护期间
if (shouldResetStats()) {
    loadBalancer.clearFrequencyStats();
    log.info("已清理LFU频率统计");
}
```

## 与其他策略对比

| 策略 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **LFU** | 负载均衡效果好，长期稳定 | 冷启动慢，不适合突发流量 | 长期运行的稳定系统 |
| **LRU** | 适应性强，响应变化快 | 可能导致负载不均 | 动态变化的系统 |
| **轮询** | 简单高效，负载均匀 | 不考虑服务状态 | 服务性能一致的场景 |
| **随机** | 实现简单，无状态 | 短期内可能不均匀 | 简单的负载分散 |
| **加权** | 考虑服务能力差异 | 需要手动配置权重 | 服务能力差异明显 |

## 故障排查

### 常见问题

1. **负载不均匀**
   - 检查服务列表是否稳定
   - 确认最大跟踪数量设置合理
   - 观察频率统计是否正常累积

2. **性能问题**
   - 检查并发访问量是否过高
   - 确认最大跟踪数量不要过大
   - 考虑是否需要定期清理统计

3. **内存占用过高**
   - 减少最大跟踪服务数量
   - 定期清理频率统计
   - 检查是否有服务泄漏

### 调试信息

```java
// 启用调试日志
<logger name="com.rpc.client.loadbalance.LFULoadBalancer" level="DEBUG"/>

// 输出详细统计信息
log.debug("LFU状态：{}", loadBalancer.toString());
```

## 版本历史

- **v1.0**：初始版本，实现基本的LFU负载均衡功能
- **v1.1**：优化并发性能，添加服务淘汰机制
- **v1.2**：修复服务列表更新时的清理逻辑问题

## 参考资料

- [负载均衡算法详解](./LOAD_BALANCER_ALGORITHMS.md)
- [RPC框架架构设计](./TECHNICAL_ARCHITECTURE.md)
- [性能调优指南](./PERFORMANCE_TUNING.md)