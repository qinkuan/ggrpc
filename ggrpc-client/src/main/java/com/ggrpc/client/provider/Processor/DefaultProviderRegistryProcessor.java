package com.ggrpc.client.provider.Processor;

import com.ggrpc.client.provider.DefaultProvider;
import com.ggrpc.remoting.ConnectionUtils;
import com.ggrpc.remoting.model.NettyRequestProcessor;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ggrpc.common.protocal.GGprotocol.AUTO_DEGRADE_SERVICE;
import static com.ggrpc.common.protocal.GGprotocol.DEGRADE_SERVICE;

/**
 * 降级provider端注册的处理器
 */
public class DefaultProviderRegistryProcessor implements NettyRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProviderRegistryProcessor.class);

    private DefaultProvider defaultProvider;

    public DefaultProviderRegistryProcessor(DefaultProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }


    @Override
    public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter request) throws Exception {

        if (logger.isDebugEnabled()) {
            logger.debug("receive request, {} {} {}",//
                    request.getCode(), //
                    ConnectionUtils.parseChannelRemoteAddr(ctx.channel()), //
                    request);
        }

        switch (request.getCode()) {
            case DEGRADE_SERVICE:
                return this.defaultProvider.handlerDegradeServiceRequest(request,ctx.channel(),DEGRADE_SERVICE);
            case AUTO_DEGRADE_SERVICE:
                return this.defaultProvider.handlerDegradeServiceRequest(request,ctx.channel(),AUTO_DEGRADE_SERVICE);
        }
        return null;
    }
}
