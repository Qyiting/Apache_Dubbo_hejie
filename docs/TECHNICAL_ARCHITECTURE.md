# DistributedX 技术架构详解

## 整体架构设计

### 1. 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application Layer)                │
├─────────────────────────────────────────────────────────────┤
│                    代理层 (Proxy Layer)                     │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │   JDK Proxy     │    │        CGLIB Proxy              │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   服务治理层 (Governance Layer)              │
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Load Balancer  │    │    Service Discovery            │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   序列化层 (Serialization Layer)            │
│  ┌─────────────────┐ ┌─────────────────┐ ┌───────────────┐ │
│  │      JSON       │ │      Kryo       │ │    Hessian    │ │
│  └─────────────────┘ └─────────────────┘ └───────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    传输层 (Transport Layer)                 │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                   Netty NIO                             │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   注册中心层 (Registry Layer)                │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Zookeeper + Curator                        │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 核心模块详解

### 1. dx-core 核心模块

#### 1.1 接口定义
```java
// 服务提供者接口
public interface ServiceProvider {
    void publishService(Object service, String serviceName);
    Object getService(String serviceName);
}

// 服务发现接口
public interface ServiceDiscovery {
    InetSocketAddress lookupService(String serviceName);
    List<InetSocketAddress> lookupServices(String serviceName);
}

// 负载均衡接口
public interface LoadBalancer {
    InetSocketAddress select(List<InetSocketAddress> serviceAddresses, String serviceName);
}
```

#### 1.2 消息协议设计
```java
/**
 * 自定义消息协议
 * +-------+--------+--------+--------+--------+--------+--------+--------+
 * | magic | version| type   | codec  | reqId  | length | body   | checksum|
 * +-------+--------+--------+--------+--------+--------+--------+--------+
 * | 4byte | 1byte  | 1byte  | 1byte  | 4byte  | 4byte  | nbyte  | 4byte   |
 * +-------+--------+--------+--------+--------+--------+--------+--------+
 */
public class DxMessage {
    private int magic = 0xCAFEBABE;  // 魔数
    private byte version = 1;         // 版本号
    private byte messageType;         // 消息类型
    private byte codec;              // 序列化类型
    private int requestId;           // 请求ID
    private int length;              // 消息体长度
    private Object data;             // 消息体
    private int checksum;            // 校验和
}
```

### 2. dx-netty 网络通信模块

#### 2.1 服务端架构
```java
@Component
public class DxNettyServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;
    
    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        bootstrap = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    // 自定义协议编解码器
                    pipeline.addLast(new DxMessageDecoder());
                    pipeline.addLast(new DxMessageEncoder());
                    // 心跳检测
                    pipeline.addLast(new IdleStateHandler(30, 0, 0));
                    // 业务处理器
                    pipeline.addLast(new DxServerHandler());
                }
            });
    }
}
```

#### 2.2 客户端连接池
```java
@Component
public class DxChannelPool {
    private final Map<InetSocketAddress, Channel> channelMap = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, Long> lastActiveTime = new ConcurrentHashMap<>();
    
    public Channel getChannel(InetSocketAddress address) {
        Channel channel = channelMap.get(address);
        if (channel == null || !channel.isActive()) {
            channel = createChannel(address);
            channelMap.put(address, channel);
        }
        lastActiveTime.put(address, System.currentTimeMillis());
        return channel;
    }
    
    // 心跳检测和连接清理
    @Scheduled(fixedRate = 30000)
    public void cleanupInactiveChannels() {
        long currentTime = System.currentTimeMillis();
        channelMap.entrySet().removeIf(entry -> {
            Long lastActive = lastActiveTime.get(entry.getKey());
            return lastActive != null && (currentTime - lastActive) > 60000;
        });
    }
}
```

### 3. dx-serialization 序列化模块

#### 3.1 序列化器工厂
```java
public class SerializerFactory {
    private static final Map<Byte, Serializer> serializers = new HashMap<>();
    
    static {
        serializers.put(SerializerType.JSON, new JsonSerializer());
        serializers.put(SerializerType.KRYO, new KryoSerializer());
        serializers.put(SerializerType.HESSIAN, new HessianSerializer());
    }
    
    public static Serializer getSerializer(byte type) {
        return serializers.get(type);
    }
}
```

#### 3.2 Kryo线程安全优化
```java
public class KryoSerializer implements Serializer {
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        return kryo;
    });
    
    @Override
    public <T> byte[] serialize(T obj) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            return baos.toByteArray();
        }
    }
}
```

### 4. dx-registry 注册中心模块

#### 4.1 Zookeeper服务注册
```java
@Component
public class ZookeeperServiceRegistry implements ServiceRegistry {
    private CuratorFramework zkClient;
    private static final String ROOT_PATH = "/distributedx";
    
    @Override
    public void registerService(String serviceName, InetSocketAddress address) {
        String servicePath = ROOT_PATH + "/" + serviceName;
        String addressPath = servicePath + "/" + address.toString();
        
        try {
            // 创建持久化服务节点
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create()
                    .creatingParentsIfNeeded()
                    .forPath(servicePath);
            }
            
            // 创建临时顺序节点
            zkClient.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(addressPath, address.toString().getBytes());
                
        } catch (Exception e) {
            throw new DxException("Failed to register service", e);
        }
    }
}
```

