/**
 * FileName: SubcribeResultCustomBody
 * Author:   yangqinkuan
 * Date:     2019-12-9 9:39
 * Description:
 */

package com.ggrpc.common.transport.body;

import com.ggrpc.common.exception.remoting.RemotingCommmonCustomException;
import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.rpc.RegisterMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 注册中心向consumer反馈的服务信息
 */
public class SubcribeResultCustomBody implements CommonCustomBody{


    private String serviceName;

    private LoadBalanceStrategy loadBalanceStrategy;

    private List<RegisterMeta> registerMeta = new ArrayList<>();


    @Override
    public void checkFields() throws RemotingCommmonCustomException {

    }

    public List<RegisterMeta> getRegisterMeta() {
        return registerMeta;
    }

    public void setRegisterMeta(List<RegisterMeta> registerMeta) {
        this.registerMeta = registerMeta;
    }

    public LoadBalanceStrategy getLoadBalanceStrategy() {
        return loadBalanceStrategy;
    }

    public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
