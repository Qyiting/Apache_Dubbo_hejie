package com.hejiexmu.rpc.auth.annotation;

import com.hejiexmu.rpc.auth.config.DataSourceConfig;

import java.lang.annotation.*;


/**
 * 数据源注解
 * 用于标记方法使用主库还是从库
 * 
 * @author hejiexmu
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    
    /**
     * 数据源类型
     * 默认使用主数据源
     */
    DataSourceConfig.DataSourceType value() default DataSourceConfig.DataSourceType.MASTER;
}