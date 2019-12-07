package com.ggrpc.example.netty;

import com.ggrpc.common.exception.remoting.RemotingException;
import com.ggrpc.remoting.model.RemotingTransporter;
import com.ggrpc.remoting.netty.NettyClientConfig;
import com.ggrpc.remoting.netty.NettyRemotingClient;


public class NettyClientTest {
	
	public static final byte TEST = -1;
	
	public static void main(String[] args) throws InterruptedException, RemotingException {
		NettyClientConfig nettyClientConfig = new NettyClientConfig();
		NettyRemotingClient client = new NettyRemotingClient(nettyClientConfig);
		client.start();
		
		TestCommonCustomBody.ComplexTestObj complexTestObj = new TestCommonCustomBody.ComplexTestObj("attr1", 2);
		TestCommonCustomBody commonCustomHeader = new TestCommonCustomBody(1, "test",complexTestObj);
		
		RemotingTransporter remotingTransporter = RemotingTransporter.createRequestTransporter(TEST, commonCustomHeader);
		RemotingTransporter request = client.invokeSync("127.0.0.1:18001", remotingTransporter, 3000);
		System.out.println(request);
	}
	
}
