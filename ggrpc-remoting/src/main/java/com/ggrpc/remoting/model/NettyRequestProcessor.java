/**
 * FileName: NettyRequestProcessor
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:34
 * Description:
 */

package com.ggrpc.remoting.model;

import io.netty.channel.ChannelHandlerContext;

public interface NettyRequestProcessor {
    RemotingTransporter processRequest(ChannelHandlerContext ctx,RemotingTransporter request) throws Exception;
}
