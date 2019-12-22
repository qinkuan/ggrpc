package com.ggrpc.example.service;


import com.ggrpc.client.annotation.RPCService;

/**
 * 
 * @author BazingaLyn
 * @description Demo
 * @time 2016年8月19日
 * @modifytime
 */
public class HelloSerivceImpl implements HelloService {

	@Override
	@RPCService(responsibilityName="xiaoy",
				serviceName="LAOPOPO.TEST.SAYHELLO",
				isVIPService = false,
				isSupportDegradeService = true,
				degradeServicePath="com.ggrpc.example.service.HelloServiceMock",
				degradeServiceDesc="默认返回hello",
				maxCallCountInMinute = 40)
	public String sayHello(String str) {
		
		//真实逻辑可能涉及到查库
		return "hello "+ str;
		
	}

}
