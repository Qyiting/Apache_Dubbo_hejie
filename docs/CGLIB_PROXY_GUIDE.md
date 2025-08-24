# CGLIB动态代理实现指南

## 概述

本项目已经支持从JDK动态代理切换到CGLIB动态代理。CGLIB（Code Generation Library）是一个强大的、高性能的代码生成库，可以在运行时扩展Java类和实现接口。

## 主要区别

| 特性 | JDK动态代理 | CGLIB动态代理 |
|------|-------------|---------------|
| 代理类型 | 只能代理接口 | 可以代理类和接口 |
| 实现方式 | 基于接口实现 | 基于继承实现 |
| 性能 | 一般 | 更好 |
| 依赖 | 无额外依赖 | 需要CGLIB库 |
| 限制 | 目标类必须实现接口 | 目标类不能是final类 |

## 依赖配置

项目已经在根`pom.xml`中添加了CGLIB依赖：

```xml
<!--CGLIB动态代理-->
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib</artifactId>
    <version>3.3.0</version>
</dependency>
```

## 使用方式

### 1. 创建CGLIB代理工厂

使用`CglibRpcProxyFactory`类来创建CGLIB代理：

```java
import com.rpc.client.factory.CglibRpcProxyFactory;
import com.rpc.client.RpcClient;

// 创建RPC客户端
RpcClient rpcClient = new RpcClient();

// 创建CGLIB代理工厂
CglibRpcProxyFactory proxyFactory = new CglibRpcProxyFactory(rpcClient);

// 创建代理对象
YourServiceClass proxy = proxyFactory.createProxy(YourServiceClass.class);
```

### 2. 基本用法示例

```java
// 创建服务代理
UserService userService = proxyFactory.createProxy(UserServiceImpl.class);

// 调用服务方法
User user = userService.getUserById(1L);
```

### 3. 高级配置

可以指定版本、分组和超时时间：

```java
// 指定版本和分组
UserService userService = proxyFactory.createProxy(
    UserServiceImpl.class, 
    "2.0.0", 
    "test-group", 
    10000L  // 10秒超时
);
```

### 4. 异步调用

CGLIB也支持异步调用：

```java
// 创建异步代理
AsyncUserService asyncUserService = proxyFactory.createAsyncProxy(AsyncUserServiceImpl.class);

// 异步调用
CompletableFuture<User> future = asyncUserService.getUserById(1L);
future.thenAccept(user -> {
    System.out.println("异步调用结果: " + user);
});
```

## 注意事项

1. **类限制**：CGLIB不能代理final类或final方法
2. **构造函数**：被代理的类必须有无参构造函数
3. **性能考虑**：CGLIB通常比JDK动态代理性能更好，但会生成更多类
4. **内存使用**：CGLIB生成的代理类会占用更多内存

## 与JDK动态代理对比

### JDK动态代理示例
```java
// 需要接口
RpcProxyFactory factory = new RpcProxyFactory(rpcClient);
UserService userService = factory.createProxy(UserService.class); // 接口
```

### CGLIB动态代理示例
```java
// 可以直接代理类
CglibRpcProxyFactory factory = new CglibRpcProxyFactory(rpcClient);
UserServiceImpl userService = factory.createProxy(UserServiceImpl.class); // 类
```

## 迁移指南

从JDK动态代理迁移到CGLIB动态代理的步骤：

1. **添加依赖**：确保项目中包含CGLIB依赖
2. **替换工厂类**：将`RpcProxyFactory`替换为`CglibRpcProxyFactory`
3. **调整代理目标**：从接口改为具体类
4. **测试验证**：确保所有功能正常工作

## 示例代码

参考`CglibClientExample.java`中的完整示例代码。

## 常见问题

### Q: CGLIB代理失败怎么办？
A: 检查目标类是否为final类，或者是否有final方法

### Q: 性能如何？
A: CGLIB通常比JDK动态代理性能好，特别是在大量调用的情况下

### Q: 是否需要修改服务端？
A: 不需要，CGLIB代理只在客户端使用，服务端保持不变