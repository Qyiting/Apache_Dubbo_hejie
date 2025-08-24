package com.rpc.client.loadbalance.robin;

import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RoundRobinLoadBalancer implements LoadBalancer {

    /** 轮询计数器 */
    private final AtomicInteger counter = new AtomicInteger(0);
    @Override
    public ServiceInfo select(List<ServiceInfo> serviceInfos, RpcRequest request) {
        if(serviceInfos == null || serviceInfos.isEmpty()) {
            log.warn("服务实例列表为空，无法进行负载均衡选择");
            return null;
        }
        // 过滤出活跃的服务实例
        List<ServiceInfo> activeServices = serviceInfos.stream().filter(ServiceInfo::isActive).collect(Collectors.toList());
        if(activeServices.isEmpty()) {
            log.warn("没有活跃的服务实例可用于负载均衡选择");
            return null;
        }
        // 如果只有一个实例，直接返回
        if(activeServices.size() == 1) {
            ServiceInfo selected = activeServices.get(0);
            log.debug("只有一个活跃服务实例，直接选择：{}", selected.getFullAddress());
            return selected;
        }
        // 轮询选择实例
        int index = getNextInt(activeServices.size());
        ServiceInfo selected = activeServices.get(index);
        log.debug("轮询负载均衡选择服务实例：{} (索引：{}/{})", selected.getFullAddress(), index,
                activeServices.size());
        return selected;
    }

    /**
     * 获取下一个索引
     * 使用原子操作确保线程安全
     *
     * @param size 服务实例数量
     * @return 下一个索引
     */
    private int getNextInt(int size) {
        if (size <= 0) {
            return 0;
        }
        // 使用原子操作确保线程安全，避免整数溢出
        int current = counter.getAndIncrement();
        // 使用绝对值确保非负，并处理整数溢出的情况
        return Math.abs(current % size);
    }

    @Override
    public String getAlgorithm() {
        return Algorithm.ROUND_ROBIN;
    }

    /**
     * 重置计数器
     * 可用于测试或特殊场景
     */
    public void reset() {
        counter.set(0);
        log.debug("轮询负载均衡器计数器已重置");
    }

    /**
     * 获取当前计数器值
     *
     * @return 当前计数器值
     */
    public int getCurrentCounter() {
        return counter.get();
    }

    @Override
    public String toString() {
        return "RoundRobinLoadBalancer{algorithm=" + getAlgorithm() +
                ", counter=" + counter.get() + "}";
    }
}
