package com.ggrpc.example.service;


import com.ggrpc.client.annotation.RPCService;

public class HelloService_3 implements HelloService {

	@Override
	@RPCService(responsibilityName = "xiaoy", serviceName = "LAOPOPO.TEST.SAYHELLO", weight = 5)
	public String sayHello(String str) {
		return "hello_3";
	}

}
