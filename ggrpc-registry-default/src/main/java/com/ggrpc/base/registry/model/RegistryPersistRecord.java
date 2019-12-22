package com.ggrpc.base.registry.model;

import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.rpc.RegisterMeta.Address;
import com.ggrpc.common.rpc.ServiceReviewState;

import java.util.ArrayList;
import java.util.List;
// 注册持久化记录
public class RegistryPersistRecord {

    // 服务名称
    private String serviceName;
    // 负载均衡策略
    private LoadBalanceStrategy balanceStrategy;

    private List<PersistProviderInfo> providerInfos = new ArrayList<PersistProviderInfo>();

    public RegistryPersistRecord() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LoadBalanceStrategy getBalanceStrategy() {
        return balanceStrategy;
    }

    public void setBalanceStrategy(LoadBalanceStrategy balanceStrategy) {
        this.balanceStrategy = balanceStrategy;
    }

    public List<PersistProviderInfo> getProviderInfos() {
        return providerInfos;
    }

    public void setProviderInfos(List<PersistProviderInfo> providerInfos) {
        this.providerInfos = providerInfos;
    }

    // 持久化服务提供者信息
    public static class PersistProviderInfo {
        // 地址信息
        private Address address;
        // 是否通过审核
        private ServiceReviewState isReviewed = ServiceReviewState.PASS_REVIEW;

        public PersistProviderInfo() {
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public ServiceReviewState getIsReviewed() {
            return isReviewed;
        }

        public void setIsReviewed(ServiceReviewState isReviewed) {
            this.isReviewed = isReviewed;
        }


    }
}
