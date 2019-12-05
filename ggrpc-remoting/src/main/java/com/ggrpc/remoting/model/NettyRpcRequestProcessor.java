/**
 * FileName: NettyRpcRequestProcessor
 * Author:   yangqinkuan
 * Date:     2019-12-5 10:40
 * Description:
 */

package com.ggrpc.remoting.model;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRpcRequestProcessor {
    void processRPCRequest(ChannelHandlerContext ctx, RemotingTransporter request);
}
