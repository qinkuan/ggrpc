/**
 * FileName: ConnectorIdleStateTrigger
 * Author:   yangqinkuan
 * Date:     2019-12-5 15:32
 * Description:
 */

package com.ggrpc.remoting.netty.idle;

import com.ggrpc.remoting.model.Heartbeats;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@ChannelHandler.Sharable
public class ConnectorIdleStateTrigger extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            IdleState state = ((IdleStateEvent)evt).state();
            if(state == IdleState.WRITER_IDLE){
                ctx.writeAndFlush(Heartbeats.heartbeatContent());
            }
        } else{
            super.userEventTriggered(ctx, evt);
        }

    }
}
