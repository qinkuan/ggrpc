package com.ggrpc.example.service;

public class HelloServiceMock implements HelloService {

	@Override
	public String sayHello(String str) {
		
		//直接给出默认的返回值
		return "hello";
	}

}
