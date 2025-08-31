# RPC认证服务测试指南

本目录包含RPC认证服务的所有测试用例，包括单元测试、集成测试和性能测试。

## 测试结构

```
src/test/java/com/hejiexmu/rpc/auth/
├── config/
│   └── TestConfig.java              # 测试配置类
├── controller/
│   └── AuthControllerIntegrationTest.java  # 控制器集成测试
├── service/
│   └── AuthServiceTest.java         # 服务层单元测试
├── performance/
│   └── PerformanceTest.java         # 性能测试
└── TestSuite.java                   # 测试套件
```

## 运行测试

### 1. 运行所有测试

```bash
# 使用Maven运行所有测试
mvn test

# 使用Gradle运行所有测试
./gradlew test
```

### 2. 运行特定类型的测试

#### 单元测试
```bash
# Maven
mvn test -Dtest=UnitTestSuite

# Gradle
./gradlew test --tests "*UnitTestSuite"
```

#### 集成测试
```bash
# Maven
mvn test -Dtest=IntegrationTestSuite

# Gradle
./gradlew test --tests "*IntegrationTestSuite"
```

#### 性能测试（需要手动启用）
```bash
# Maven - 移除@Disabled注解后运行
mvn test -Dtest=PerformanceTest

# Gradle - 移除@Disabled注解后运行
./gradlew test --tests "*PerformanceTest"
```

### 3. 运行特定测试方法

```bash
# Maven
mvn test -Dtest=AuthServiceTest#testLoginSuccess

# Gradle
./gradlew test --tests "AuthServiceTest.testLoginSuccess"
```

## 测试配置

### 测试环境配置

测试使用独立的配置文件 `application-test.yml`，包含：
- 内存数据库配置
- Mock Redis配置
- 测试专用的安全配置
- 日志配置

### Mock配置

`TestConfig.java` 提供了测试所需的Mock Bean：
- `RedisTemplate`
- `RedisCacheService`
- `masterDataSource`
- `slaveDataSource`
- `BCryptPasswordEncoder`（低强度，提高测试速度）

## 测试覆盖率

### 生成测试覆盖率报告

```bash
# Maven with JaCoCo
mvn clean test jacoco:report

# Gradle with JaCoCo
./gradlew test jacocoTestReport
```

覆盖率报告将生成在：
- Maven: `target/site/jacoco/index.html`
- Gradle: `build/reports/jacoco/test/html/index.html`

### 覆盖率目标

- 行覆盖率：≥ 80%
- 分支覆盖率：≥ 70%
- 方法覆盖率：≥ 85%

## 测试类说明

### AuthServiceTest

**测试范围：**
- 用户登录功能
- 用户注册功能
- 令牌刷新功能
- 用户登出功能
- 密码修改功能

**测试场景：**
- 成功场景测试
- 失败场景测试（用户不存在、密码错误、账户锁定等）
- 边界条件测试
- 异常处理测试

### AuthControllerIntegrationTest

**测试范围：**
- HTTP请求处理
- 请求参数验证
- 响应格式验证
- 错误处理

**测试场景：**
- 各种HTTP状态码
- JSON请求和响应
- 参数验证
- 异常情况处理

### PerformanceTest

**测试范围：**
- 登录性能测试
- 注册性能测试
- JWT令牌生成性能测试
- 内存使用测试

**性能指标：**
- 吞吐量（TPS）
- 响应时间
- 成功率
- 内存使用

**注意：** 性能测试默认被 `@Disabled` 注解禁用，需要手动启用后运行。

## 测试数据

### 测试用户数据

```java
// 有效用户
username: "testuser"
password: "password123"
email: "test@example.com"

// 锁定用户
username: "lockeduser"
password: "password123"
status: LOCKED

// 禁用用户
username: "disableduser"
password: "password123"
status: DISABLED
```

### 测试令牌

```java
// 有效令牌
String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

// 过期令牌
String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

// 无效令牌
String invalidToken = "invalid.token.format";
```

## 持续集成

### GitHub Actions配置示例

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        distribution: 'adopt'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v2
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    
    - name: Run tests
      run: mvn clean test
    
    - name: Generate test report
      run: mvn jacoco:report
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v1
```

## 故障排除

### 常见问题

1. **测试数据库连接失败**
   - 检查测试配置文件中的数据库配置
   - 确保测试数据库服务正在运行

2. **Redis连接失败**
   - 检查Redis Mock配置
   - 确保TestConfig中的Mock Bean正确配置

3. **测试超时**
   - 检查测试方法的超时设置
   - 优化测试数据和Mock响应时间

4. **内存不足**
   - 增加JVM堆内存：`-Xmx2g`
   - 优化测试数据大小

### 调试技巧

1. **启用详细日志**
   ```properties
   logging.level.com.hejiexmu.rpc.auth=DEBUG
   logging.level.org.springframework.test=DEBUG
   ```

2. **使用测试切片**
   ```java
   @WebMvcTest(AuthController.class)
   @DataJpaTest
   @JsonTest
   ```

3. **Mock验证**
   ```java
   verify(authService, times(1)).login(any(LoginRequest.class));
   verifyNoMoreInteractions(authService);
   ```

## 最佳实践

1. **测试命名**
   - 使用描述性的测试方法名
   - 遵循 `should_ExpectedBehavior_When_StateUnderTest` 模式

2. **测试组织**
   - 每个测试方法只测试一个功能点
   - 使用 `@BeforeEach` 和 `@AfterEach` 进行测试准备和清理

3. **断言**
   - 使用具体的断言而不是通用的assertTrue
   - 提供有意义的断言消息

4. **测试数据**
   - 使用测试构建器模式创建测试数据
   - 避免硬编码测试数据

5. **Mock使用**
   - 只Mock外部依赖
   - 验证Mock的交互
   - 重置Mock状态

## 参考资料

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)