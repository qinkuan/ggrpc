/**
 * FileName: DefaultConsumer
 * Author:   yangqinkuan
 * Date:     2019-12-9 12:07
 * Description:
 */

package com.ggrpc.client.consumer;

import com.ggrpc.common.protocal.GGprotocol;
import com.ggrpc.common.rpc.RegisterMeta;
import com.ggrpc.common.utils.ChannelGroup;
import com.ggrpc.common.utils.JUnsafe;
import com.ggrpc.common.utils.NettyChannelGroup;
import com.ggrpc.common.utils.UnresolvedAddress;
import com.ggrpc.remoting.ConnectionUtils;
import com.ggrpc.remoting.netty.NettyClientConfig;
import com.ggrpc.remoting.netty.NettyRemotingClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 默认的消费类
 */
public abstract class DefaultConsumer extends AbstractDefaultConsumer{
    private static final Logger logger = LoggerFactory.getLogger(DefaultConsumer.class);
    // 注册中心的配置
    private NettyClientConfig registryClientConfig;
    // 直连默认provider的配置
    private NettyClientConfig providerClientConfig;
    // 自己作为消费者的配置
    private ConsumerConfig consumerConfig;
    // 与注册中心连接的netty客户端
    protected NettyRemotingClient registryNettyRemotingClient;
    // 与provider连接的客户端
    protected NettyRemotingClient providerNettyRemotingClient;
    // 消费者注册处理功能
    private DefaultConsumerRegistry defaultConsumerRegistry;
    // 消费者管理
    private ConsumerManager consumerManager;
    private Channel registyChannel;
    // 构建消费者
    public DefaultConsumer(NettyClientConfig registryClientConfig, NettyClientConfig providerClientConfig, ConsumerConfig consumerConfig) {
        this.registryClientConfig = registryClientConfig;
        this.providerClientConfig = providerClientConfig;
        this.consumerConfig = consumerConfig;
        defaultConsumerRegistry = new DefaultConsumerRegistry(this);
        consumerManager = new ConsumerManager(this);
        initialize();
    }
    // 消费者端初始化，包括客户端配置，处理器注册
    private void initialize() {

        //因为服务消费端可以直连provider，所以当传递过来的与注册中心连接的配置文件为空的时候，可以不初始化registryNettyRemotingClient
        if(null != this.registryClientConfig){
            this.registryNettyRemotingClient = new NettyRemotingClient(this.registryClientConfig);
            // 注册处理器
            this.registerProcessor();
        }
        // 作为连接provider的客户端
        this.providerNettyRemotingClient = new NettyRemotingClient(this.providerClientConfig);

    }

    private void registerProcessor() {
        // 订阅结果处理器
        this.registryNettyRemotingClient.registerProcessor(GGprotocol.SUBCRIBE_RESULT, new DefaultConsumerRegistryProcessor(this), null);
        // 订阅取消处理器
        this.registryNettyRemotingClient.registerProcessor(GGprotocol.SUBCRIBE_SERVICE_CANCEL, new DefaultConsumerRegistryProcessor(this), null);
        // 修改负载均衡策略处理器
        this.registryNettyRemotingClient.registerProcessor(GGprotocol.CHANGE_LOADBALANCE, new DefaultConsumerRegistryProcessor(this), null);
    }

