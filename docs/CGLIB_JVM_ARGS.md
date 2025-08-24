# CGLIB Java 9+ 模块系统解决方案

## 问题原因

你遇到的错误：
```
java.lang.reflect.InaccessibleObjectException: Unable to make protected final java.lang.Class java.lang.ClassLoader.defineClass... accessible: module java.base does not "opens java.lang" to unnamed module
```

这是由于Java 9+引入的模块系统（JPMS）限制了反射访问，CGLIB无法访问java.lang.ClassLoader的defineClass方法。

## 解决方案

### 方法1：添加JVM启动参数（推荐）

在运行程序时添加以下JVM参数：

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

### 方法2：Maven配置

在`pom.xml`中添加Maven插件配置：

```xml

<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <configuration>
        <mainClass>com.rpc.example.clientexam.CglibClientExample</mainClass>
        <arguments>
            <argument>--add-opens</argument>
            <argument>java.base/java.lang=ALL-UNNAMED</argument>
            <argument>--add-opens</argument>
            <argument>java.base/java.lang.reflect=ALL-UNNAMED</argument>
            <argument>--add-opens</argument>
            <argument>java.base/java.util=ALL-UNNAMED</argument>
        </arguments>
    </configuration>
</plugin>
```

### 方法3：IDE配置

#### IntelliJ IDEA
1. 打开Run/Debug Configurations
2. 在VM options中添加：
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

#### Eclipse
1. 右键项目 → Run As → Run Configurations
2. 在Arguments标签页的VM arguments中添加上述参数

### 方法4：使用Java 8

如果可能，使用Java 8运行项目，因为Java 8没有模块系统限制。

## 运行示例

### 命令行运行
```bash
java --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     -cp target/classes:../rpc-client/target/classes:../rpc-core/target/classes:{其他依赖路径} \
     com.rpc.example.CglibClientExam
```

### Maven运行
```bash
mvn exec:java -Dexec.mainClass="com.rpc.example.clientexam.CglibClientExample" \
              -Dexec.args="--add-opens java.base/java.lang=ALL-UNNAMED \
                          --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
                          --add-opens java.base/java.util=ALL-UNNAMED"
```

## 验证修复

运行以下命令验证：
```bash
java --add-opens java.base/java.lang=ALL-UNNAMED --version
```

如果成功，应该显示Java版本信息而没有错误。

## 注意事项

1. 这些参数只在Java 9+需要
2. 生产环境部署时也需要添加这些参数
3. 考虑使用JDK动态代理作为替代方案，如果CGLIB不是必须的