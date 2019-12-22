package com.ggrpc.example.generic.test_1;


import com.ggrpc.client.provider.DefaultProvider;
import com.ggrpc.common.exception.remoting.RemotingException;
import com.ggrpc.example.service.ByeServiceImpl;
import com.ggrpc.example.service.HelloSerivceImpl;
import com.ggrpc.remoting.netty.NettyClientConfig;
import com.ggrpc.remoting.netty.NettyServerConfig;

public class ProviderTest {

	public static void main(String[] args) throws InterruptedException, RemotingException {

		DefaultProvider defaultProvider = new DefaultProvider(new NettyClientConfig(), new NettyServerConfig());

		defaultProvider.registryAddress("127.0.0.1:18010") // 注册中心的地址
				.serviceListenPort(8899) // 暴露服务的端口
				.publishService(new HelloSerivceImpl(), new ByeServiceImpl()) // 暴露的服务
				.start(); // 启动服务

	}

}
