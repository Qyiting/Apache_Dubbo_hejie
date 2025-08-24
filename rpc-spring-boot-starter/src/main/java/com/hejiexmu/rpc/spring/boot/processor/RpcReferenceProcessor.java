package com.hejiexmu.rpc.spring.boot.processor;

import com.hejiexmu.rpc.spring.boot.annotation.RpcReference;
import com.hejiexmu.rpc.spring.boot.properties.RpcProperties;
import com.rpc.client.RpcClient;
import com.rpc.client.factory.RpcProxyFactory;
import com.rpc.serialization.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RPC引用注解处理器
 * 负责扫描和注入带有@RpcReference注解的字段
 * 
 * @author hejiexmu
 */
@Slf4j
public class RpcReferenceProcessor implements BeanPostProcessor {
    
    private final RpcClient rpcClient;
    private final RpcProperties rpcProperties;
    private final RpcProxyFactory rpcProxyFactory;
    
    // 缓存已创建的代理对象，避免重复创建
    private final ConcurrentMap<String, Object> proxyCache = new ConcurrentHashMap<>();
    
    public RpcReferenceProcessor(RpcClient rpcClient, RpcProperties rpcProperties) {
        this.rpcClient = rpcClient;
        this.rpcProperties = rpcProperties;
        this.rpcProxyFactory = new RpcProxyFactory(rpcClient);
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 扫描所有字段，查找@RpcReference注解
        ReflectionUtils.doWithFields(beanClass, field -> {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                injectRpcReference(bean, field, rpcReference);
            }
        });
        
        return bean;
    }
    
    /**
     * 注入RPC引用
     */
    private void injectRpcReference(Object bean, Field field, RpcReference rpcReference) {
        try {
            // 确定服务接口类型
            Class<?> interfaceClass = determineInterfaceClass(field, rpcReference);
            
            if (interfaceClass == null) {
                log.warn("无法确定服务接口类型，跳过注入：{}.{}", bean.getClass().getName(), field.getName());
                return;
            }
            
            // 构建缓存键
            String cacheKey = buildCacheKey(interfaceClass, rpcReference);
            
            // 从缓存获取或创建代理对象
            Object proxy = proxyCache.computeIfAbsent(cacheKey, key -> createProxy(interfaceClass, rpcReference));
            
            // 注入代理对象
            field.setAccessible(true);
            ReflectionUtils.setField(field, bean, proxy);
            
            log.info("RPC引用注入成功：字段={}.{}, 接口={}, 版本={}, 分组={}",
                    bean.getClass().getName(),
                    field.getName(),
                    interfaceClass.getName(),
                    rpcReference.version(),
                    rpcReference.group());
                    
        } catch (Exception e) {
            log.error("注入RPC引用失败：{}.{}", bean.getClass().getName(), field.getName(), e);
            throw new RuntimeException("注入RPC引用失败", e);
        }
    }
    
    /**
     * 确定服务接口类型
     */
    private Class<?> determineInterfaceClass(Field field, RpcReference rpcReference) {
        // 1. 如果注解中指定了接口类型，直接使用
        if (rpcReference.interfaceClass() != void.class) {
            return rpcReference.interfaceClass();
        }
        
        // 2. 使用字段类型
        Class<?> fieldType = field.getType();
        
        // 3. 检查字段类型是否为接口
        if (fieldType.isInterface()) {
            return fieldType;
        }
        
        // 4. 如果字段类型不是接口，尝试获取其实现的接口
        Class<?>[] interfaces = fieldType.getInterfaces();
        if (interfaces.length == 1) {
            return interfaces[0];
        }
        
        // 5. 如果有多个接口，尝试找到非框架接口
        for (Class<?> interfaceClass : interfaces) {
            String packageName = interfaceClass.getPackage().getName();
            if (!packageName.startsWith("org.springframework") && 
                !packageName.startsWith("java.") &&
                !packageName.startsWith("javax.")) {
                return interfaceClass;
            }
        }
        
        log.warn("无法自动确定服务接口类型，请在@RpcReference注解中明确指定interfaceClass属性：{}", field.getName());
        return null;
    }
    
    /**
     * 创建RPC代理对象
     */
    private Object createProxy(Class<?> interfaceClass, RpcReference rpcReference) {
        // 获取配置参数
        String version = StringUtils.hasText(rpcReference.version()) ? 
                rpcReference.version() : "1.0.0";
        String group = StringUtils.hasText(rpcReference.group()) ? 
                rpcReference.group() : "default";
        long timeout = rpcReference.timeout() > 0 ? 
                rpcReference.timeout() : rpcProperties.getConsumer().getTimeout();
        
        // 获取序列化类型
        byte serializationType = rpcProperties.getProvider().getSerializer();
        
        // 设置代理工厂参数
        rpcProxyFactory.setVersion(version);
        rpcProxyFactory.setGroup(group);
        rpcProxyFactory.setTimeout(timeout);
        
        // 根据是否异步创建不同类型的代理
        if (rpcReference.async()) {
            return rpcProxyFactory.createAsyncProxy(interfaceClass, version, group, timeout, serializationType);
        } else {
            return rpcProxyFactory.createProxy(interfaceClass, version, group, timeout, serializationType);
        }
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(Class<?> interfaceClass, RpcReference rpcReference) {
        return String.format("%s:%s:%s:%d:%b",
                interfaceClass.getName(),
                rpcReference.version(),
                rpcReference.group(),
                rpcReference.timeout(),
                rpcReference.async());
    }
}