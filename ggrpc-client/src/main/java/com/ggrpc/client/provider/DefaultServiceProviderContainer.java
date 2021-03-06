package com.ggrpc.client.provider;

import com.ggrpc.client.provider.model.ServiceWrapper;
import com.ggrpc.common.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 服务容器
 */
public class DefaultServiceProviderContainer implements ServiceProviderContainer{
    // 服务本地缓存
    private final ConcurrentMap<String, Pair<CurrentServiceState, ServiceWrapper>> serviceProviders = new ConcurrentHashMap<String, Pair<CurrentServiceState, ServiceWrapper>>();

    public void registerService(String uniqueKey, ServiceWrapper serviceWrapper) {
        // 当前服务状态  服务编制类
        Pair<CurrentServiceState, ServiceWrapper> pair = new Pair<CurrentServiceState, ServiceWrapper>();
        pair.setKey(new CurrentServiceState());
        pair.setValue(serviceWrapper);
        serviceProviders.put(uniqueKey, pair);

    }

    public Pair<CurrentServiceState, ServiceWrapper> lookupService(String uniqueKey) {
        return serviceProviders.get(uniqueKey);
    }

    // 从目前管理的服务中，找到所有支持自动降级的服务，为以后计算判断
    @Override
    public List<Pair<String, CurrentServiceState>> getNeedAutoDegradeService() {

        ConcurrentMap<String, Pair<CurrentServiceState, ServiceWrapper>> _serviceProviders = this.serviceProviders;
        List<Pair<String, CurrentServiceState>> list = new ArrayList<Pair<String,CurrentServiceState>>();

        for(String serviceName: _serviceProviders.keySet()){

            Pair<CurrentServiceState, ServiceWrapper> pair = _serviceProviders.get(serviceName);

            //如果已经设置成自动降级的时候
            if(pair != null && pair.getKey().getIsAutoDegrade().get()){

                Pair<String, CurrentServiceState> targetPair = new Pair<String,CurrentServiceState>();
                targetPair.setKey(serviceName);
                targetPair.setValue(pair.getKey());
                list.add(targetPair);
            }

        }
        return list;
    }



    /**
     * 当前服务实例的状态
     */
    public static class CurrentServiceState {


        private AtomicBoolean hasDegrade = new AtomicBoolean(true);    // 是否已经降级
        private AtomicBoolean hasLimitStream = new AtomicBoolean(true); // 是否已经限流
        private AtomicBoolean isAutoDegrade = new AtomicBoolean(false); // 是否已经开始自动降级
        private Integer minSuccecssRate = 90; 							// 服务最低的成功率，调用成功率低于多少开始自动降级

        public AtomicBoolean getHasDegrade() {
            return hasDegrade;
        }

        public void setHasDegrade(AtomicBoolean hasDegrade) {
            this.hasDegrade = hasDegrade;
        }

        public AtomicBoolean getHasLimitStream() {
            return hasLimitStream;
        }

        public void setHasLimitStream(AtomicBoolean hasLimitStream) {
            this.hasLimitStream = hasLimitStream;
        }

        public AtomicBoolean getIsAutoDegrade() {
            return isAutoDegrade;
        }

        public void setIsAutoDegrade(AtomicBoolean isAutoDegrade) {
            this.isAutoDegrade = isAutoDegrade;
        }

        public Integer getMinSuccecssRate() {
            return minSuccecssRate;
        }

        public void setMinSuccecssRate(Integer minSuccecssRate) {
            this.minSuccecssRate = minSuccecssRate;
        }

    }

}
