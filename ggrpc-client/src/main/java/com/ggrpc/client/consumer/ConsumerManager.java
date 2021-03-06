/**
 * FileName: ConsumerManager
 * Author:   yangqinkuan
 * Date:     2019-12-19 10:58
 * Description:
 */

package com.ggrpc.client.consumer;

import com.ggrpc.common.protocal.GGprotocol;
import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.rpc.RegisterMeta;
import com.ggrpc.common.transport.body.AckCustomBody;
import com.ggrpc.common.transport.body.SubcribeResultCustomBody;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;

/**
 * 消费端的一些逻辑处理
 */
public class ConsumerManager {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerManager.class);

    private DefaultConsumer defaultConsumer; //consumer模块的代码手持defaultConsumer好办事
    // 读写锁用于更新本地注册列表
    private final ReentrantReadWriteLock registriesLock = new ReentrantReadWriteLock();
    // 本地存储注册信息表
    private final Map<String, List<RegisterMeta>> registries = new ConcurrentHashMap<>();
    public ConsumerManager(DefaultConsumer defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
    }

    /**
     * 处理服务的订阅结果
     * @param subcribeResponse
     * @param channel
     * @return
     */
    public RemotingTransporter handlerSubcribeResult(RemotingTransporter subcribeResponse, Channel channel) {

        if (logger.isDebugEnabled()) {
            logger.debug("handler subcribe result [{}] and channel [{}]", subcribeResponse, channel);
        }
        // 获取到返回结果
        AckCustomBody ackCustomBody = new AckCustomBody(subcribeResponse.getOpaque(), false);
        RemotingTransporter responseTransporter = RemotingTransporter.createResponseTransporter(GGprotocol.ACK, ackCustomBody, subcribeResponse.getOpaque());
        // 序列化成实体类
        SubcribeResultCustomBody subcribeResultCustomBody = serializerImpl().readObject(subcribeResponse.bytes(), SubcribeResultCustomBody.class);

        String serviceName = null;
        if (subcribeResultCustomBody != null && subcribeResultCustomBody.getRegisterMeta() != null && !subcribeResultCustomBody.getRegisterMeta().isEmpty()) {

            for (RegisterMeta registerMeta : subcribeResultCustomBody.getRegisterMeta()) {

                if (null == serviceName) {
                    serviceName = registerMeta.getServiceName();
                }
                // 更新本地注册表(本地已注册信息)
                notify(serviceName, registerMeta, NotifyListener.NotifyEvent.CHILD_ADDED);
            }
        }

        ackCustomBody.setSuccess(true);
        return responseTransporter;
    }
    /**
     * 处理服务取消的时候逻辑处理
     *
     * @param request
     * @param channel
     * @return
     */
    public RemotingTransporter handlerSubscribeResultCancel(RemotingTransporter request, Channel channel) {
        AckCustomBody ackCustomBody = new AckCustomBody(request.getOpaque(), false);
        RemotingTransporter responseTransporter = RemotingTransporter.createResponseTransporter(GGprotocol.ACK, ackCustomBody, request.getOpaque());

        SubcribeResultCustomBody subcribeResultCustomBody = serializerImpl().readObject(request.bytes(), SubcribeResultCustomBody.class);

        if (subcribeResultCustomBody != null && subcribeResultCustomBody.getRegisterMeta() != null && !subcribeResultCustomBody.getRegisterMeta().isEmpty()) {

            for (RegisterMeta registerMeta : subcribeResultCustomBody.getRegisterMeta()) {
                notify(registerMeta.getServiceName(), registerMeta, NotifyListener.NotifyEvent.CHILD_REMOVED);
            }
        }
        ackCustomBody.setSuccess(true);
        return responseTransporter;
    }
    /**
     * 处理注册中心发送过来的负载均衡策略的变化
     * @param request
     * @param channel
     * @return
     */
    public RemotingTransporter handlerServiceLoadBalance(RemotingTransporter request, Channel channel) {

        if(logger.isDebugEnabled()){
            logger.debug("handler change loadBalance strategy [{}] and channel [{}]",request,channel);
        }
        //+++++++++++++++++++++++++++++++++++++++ 这段代码可以被抽象
        AckCustomBody ackCustomBody = new AckCustomBody(request.getOpaque(), false);
        RemotingTransporter responseTransporter = RemotingTransporter.createResponseTransporter(GGprotocol.ACK, ackCustomBody, request.getOpaque());
        SubcribeResultCustomBody subcribeResultCustomBody = serializerImpl().readObject(request.bytes(), SubcribeResultCustomBody.class);
        //++++++++++++++++++++++++++++++++++++++
        String serviceName = subcribeResultCustomBody.getServiceName();

        LoadBalanceStrategy balanceStrategy = subcribeResultCustomBody.getLoadBalanceStrategy();

        defaultConsumer.setServiceLoadBalanceStrategy(serviceName, balanceStrategy);

        ackCustomBody.setSuccess(true);
        return responseTransporter;
    }

    private void notify(String serviceName, RegisterMeta registerMeta, NotifyListener.NotifyEvent event) {
        boolean notifyNeeded = false;

        final Lock writeLock = registriesLock.writeLock();
        writeLock.lock();
        try {
            List<RegisterMeta> registerMetas = registries.get(serviceName);
            if (registerMetas == null) {
                if (event == NotifyListener.NotifyEvent.CHILD_REMOVED) {
                    return;
                }
                registerMetas = new ArrayList<RegisterMeta>();
                registerMetas.add(registerMeta);
                notifyNeeded = true;
            } else {
                if (event == NotifyListener.NotifyEvent.CHILD_REMOVED) {
                    registerMetas.remove(registerMeta);
                } else if (event == NotifyListener.NotifyEvent.CHILD_ADDED) {
                    registerMetas.add(registerMeta);
                }
                notifyNeeded = true;
            }
            registries.put(serviceName, registerMetas);
        } finally {
            writeLock.unlock();
        }
        // 更新远程服务列表
        if (notifyNeeded) {

            NotifyListener listener = this.defaultConsumer.getDefaultConsumerRegistry().getServiceMatchedNotifyListener().get(serviceName);
            if (null != listener) {
                listener.notify(registerMeta, event);
            }

        }
    }
}
