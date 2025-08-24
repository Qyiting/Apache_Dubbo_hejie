package com.rpc.client.loadbalance.random;

import com.rpc.client.loadbalance.LoadBalancer;
import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author 何杰
 * @version 1.0
 */
@Slf4j
public class RandomLoadBalancer implements LoadBalancer {
    /** 随机数生成器 */
    private final Random random = new Random();
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
        // 随机选择一个实例
        int index = random.nextInt(activeServices.size());
        ServiceInfo selected = activeServices.get(index);
        log.debug("随机负载均衡选择服务实例：{} (索引：{}/{})", selected.getFullAddress(), index,
                activeServices.size());
        return selected;
    }

    @Override
    public String getAlgorithm() {
        return Algorithm.RANDOM;
    }

    @Override
    public String toString() {
        return "RandomLoadBalancer{algorithm=" + getAlgorithm() + "}";
    }
}
