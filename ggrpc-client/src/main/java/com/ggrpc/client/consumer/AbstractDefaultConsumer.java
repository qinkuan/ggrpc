/**
 * FileName: AbstractDefaultConsumer
 * Author:   yangqinkuan
 * Date:     2019-12-9 12:09
 * Description:
 */

package com.ggrpc.client.consumer;

import com.ggrpc.client.loadbalance.LoadBalanceStrategies;
import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.utils.ChannelGroup;
import com.ggrpc.common.utils.UnresolvedAddress;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消费端的抽象类，这个类的意义
 * 保存从注册中心获取到的每个服务的提供者信息
 * 保存每一个服务的负载均衡的策略
 */
public abstract class AbstractDefaultConsumer implements Consumer{
    /******key是服务名，Value该服务对应的提供的channel的信息集合************/
    private volatile static ConcurrentMap<String, CopyOnWriteArrayList<ChannelGroup>> groups = new ConcurrentHashMap<>();
    /***********某个服务提供者的地址对应的channelGroup*************/
    protected final ConcurrentMap<UnresolvedAddress, ChannelGroup> addressGroups = new ConcurrentHashMap<>();
    /*********************某个服务对应的负载均衡的策略***************/
    protected final ConcurrentHashMap<String, LoadBalanceStrategy> loadConcurrentHashMap = new ConcurrentHashMap<>();

    /**
     * 为某个服务增加一个ChannelGroup
     * @param serviceName
     * @param group
     */
    public static boolean addIfAbsent(String serviceName, ChannelGroup group) {
        String _serviceName = serviceName;
        CopyOnWriteArrayList<ChannelGroup> groupList = groups.get(_serviceName);
        if (groupList == null) {
            CopyOnWriteArrayList<ChannelGroup> newGroupList = new CopyOnWriteArrayList<ChannelGroup>();
            groupList = groups.putIfAbsent(_serviceName, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }
        return groupList.addIfAbsent(group);
    }

    /**
     * 当某个group 失效或者下线的时候，将其冲value中移除
     * @param serviceName
     * @param group
     */
    public static boolean removedIfAbsent(String serviceName, ChannelGroup group) {
        String _serviceName = serviceName;
        CopyOnWriteArrayList<ChannelGroup> groupList = groups.get(_serviceName);
        if (groupList == null) {
            return false;
        }
        return groupList.remove(group);
    }


    public static CopyOnWriteArrayList<ChannelGroup> getChannelGroupByServiceName(String service) {
        return groups.get(service);
    }

    @Override
    public void setServiceLoadBalanceStrategy(String serviceName, LoadBalanceStrategy loadBalanceStrategy) {
        LoadBalanceStrategy balanceStrategy = loadConcurrentHashMap.get(serviceName);
        if(null == balanceStrategy){
            balanceStrategy = LoadBalanceStrategy.WEIGHTINGRANDOM;
            loadConcurrentHashMap.put(serviceName, balanceStrategy);
        }
        balanceStrategy = loadBalanceStrategy;
    }


    public static ConcurrentMap<String, CopyOnWriteArrayList<ChannelGroup>> getGroups() {
        return groups;
    }

    @Override
    public ChannelGroup loadBalance(String serviceName,LoadBalanceStrategy directBalanceStrategy) {
        LoadBalanceStrategy balanceStrategy = loadConcurrentHashMap.get(serviceName);
        // 获取到已有连接channel中服务对应channellist
        CopyOnWriteArrayList<ChannelGroup> list = groups.get(serviceName);
        if(balanceStrategy == null){

            if(directBalanceStrategy == null){
                balanceStrategy = LoadBalanceStrategy.WEIGHTINGRANDOM;
            }else{
                balanceStrategy = directBalanceStrategy;
            }
        }

        if(null == list || list.size() == 0){
            return null;
        }
        switch (balanceStrategy) {

            case RANDOM:
                return LoadBalanceStrategies.RANDOMSTRATEGIES.select(list);
            case WEIGHTINGRANDOM:
                return LoadBalanceStrategies.WEIGHTRANDOMSTRATEGIES.select(list);
            case ROUNDROBIN:
                return LoadBalanceStrategies.ROUNDROBIN.select(list);
            default:
                break;
        }
        return null;
    }


}
