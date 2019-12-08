package com.ggrpc.base.registry;

import com.ggrpc.base.registry.model.RegistryPersistRecord;
import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.rpc.RegisterMeta;
import com.ggrpc.common.rpc.RegisterMeta.Address;
import com.ggrpc.common.transport.body.ManagerServiceCustomBody;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ConcurrentSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;

public class RegistryProviderManager implements RegistryProviderServer{
    private static final AttributeKey<ConcurrentSet<String>> S_SUBSCRIBE_KEY = AttributeKey.valueOf("server.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> S_PUBLISH_KEY = AttributeKey.valueOf("server.published");

    private DefaultRegistryServer defaultRegistryServer;

    // 某个服务
    private final ConcurrentMap<String, ConcurrentMap<Address, RegisterMeta>> globalRegisterInfoMap = new ConcurrentHashMap<String, ConcurrentMap<Address, RegisterMeta>>();
    // 指定节点都注册了哪些服务
    private final ConcurrentMap<Address, ConcurrentSet<String>> globalServiceMetaMap = new ConcurrentHashMap<RegisterMeta.Address, ConcurrentSet<String>>();
    // 某个服务 订阅它的消费者的channel集合
    private final ConcurrentMap<String, ConcurrentSet<Channel>> globalConsumerMetaMap = new ConcurrentHashMap<String, ConcurrentSet<Channel>>();
    // 提供者某个地址对应的channel
    private final ConcurrentMap<Address, Channel> globalProviderChannelMetaMap = new ConcurrentHashMap<RegisterMeta.Address, Channel>();
    //每个服务的历史记录
    private final ConcurrentMap<String, RegistryPersistRecord> historyRecords = new ConcurrentHashMap<String, RegistryPersistRecord>();
    //每个服务对应的负载策略
    private final ConcurrentMap<String, LoadBalanceStrategy> globalServiceLoadBalance = new ConcurrentHashMap<String, LoadBalanceStrategy>();

    public RegistryProviderManager(DefaultRegistryServer defaultRegistryServer) {
        this.defaultRegistryServer = defaultRegistryServer;
    }

    public RemotingTransporter handleManager(RemotingTransporter request,Channel channel) throws RemotingSendRequestException, RemotingTimeoutException,InterruptedException{
        ManagerServiceCustomBody managerServiceCustomBody = serializerImpl().readObject(request.bytes(), ManagerServiceCustomBody.class);
        switch (managerServiceCustomBody.getManagerServiceRequestType()) {
            case REVIEW:
                return handleReview(managerServiceCustomBody.getSerivceName(), managerServiceCustomBody.getAddress(), request.getOpaque(),
                        managerServiceCustomBody.getServiceReviewState());
            case DEGRADE:
                return handleDegradeService(request, channel);
            case MODIFY_WEIGHT:
                return handleModifyWeight(request.getOpaque(), managerServiceCustomBody);
            case MODIFY_LOADBALANCE:
                return handleModifyLoadBalance(request.getOpaque(), managerServiceCustomBody);
            case METRICS:
                return handleMetricsService(managerServiceCustomBody.getSerivceName(), request.getOpaque());
            default:
                break;
        }
        return null;
    }

    @Override
    public RemotingTransporter handlerRegister(RemotingTransporter remotingTransporter, Channel channel) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {

        return null;
    }
}
