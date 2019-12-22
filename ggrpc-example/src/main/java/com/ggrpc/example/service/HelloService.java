package com.ggrpc.example.service;

import com.ggrpc.client.annotation.RPCConsumer;

/**
 * 
 * @description
 */
public interface HelloService {
	@RPCConsumer(serviceName="LAOPOPO.TEST.SAYHELLO")
	String sayHello(String str);

}