    // 订阅服务
    @Override
    public SubscribeManager subscribeService(final String service) {

        SubscribeManager manager = new SubscribeManager() {

            private final ReentrantLock lock = new ReentrantLock();
            // 其他线程进行调用服务时，会等待服务可用，3秒钟
            private final Condition notifyCondition = lock.newCondition();
            private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

            @Override
            public void start() {
                subcribeService(service, new NotifyListener() {
                    // 订阅服务成功后，监听器，进行连接
                    @Override
                    public void notify(RegisterMeta registerMeta, NotifyEvent event) {

                        // host
                        String remoteHost = registerMeta.getAddress().getHost();
                        // port vip服务 port端口号-2
                        int remotePort = registerMeta.isVIPService() ? (registerMeta.getAddress().getPort() - 2) : registerMeta.getAddress().getPort();

                        final ChannelGroup group = group(new UnresolvedAddress(remoteHost, remotePort));
                        if (event == NotifyEvent.CHILD_ADDED) {
                            // 链路复用，如果此host和port对应的链接的channelGroup是已经存在的，则无需建立新的链接，只需要将此group与service建立关系即可
                            if (!group.isAvailable()) {

                                int connCount = registerMeta.getConnCount() < 0 ? 1 : registerMeta.getConnCount();

                                group.setWeight(registerMeta.getWeight());

                                // 消费端对服务端进行连接，并发channel保存
                                for (int i = 0; i < connCount; i++) {

                                    try {
                                        // 所有的consumer与provider之间的链接不进行短线重连操作
                                        DefaultConsumer.this.getProviderNettyRemotingClient().setreconnect(false);
                                        // Consumer订阅成功后连接Provider
                                        DefaultConsumer.this.getProviderNettyRemotingClient().getBootstrap()
                                                .connect(ConnectionUtils.string2SocketAddress(remoteHost + ":" + remotePort)).addListener(new ChannelFutureListener() {
                                            // 完成监听器
                                            @Override
                                            public void operationComplete(ChannelFuture future) throws Exception {
                                                group.add(future.channel());
                                                onSucceed(signalNeeded.getAndSet(true));
                                            }

                                        });
                                    } catch (Exception e) {
                                        logger.error("connection provider host [{}] and port [{}] occor exception [{}]", remoteHost, remotePort, e.getMessage());
                                    }
                                }
                            }else{
                                onSucceed(signalNeeded.getAndSet(true));
                            }
                            addChannelGroup(service,group);
                        }else if(event == NotifyEvent.CHILD_REMOVED){
                            removedIfAbsent(service, group);
                        }
                    }
                });
            }

            @Override
            public boolean waitForAvailable(long timeoutMillis) {
                if (isServiceAvailable(service)) {
                    return true;
                }
                boolean available = false;
                long start = System.nanoTime();
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    while (!isServiceAvailable(service)) {
                        signalNeeded.set(true);
                        notifyCondition.await(timeoutMillis, MILLISECONDS);

                        available = isServiceAvailable(service);
                        if (available || (System.nanoTime() - start) > MILLISECONDS.toNanos(timeoutMillis)) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    JUnsafe.throwException(e);
                } finally {
                    _look.unlock();
                }
                return available;
            }
            // 判断服务是否可用
            private boolean isServiceAvailable(String service) {
                CopyOnWriteArrayList<ChannelGroup> list = DefaultConsumer.super.getChannelGroupByServiceName(service);
                if(list == null){
                    return false;
                }else{
                    for(ChannelGroup channelGroup : list){
                        if(channelGroup.isAvailable()){
                            return true;
                        }
                    }
                }
                return false;
            }
            // onsucceed
            private void onSucceed(boolean doSignal) {
                if (doSignal) {
                    final ReentrantLock _look = lock;
                    _look.lock();
                    try {
                        notifyCondition.signalAll();
                    } finally {
                        _look.unlock();
                    }
                }
            }

        };
        // 开始订阅服务
        manager.start();
        return manager;
    }

    @Override
    public void subcribeService(String subcribeServices, NotifyListener listener) {
        if (subcribeServices != null) {
            this.defaultConsumerRegistry.subcribeService(subcribeServices,listener);
        }
    }

    @Override
    public boolean addChannelGroup(String serviceName, ChannelGroup group) {
        return DefaultConsumer.super.addIfAbsent(serviceName, group);
    }

    @Override
    public boolean removeChannelGroup(String serviceName, ChannelGroup group) {
        return DefaultConsumer.super.removedIfAbsent(serviceName, group);
    }

    @Override
    public Channel directGetProviderByChannel(UnresolvedAddress address) throws InterruptedException {
        return this.providerNettyRemotingClient.getAndCreateChannel(address.getHost()+":"+address.getPort());
    }


