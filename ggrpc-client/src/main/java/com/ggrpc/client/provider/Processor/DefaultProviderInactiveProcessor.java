package com.ggrpc.client.provider.Processor;

import com.ggrpc.client.provider.DefaultProvider;
import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.remoting.model.NettyChannelInactiveProcessor;
import io.netty.channel.ChannelHandlerContext;

/**
 *
 * @description provider的netty inactive触发的事件
 * @time
 */
public class DefaultProviderInactiveProcessor implements NettyChannelInactiveProcessor {
    private DefaultProvider defaultProvider;

    public DefaultProviderInactiveProcessor(DefaultProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }
    @Override
    public void processChannelInactive(ChannelHandlerContext ctx) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        defaultProvider.setProviderStateIsHealthy(false);
    }
}
