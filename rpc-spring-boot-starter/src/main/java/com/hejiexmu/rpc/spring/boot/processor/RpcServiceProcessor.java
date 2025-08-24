package com.hejiexmu.rpc.spring.boot.processor;

import com.hejiexmu.rpc.spring.boot.annotation.RpcService;
import com.rpc.server.RpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * RPC服务注解处理器
 * 负责扫描和注册带有@RpcService注解的服务
 * 
 * @author hejiexmu
 */
@Slf4j
@Component
public class RpcServiceProcessor implements BeanPostProcessor {
    
    private final RpcServer rpcServer;
    
    public RpcServiceProcessor(RpcServer rpcServer) {
        this.rpcServer = rpcServer;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        
        // 检查是否有@RpcService注解
        RpcService rpcService = AnnotationUtils.findAnnotation(beanClass, RpcService.class);
        if (rpcService != null && rpcService.enabled()) {
            registerRpcService(bean, beanClass, rpcService);
        }
        
        return bean;
    }
    
    /**
     * 注册RPC服务
     */
    private void registerRpcService(Object serviceBean, Class<?> beanClass, RpcService rpcService) {
        try {
            // 确定服务接口类型
            Class<?> interfaceClass = determineInterfaceClass(beanClass, rpcService);
            
            if (interfaceClass == null) {
                log.warn("无法确定服务接口类型，跳过注册：{}", beanClass.getName());
                return;
            }
            
            // 注册服务到RPC服务器
            rpcServer.registerService(
                interfaceClass,
                serviceBean,
                rpcService.version(),
                rpcService.group()
            );
            
            log.info("RPC服务注册成功：接口={}, 实现={}, 版本={}, 分组={}, 权重={}",
                    interfaceClass.getName(),
                    beanClass.getName(),
                    rpcService.version(),
                    rpcService.group(),
                    rpcService.weight());
                    
        } catch (Exception e) {
            log.error("注册RPC服务失败：{}", beanClass.getName(), e);
            throw new RuntimeException("注册RPC服务失败", e);
        }
    }
    
    /**
     * 确定服务接口类型
     */
    private Class<?> determineInterfaceClass(Class<?> beanClass, RpcService rpcService) {
        // 1. 如果注解中指定了接口类型，直接使用
        if (rpcService.interfaceClass() != void.class) {
            return rpcService.interfaceClass();
        }
        
        // 2. 获取实现类的所有接口
        Class<?>[] interfaces = beanClass.getInterfaces();
        
        // 3. 如果只有一个接口，直接使用
        if (interfaces.length == 1) {
            return interfaces[0];
        }
        
        // 4. 如果有多个接口，尝试找到非Spring框架的接口
        for (Class<?> interfaceClass : interfaces) {
            String packageName = interfaceClass.getPackage().getName();
            // 排除Spring框架相关的接口
            if (!packageName.startsWith("org.springframework") && 
                !packageName.startsWith("org.aopalliance") &&
                !packageName.startsWith("java.") &&
                !packageName.startsWith("javax.")) {
                return interfaceClass;
            }
        }
        
        // 5. 如果都是框架接口或没有接口，返回null
        log.warn("无法自动确定服务接口类型，请在@RpcService注解中明确指定interfaceClass属性：{}", beanClass.getName());
        return null;
    }
}