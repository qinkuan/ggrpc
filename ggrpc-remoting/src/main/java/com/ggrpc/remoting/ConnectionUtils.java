/**
 * FileName: ConnectionUtils
 * Author:   yangqinkuan
 * Date:     2019-12-5 15:04
 * Description:
 */

package com.ggrpc.remoting;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class ConnectionUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionUtils.class);

    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        final SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }
        return "";
    }
}
