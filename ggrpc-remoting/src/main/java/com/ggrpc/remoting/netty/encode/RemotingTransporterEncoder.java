/**
 * FileName: RemotingTransporterEncoder
 * Author:   yangqinkuan
 * Date:     2019-12-3 16:13
 * Description:
 */

package com.ggrpc.remoting.netty.encode;

import com.ggrpc.common.protocal.GGprotocol;
import com.ggrpc.common.serialization.SerializerHolder;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

public class RemotingTransporterEncoder  extends MessageToByteEncoder<RemotingTransporter> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingTransporter msg, ByteBuf out) throws IOException {
        doEncodeRemotingTransporter(msg, out);
    }

    private void doEncodeRemotingTransporter(RemotingTransporter msg, ByteBuf out) throws IOException {
        byte[] body = SerializerHolder.serializerImpl().writeObject(msg.getCustomHeader());


        byte isCompress = GGprotocol.UNCOMPRESS;
//		if(body.length > 1024){ //经过测试，压缩之后的效率低于不压缩
//			isCompress = LaopopoProtocol.COMPRESS;
//			body = Snappy.compress(body);
//		}

        out.writeShort(GGprotocol.MAGIC). 	           //协议头
                writeByte(msg.getTransporterType())// 传输类型 sign 是请求还是响应
                .writeByte(msg.getCode())          // 请求类型requestcode 表明主题信息的类型，也代表请求的类型
                .writeLong(msg.getOpaque())        //requestId
                .writeInt(body.length)             //length
                .writeByte(isCompress)			   //是否压缩
                .writeBytes(body);

    }

}
