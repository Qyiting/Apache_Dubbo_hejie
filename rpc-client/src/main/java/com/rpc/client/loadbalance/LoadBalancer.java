package com.rpc.client.loadbalance;

import com.rpc.core.request.RpcRequest;
import com.rpc.core.serviceinfo.ServiceInfo;

import java.util.List;

/**
 * @author 何杰
 * @version 1.0
 */
public interface LoadBalancer {
    /**
     * 从服务实例列表中选择一个服务实例
     *
     * @param serviceInfos 服务实例列表
     * @param request RPC请求（可用于基于请求内容的负载均衡）
     * @return 选中的服务实例，如果没有可用实例则返回null
     */
    ServiceInfo select(List<ServiceInfo> serviceInfos, RpcRequest request);

    /**
     * 获取负载均衡算法名称
     *
     * @return 算法名称
     */
    String getAlgorithm();
    
    /**
     * 更新服务列表（可选实现）
     * 当服务列表发生变化时，负载均衡器可以进行相应的优化处理
     *
     * @param serviceInfos 新的服务实例列表
     */
    default void updateServiceList(List<ServiceInfo> serviceInfos) {
        // 默认空实现，子类可以根据需要重写
    }

    /**
     * 负载均衡算法类型常量
     */
    interface Algorithm {
        /** 随机算法 */
        String RANDOM = "random";

        /** 轮询算法 */
        String ROUND_ROBIN = "round_robin";

        /** 加权轮询算法 */
        String WEIGHTED_ROUND_ROBIN = "weighted_round_robin";

        /** 最少活跃调用算法 */
        String LEAST_ACTIVE = "least_active";

        /** 一致性哈希算法 */
        String CONSISTENT_HASH = "consistent_hash";

        /** 最短响应时间算法 */
        String SHORTEST_RESPONSE = "shortest_response";

        /** LRU算法 */
        String LRU = "lru";

        /** LFU 算法 */
        String LFU = "lfu";
    }
}
