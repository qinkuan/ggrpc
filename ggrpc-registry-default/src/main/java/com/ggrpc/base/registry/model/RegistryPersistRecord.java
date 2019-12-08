package com.ggrpc.base.registry.model;

import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.rpc.RegisterMeta.Address;
import com.ggrpc.common.rpc.ServiceReviewState;

import java.util.ArrayList;
import java.util.List;

public class RegistryPersistRecord {


    private String serviceName;

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


    public static class PersistProviderInfo {

        private Address address;

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
