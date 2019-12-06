/**
 * FileName: NettyChannelInactiveProcessor
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:40
 * Description:
 */

package com.ggrpc.remoting.model;

import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import io.netty.channel.ChannelHandlerContext;

public interface NettyChannelInactiveProcessor {
    void processChannelInactive(ChannelHandlerContext ctx) throws RemotingSendRequestException, RemotingTimeoutException, InterruptedException;
}
