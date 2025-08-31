package com.hejiexmu.rpc.auth.aspect;

import com.hejiexmu.rpc.auth.annotation.DataSource;
import com.hejiexmu.rpc.auth.config.DataSourceConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切面
 * 实现基于注解的读写分离自动切换
 * 
 * @author hejiexmu
 */
@Aspect
@Component
public class DataSourceAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);
    
    /**
     * 切点：所有带有@DataSource注解的方法
     */
    @Pointcut("@annotation(com.hejiexmu.rpc.auth.annotation.DataSource)")
    public void dataSourcePointcut() {
    }
    
    /**
     * 切点：所有Repository类的方法
     */
    @Pointcut("execution(* com.hejiexmu.rpc.auth.repository..*(..))") 
    public void repositoryPointcut() {
    }
    
    /**
     * 环绕通知：处理数据源切换
     */
    @Around("dataSourcePointcut() || repositoryPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        DataSourceConfig.DataSourceType dataSourceType = determineDataSourceType(point);
        
        try {
            // 设置数据源
            DataSourceConfig.DataSourceContextHolder.setDataSourceType(dataSourceType);
            
            logger.debug("切换到数据源: {}, 方法: {}", 
                        dataSourceType, 
                        point.getSignature().toShortString());
            
            // 执行目标方法
            return point.proceed();
            
        } finally {
            // 清除数据源设置
            DataSourceConfig.DataSourceContextHolder.clearDataSourceType();
            
            logger.debug("清除数据源设置: {}", point.getSignature().toShortString());
        }
    }
    
    /**
     * 确定数据源类型
     */
    private DataSourceConfig.DataSourceType determineDataSourceType(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        
        // 1. 首先检查方法级别的@DataSource注解
        DataSource dataSourceAnnotation = AnnotationUtils.findAnnotation(method, DataSource.class);
        if (dataSourceAnnotation != null) {
            return dataSourceAnnotation.value();
        }
        
        // 2. 检查类级别的@DataSource注解
        Class<?> targetClass = point.getTarget().getClass();
        dataSourceAnnotation = AnnotationUtils.findAnnotation(targetClass, DataSource.class);
        if (dataSourceAnnotation != null) {
            return dataSourceAnnotation.value();
        }
        
        // 3. 根据方法名自动判断
        String methodName = method.getName();
        if (isReadOperation(methodName)) {
            return DataSourceConfig.DataSourceType.SLAVE;
        } else if (isWriteOperation(methodName)) {
            return DataSourceConfig.DataSourceType.MASTER;
        }
        
        // 4. 根据事务只读属性判断
        if (isReadOnlyTransaction()) {
            return DataSourceConfig.DataSourceType.SLAVE;
        }
        
        // 5. 默认使用主数据源
        return DataSourceConfig.DataSourceType.MASTER;
    }
    
    /**
     * 判断是否为读操作
     */
    private boolean isReadOperation(String methodName) {
        return methodName.startsWith("find") ||
               methodName.startsWith("get") ||
               methodName.startsWith("query") ||
               methodName.startsWith("select") ||
               methodName.startsWith("search") ||
               methodName.startsWith("count") ||
               methodName.startsWith("exists") ||
               methodName.startsWith("list") ||
               methodName.startsWith("page") ||
               methodName.startsWith("load") ||
               methodName.startsWith("read") ||
               methodName.startsWith("check") ||
               methodName.startsWith("validate") ||
               methodName.startsWith("verify");
    }
    
    /**
     * 判断是否为写操作
     */
    private boolean isWriteOperation(String methodName) {
        return methodName.startsWith("save") ||
               methodName.startsWith("insert") ||
               methodName.startsWith("add") ||
               methodName.startsWith("create") ||
               methodName.startsWith("update") ||
               methodName.startsWith("modify") ||
               methodName.startsWith("edit") ||
               methodName.startsWith("delete") ||
               methodName.startsWith("remove") ||
               methodName.startsWith("drop") ||
               methodName.startsWith("clear") ||
               methodName.startsWith("reset") ||
               methodName.startsWith("set") ||
               methodName.startsWith("put") ||
               methodName.startsWith("merge") ||
               methodName.startsWith("batch") ||
               methodName.startsWith("bulk") ||
               methodName.startsWith("execute") ||
               methodName.startsWith("process") ||
               methodName.startsWith("handle");
    }
    
    /**
     * 判断当前事务是否为只读事务
     */
    private boolean isReadOnlyTransaction() {
        try {
            // 这里可以通过Spring的事务管理器来判断当前事务是否为只读
            // 由于复杂性，这里简化处理，实际项目中可以根据需要实现
            return false;
        } catch (Exception e) {
            logger.debug("检查事务只读属性失败", e);
            return false;
        }
    }
}