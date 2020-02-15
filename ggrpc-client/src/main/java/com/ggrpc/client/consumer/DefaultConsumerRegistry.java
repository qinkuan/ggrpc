/**
 * FileName: DefaultConsumerRegistry
 * Author:   yangqinkuan
 * Date:     2019-12-19 10:45
 * Description:
 */

package com.ggrpc.client.consumer;

import com.ggrpc.common.protocal.GGprotocol;
import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.common.transport.body.SubscribeRequestCustomBody;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 消费者的注册处理功能
 */
public class DefaultConsumerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConsumerRegistry.class);
    private DefaultConsumer defaultConsumer;
    private ConcurrentHashMap<String, NotifyListener> serviceMatchedNotifyListener = new ConcurrentHashMap<>();

    private long timeout;

    public DefaultConsumerRegistry(DefaultConsumer defaultConsumer) {
        this.defaultConsumer = defaultConsumer;
        this.timeout = this.defaultConsumer.getConsumerConfig().getRegistryTimeout();
    }
    // 为服务添加listener，不同节点上下线
    public void subcribeService(String serviceName, NotifyListener listener) {

        if(listener != null){
            serviceMatchedNotifyListener.put(serviceName, listener);
        }
        // 更新与注册中心的channel
        if (this.defaultConsumer.getRegistyChannel() == null) {
            this.defaultConsumer.getOrUpdateHealthyChannel();
        }
        // 开始订阅服务
        if (this.defaultConsumer.getRegistyChannel() != null) {

            logger.info("registry center channel is [{}]", this.defaultConsumer.getRegistyChannel());

            SubscribeRequestCustomBody body = new SubscribeRequestCustomBody();
            body.setServiceName(serviceName);

            RemotingTransporter remotingTransporter = RemotingTransporter.createRequestTransporter(GGprotocol.SUBSCRIBE_SERVICE, body);
            try {
                // 向注册中心发送注册消息
                RemotingTransporter registryResponse = sendKernelSubscribeInfo(this.defaultConsumer.getRegistyChannel(), remotingTransporter, timeout);
                // 处理注册响应
                RemotingTransporter ackTransporter = this.defaultConsumer.getConsumerManager().handlerSubcribeResult(registryResponse,
                        this.defaultConsumer.getRegistyChannel());
                // 对注册中心发送过来的服务信息进行响应
                this.defaultConsumer.getRegistyChannel().writeAndFlush(ackTransporter);
            } catch (Exception e) {
                logger.warn("registry failed [{}]", e.getMessage());
            }

        } else {
            logger.warn("sorry can not connection to registry address [{}],please check your registry address", this.defaultConsumer.getRegistryClientConfig()
                    .getDefaultAddress());
        }

    }

    // 通过客户端向注册中心发送消息
    private RemotingTransporter sendKernelSubscribeInfo(Channel registyChannel, RemotingTransporter remotingTransporter, long timeout)
            throws RemotingTimeoutException, RemotingSendRequestException, InterruptedException {
        // 通过客户端向注册中心发送消息
        return this.defaultConsumer.getRegistryNettyRemotingClient().invokeSyncImpl(this.defaultConsumer.getRegistyChannel(), remotingTransporter, timeout);
    }
    public ConcurrentHashMap<String, NotifyListener> getServiceMatchedNotifyListener() {
        return serviceMatchedNotifyListener;
    }



}
