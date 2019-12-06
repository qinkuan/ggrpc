/**
 * FileName: Heartbeats
 * Author:   yangqinkuan
 * Date:     2019-12-5 15:36
 * Description:
 */

package com.ggrpc.remoting.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static com.ggrpc.common.exception.protocal.GGprotocol.HEAD_LENGTH;
import static com.ggrpc.common.exception.protocal.GGprotocol.HEARTBEAT;
import static com.ggrpc.common.exception.protocal.GGprotocol.MAGIC;

public class Heartbeats {
    private static final ByteBuf HEARTBEAT_BUF;
    static {
        ByteBuf buf = Unpooled.buffer(HEAD_LENGTH);
        buf.writeShort(MAGIC);
        buf.writeByte(HEARTBEAT);
        buf.writeByte(0);
        buf.writeLong(0);
        buf.writeInt(0);
        // 这是干嘛
        HEARTBEAT_BUF = Unpooled.unmodifiableBuffer(Unpooled.unreleasableBuffer(buf));
    }
    /**
     * Returns the shared heartbeat content.
     */
    public static ByteBuf heartbeatContent() {
        return HEARTBEAT_BUF.duplicate();
    }
}