#### 4.2 服务发现与监听
```java
@Component
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    private final Map<String, List<InetSocketAddress>> serviceCache = new ConcurrentHashMap<>();
    private final Map<String, PathChildrenCache> pathCacheMap = new ConcurrentHashMap<>();
    
    @Override
    public List<InetSocketAddress> lookupServices(String serviceName) {
        List<InetSocketAddress> addresses = serviceCache.get(serviceName);
        if (addresses == null) {
            addresses = loadServiceAddresses(serviceName);
            serviceCache.put(serviceName, addresses);
            watchService(serviceName);
        }
        return new ArrayList<>(addresses);
    }
    
    private void watchService(String serviceName) {
        String servicePath = ROOT_PATH + "/" + serviceName;
        PathChildrenCache pathCache = new PathChildrenCache(zkClient, servicePath, true);
        
        pathCache.getListenable().addListener((client, event) -> {
            // 服务节点变化时更新缓存
            List<InetSocketAddress> newAddresses = loadServiceAddresses(serviceName);
            serviceCache.put(serviceName, newAddresses);
        });
        
        pathCacheMap.put(serviceName, pathCache);
    }
}
```

### 5. 负载均衡策略

#### 5.1 一致性哈希负载均衡
```java
public class ConsistentHashLoadBalancer implements LoadBalancer {
    private final TreeMap<Long, InetSocketAddress> virtualNodes = new TreeMap<>();
    private final int virtualNodeCount = 160;
    
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        if (addresses.isEmpty()) return null;
        
        // 构建虚拟节点环
        buildVirtualNodeRing(addresses);
        
        // 计算服务名的哈希值
        long hash = hash(serviceName);
        
        // 顺时针找到第一个虚拟节点
        Map.Entry<Long, InetSocketAddress> entry = virtualNodes.ceilingEntry(hash);
        if (entry == null) {
            entry = virtualNodes.firstEntry();
        }
        
        return entry.getValue();
    }
    
    private void buildVirtualNodeRing(List<InetSocketAddress> addresses) {
        virtualNodes.clear();
        for (InetSocketAddress address : addresses) {
            for (int i = 0; i < virtualNodeCount; i++) {
                String virtualNodeKey = address.toString() + "#" + i;
                long hash = hash(virtualNodeKey);
                virtualNodes.put(hash, address);
            }
        }
    }
}
```

### 6. 动态代理实现

#### 6.1 JDK动态代理
```java
public class JdkProxyFactory implements ProxyFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz, ServiceInvoker invoker) {
        return (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class<?>[]{clazz},
            new DxInvocationHandler(invoker)
        );
    }
}

class DxInvocationHandler implements InvocationHandler {
    private final ServiceInvoker invoker;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 构建RPC请求
        DxRequest request = DxRequest.builder()
            .serviceName(method.getDeclaringClass().getName())
            .methodName(method.getName())
            .parameterTypes(method.getParameterTypes())
            .parameters(args)
            .build();
            
        // 执行远程调用
        return invoker.invoke(request);
    }
}
```

## 性能优化策略

### 1. 网络层优化
- **零拷贝技术**：使用Netty的直接内存和零拷贝特性
- **连接复用**：实现连接池，避免频繁创建连接
- **批量处理**：支持请求批量发送和响应批量处理

### 2. 序列化优化
- **协议选择**：根据数据特点选择最优序列化协议
- **对象池化**：复用序列化器实例，减少GC压力
- **压缩算法**：支持数据压缩，减少网络传输

### 3. 内存优化
- **对象池**：复用频繁创建的对象
- **缓存策略**：合理使用缓存，避免重复计算
- **内存监控**：实时监控内存使用情况

## 监控与运维

### 1. 性能指标监控
```java
@Component
public class DxMetrics {
    private final Counter requestCounter = Counter.build()
        .name("dx_requests_total")
        .help("Total requests")
        .register();
        
    private final Histogram requestDuration = Histogram.build()
        .name("dx_request_duration_seconds")
        .help("Request duration")
        .register();
        
    public void recordRequest(long duration) {
        requestCounter.inc();
        requestDuration.observe(duration / 1000.0);
    }
}
```

### 2. 健康检查
```java
@RestController
public class HealthController {
    @Autowired
    private DxServer dxServer;
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", dxServer.isRunning() ? "UP" : "DOWN");
        status.put("timestamp", System.currentTimeMillis());
        status.put("activeConnections", dxServer.getActiveConnections());
        return ResponseEntity.ok(status);
    }
}
```

## 扩展点设计

### 1. SPI机制
```java
// 在META-INF/services/com.dx.LoadBalancer文件中配置
com.dx.loadbalance.RandomLoadBalancer
com.dx.loadbalance.RoundRobinLoadBalancer
com.dx.loadbalance.ConsistentHashLoadBalancer

// 动态加载实现
public class LoadBalancerFactory {
    private static final ServiceLoader<LoadBalancer> loader = 
        ServiceLoader.load(LoadBalancer.class);
        
    public static LoadBalancer getLoadBalancer(String type) {
        for (LoadBalancer balancer : loader) {
            if (balancer.getType().equals(type)) {
                return balancer;
            }
        }
        throw new IllegalArgumentException("Unknown load balancer type: " + type);
    }
}
```

---

*本文档详细介绍了DistributedX框架的技术架构和实现细节，为深入理解和扩展框架提供了完整的技术参考。*