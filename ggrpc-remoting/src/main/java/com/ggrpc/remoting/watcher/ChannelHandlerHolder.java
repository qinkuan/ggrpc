/**
 * FileName: ChannelHandlerHolder
 * Author:   yangqinkuan
 * Date:     2019-12-5 16:30
 * Description:
 */

package com.ggrpc.remoting.watcher;

import io.netty.channel.ChannelHandler;

public interface ChannelHandlerHolder {
    ChannelHandler[] handlers();
}
