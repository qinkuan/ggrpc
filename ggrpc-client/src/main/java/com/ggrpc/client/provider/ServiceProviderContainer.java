package com.ggrpc.client.provider;

import com.ggrpc.client.provider.model.ServiceWrapper;
import com.ggrpc.client.provider.DefaultServiceProviderContainer.CurrentServiceState;
import com.ggrpc.common.utils.Pair;

import java.util.List;

public interface ServiceProviderContainer {

    /**
     * 将服务放置在服务容器中，用来进行统一的管理
     * @param serviceName 该服务的名称
     * @param serviceWrapper 该服务的包装编织类
     */
    void registerService(String serviceName, ServiceWrapper serviceWrapper);

    /**
     * 根据服务的名称来获取对应的服务编织类
     * @param serviceName 服务名
     * @return 服务编织类
     */
    Pair<CurrentServiceState, ServiceWrapper> lookupService(String serviceName);



    /**
     * 获取到所有需要自动降级的服务
     * @return
     */
    List<Pair<String, DefaultServiceProviderContainer.CurrentServiceState>> getNeedAutoDegradeService();
}
