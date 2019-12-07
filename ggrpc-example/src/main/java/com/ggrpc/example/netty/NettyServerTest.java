package com.ggrpc.example.netty;

import com.ggrpc.common.exception.protocal.GGprotocol;
import com.ggrpc.remoting.model.NettyRequestProcessor;
import com.ggrpc.remoting.model.RemotingTransporter;
import com.ggrpc.remoting.netty.NettyRemotingServer;
import com.ggrpc.remoting.netty.NettyServerConfig;
import io.netty.channel.ChannelHandlerContext;


import java.util.concurrent.Executors;

import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;


public class NettyServerTest {
	
	public static final byte TEST = -1;
	
	public static void main(String[] args) {
		
		NettyServerConfig config = new NettyServerConfig();
		config.setListenPort(18001);
		NettyRemotingServer server = new NettyRemotingServer(config);
		server.registerProecessor(TEST, new NettyRequestProcessor() {
			
			@Override
			public RemotingTransporter processRequest(ChannelHandlerContext ctx, RemotingTransporter transporter) throws Exception {
				transporter.setCustomHeader(serializerImpl().readObject(transporter.bytes(), TestCommonCustomBody.class));
				System.out.println(transporter);
				transporter.setTransporterType(GGprotocol.RESPONSE_REMOTING);
				return transporter;
			}
		}, Executors.newCachedThreadPool());
		server.start();
	}

}
