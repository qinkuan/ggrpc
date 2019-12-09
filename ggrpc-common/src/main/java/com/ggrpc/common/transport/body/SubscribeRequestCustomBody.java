package com.ggrpc.common.transport.body;


import com.ggrpc.common.exception.remoting.RemotingCommmonCustomException;

/**
 * 
 *消费者订阅服务的主题消息，这边做的相对简单，只要有唯一的名字控制就好
 */
public class SubscribeRequestCustomBody implements CommonCustomBody {
	
	private String serviceName;
	
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}
	

}
