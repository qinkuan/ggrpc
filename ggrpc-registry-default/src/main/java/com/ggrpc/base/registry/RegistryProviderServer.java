package com.ggrpc.base.registry;

import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;

/**
 * @description 注册中心处理provider的服务接口
 */
public interface RegistryProviderServer {


    /**
     * 处理provider发送过来的注册信息
     * @param remotingTransporter 里面的CommonCustomBody 是#PublishServiceCustomBody
     * @param channel
     * @return
     * @throws InterruptedException
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     */
    RemotingTransporter handlerRegister(RemotingTransporter remotingTransporter, Channel channel) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException;

}
