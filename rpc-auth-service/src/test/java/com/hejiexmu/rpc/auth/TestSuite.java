package com.hejiexmu.rpc.auth;

import com.hejiexmu.rpc.auth.controller.AuthControllerIntegrationTest;
import com.hejiexmu.rpc.auth.service.AuthServiceTest;
import com.hejiexmu.rpc.auth.performance.PerformanceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.ExcludeTags;

/**
 * 测试套件
 * 组织和运行所有测试用例
 * 
 * @author hejiexmu
 */
@Suite
@SuiteDisplayName("RPC Auth Service Test Suite")
@SelectClasses({
    AuthServiceTest.class,
    AuthControllerIntegrationTest.class
})
public class TestSuite {
    // 测试套件类，用于组织测试
}

/**
 * 单元测试套件
 */
@Suite
@SuiteDisplayName("Unit Tests")
@SelectClasses({
    AuthServiceTest.class
})
@IncludeTags("unit")
class UnitTestSuite {
}

/**
 * 集成测试套件
 */
@Suite
@SuiteDisplayName("Integration Tests")
@SelectClasses({
    AuthControllerIntegrationTest.class
})
@IncludeTags("integration")
class IntegrationTestSuite {
}

/**
 * 性能测试套件
 * 注意：性能测试默认被禁用，需要手动运行
 */
@Suite
@SuiteDisplayName("Performance Tests")
@SelectClasses({
    PerformanceTest.class
})
@IncludeTags("performance")
class PerformanceTestSuite {
}