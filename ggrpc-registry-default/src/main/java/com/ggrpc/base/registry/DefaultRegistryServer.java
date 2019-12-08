package com.ggrpc.base.registry;

import com.ggrpc.remoting.netty.NettyRemotingServer;
import com.ggrpc.remoting.netty.NettyServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 *
 * #######注册中心######
 *
 * 可以有多个注册中心，所有的注册中心之前不进行通讯，都是无状态的
 * 1)provider端与每一个注册中心之间保持长连接，保持重连
 * 2)consumer随机选择一个注册中心保持长连接，如果断了，不去主动重连，选择其他可用的注册中心
 *
 * @description 默认的注册中心，处理注册端的所有事宜：
 * 1)处理consumer端发送过来的注册信息
 * 2)处理provider端发送过来的订阅信息
 * 3)当服务下线需要通知对应的consumer变更后的注册信息
 * 4)所有的注册订阅信息的储存和健康检查
 * 5)接收管理者的一些信息请求，比如 服务统计 | 某个实例的服务降级 | 通知消费者的访问策略  | 改变某个服务实例的比重
 * 6)将管理者对服务的一些信息 例如审核结果，负载算法等信息持久化到硬盘
 *
 */
public class DefaultRegistryServer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistryServer.class);

    private final NettyServerConfig nettyServerConfig;       //netty Server的一些配置文件
    private RegistryServerConfig registryServerConfig;       //注册中心的配置文件
    private NettyRemotingServer remotingServer;  	         //注册中心的netty server端
    private RegistryConsumerManager consumerManager;         //注册中心消费侧的管理逻辑控制类
    private RegistryProviderManager providerManager;         //注册中心服务提供者的管理逻辑控制类
    private ExecutorService remotingExecutor;                //执行器
    private ExecutorService remotingChannelInactiveExecutor; //channel inactive的线程执行器

}