    // 启动客户端
    @Override
    public void start() {

        logger.info("######### consumer start.... #########");
        // 如果连接注册中心的client初始化成功的情况下，且连接注册中心的地址不为空的时候去尝试连接注册中心
        if (null != this.registryClientConfig && null != this.registryNettyRemotingClient) {
            this.registryNettyRemotingClient.start();
            // 获取到与注册中心集群的一个健康的的Netty 长连接的channel
            getOrUpdateHealthyChannel();
        }

        this.providerNettyRemotingClient.setreconnect(false);
        this.providerNettyRemotingClient.start();

    }

    @Override
    public void getOrUpdateHealthyChannel() {

        //获取到注册中心的地址
        String addresses = this.registryClientConfig.getDefaultAddress();

        if (registyChannel != null && registyChannel.isActive() && registyChannel.isWritable())
            return;

        if (addresses == null || "".equals(addresses)) {
            logger.error("registry address is empty");
            return;
        }

        //与注册中心连接的时候重试次数
        int retryConnectionTimes = this.consumerConfig.getRetryConnectionRegistryTimes();
        //连接给每次注册中心的时候最大的超时时间
        long maxTimeout = this.consumerConfig.getMaxRetryConnectionRegsitryTime();

        String[] adds = addresses.split(",");

        for (int i = 0; i < adds.length; i++) {

            if (registyChannel != null && registyChannel.isActive() && registyChannel.isWritable())
                return;

            String currentAddress = adds[i];
            //开始计时
            final long beginTimestamp = System.currentTimeMillis();
            long endTimestamp = beginTimestamp;

            int times = 0;

            //当重试次数小于最大次数且每个实例重试的时间小于最大的时间的时候，不断重试
            for (; times < retryConnectionTimes && (endTimestamp - beginTimestamp) < maxTimeout; times++) {
                try {
                    Channel channel = registryNettyRemotingClient.createChannel(currentAddress);
                    if (channel != null && channel.isActive() && channel.isWritable()) {
                        this.registyChannel = channel;
                        break;
                    } else {
                        continue;
                    }
                } catch (InterruptedException e) {
                    logger.warn("connection registry center [{}] fail", currentAddress);
                    endTimestamp = System.currentTimeMillis();
                    continue;
                }
            }
        }
    }

    public NettyRemotingClient getRegistryNettyRemotingClient() {
        return registryNettyRemotingClient;
    }

    public void setRegistryNettyRemotingClient(NettyRemotingClient registryNettyRemotingClient) {
        this.registryNettyRemotingClient = registryNettyRemotingClient;
    }

    public Channel getRegistyChannel() {
        return registyChannel;
    }

    public void setRegistyChannel(Channel registyChannel) {
        this.registyChannel = registyChannel;
    }

    public ConsumerConfig getConsumerConfig() {
        return consumerConfig;
    }

    public void setConsumerConfig(ConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public NettyClientConfig getRegistryClientConfig() {
        return registryClientConfig;
    }

    public void setRegistryClientConfig(NettyClientConfig registryClientConfig) {
        this.registryClientConfig = registryClientConfig;
    }

    public ConsumerManager getConsumerManager() {
        return consumerManager;
    }

    public void setConsumerManager(ConsumerManager consumerManager) {
        this.consumerManager = consumerManager;
    }

    public NettyRemotingClient getProviderNettyRemotingClient() {
        return providerNettyRemotingClient;
    }

    public void setProviderNettyRemotingClient(NettyRemotingClient providerNettyRemotingClient) {
        this.providerNettyRemotingClient = providerNettyRemotingClient;
    }

    public DefaultConsumerRegistry getDefaultConsumerRegistry() {
        return defaultConsumerRegistry;
    }

    public void setDefaultConsumerRegistry(DefaultConsumerRegistry defaultConsumerRegistry) {
        this.defaultConsumerRegistry = defaultConsumerRegistry;
    }
    // 获取某个服务提供者机器provider对应的Channel组
    public ChannelGroup group(UnresolvedAddress address) {

        ChannelGroup group = addressGroups.get(address);
        if (group == null) {
            ChannelGroup newGroup = newChannelGroup(address);
            group = addressGroups.putIfAbsent(address, newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    private ChannelGroup newChannelGroup(UnresolvedAddress address) {
        return new NettyChannelGroup(address);
    }

}
