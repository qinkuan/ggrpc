package com.ggrpc.example.service;


import com.ggrpc.client.annotation.RPCService;

public class HelloServiceBenchmark implements HelloService {

	@Override
	@RPCService(responsibilityName="xiaoy",
	serviceName="LAOPOPO.TEST.SAYHELLO",
	connCount = 4,
	isFlowController = false,
	degradeServiceDesc="默认返回hello")
	public String sayHello(String str) {
		for(int i = 0;i<20000;i++){
			i++;
		}
		return str;
	}
	

}
