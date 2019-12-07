package com.ggrpc.common.loadbalance;

public enum  LoadBalanceStrategy {
    RANDOM, //随机
    WEIGHTINGRANDOM, //加权随机
    ROUNDROBIN, //轮询
}
