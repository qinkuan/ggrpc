package com.ggrpc.example.benchmark;


import com.ggrpc.client.consumer.Consumer;
import com.ggrpc.client.consumer.ConsumerClient;
import com.ggrpc.client.consumer.ConsumerConfig;
import com.ggrpc.client.consumer.proxy.ProxyFactory;
import com.ggrpc.example.service.HelloService;
import com.ggrpc.remoting.netty.NettyClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * 2016-10-14 14:27:57.034 WARN  [main] [BenchmarkClient] - count=25600000
 * 2016-10-14 14:27:57.035 WARN  [main] [BenchmarkClient] - Request count: 25600000, time: 496 second, qps: 51612
 * 
 * 2016-10-18 16:06:38.906 WARN  [main] [BenchmarkClient] - count=12800000
 * 2016-10-18 16:06:38.906 WARN  [main] [BenchmarkClient] - Request count: 12800000, time: 199 second, qps: 64321
 */
public class BenchmarkClient {
	
	private static final Logger logger = LoggerFactory.getLogger(BenchmarkClient.class);
	static String s = "顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶" +
			"顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶assa"
			;
	public static void main(String[] args) throws Exception {
		
		int processors = Runtime.getRuntime().availableProcessors();

		NettyClientConfig registryNettyClientConfig = new NettyClientConfig();
		registryNettyClientConfig.setDefaultAddress("127.0.0.1:18010");

		NettyClientConfig provideClientConfig = new NettyClientConfig();

		ConsumerClient client = new ConsumerClient(registryNettyClientConfig, provideClientConfig, new ConsumerConfig());

		client.start();

		Consumer.SubscribeManager subscribeManager = client.subscribeService("LAOPOPO.TEST.SAYHELLO");

		if (!subscribeManager.waitForAvailable(3000l)) {
			throw new Exception("no service provider");
		}

		final HelloService helloService = ProxyFactory.factory(HelloService.class).consumer(client).timeoutMillis(3000l).newProxyInstance();

		for (int i = 0; i < 5000; i++) {
			String str = helloService.sayHello("Lyncc");
			System.out.println(str);
		}
		final int t = 5000;
		final int step = 3;
		long start = System.currentTimeMillis();
		final CountDownLatch latch = new CountDownLatch(processors << step);
		final AtomicLong count = new AtomicLong();
		for (int i = 0; i < (processors << step); i++) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < t; i++) {
						try {
							helloService.sayHello(s);

							if (count.getAndIncrement() % 10000 == 0) {
								logger.warn("count=" + count.get());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					latch.countDown();
				}
			}).start();
		}
		try {
			latch.await();
			logger.warn("count=" + count.get());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long second = (System.currentTimeMillis() - start) / 1000;
		logger.warn("Request count: " + count.get() + ", time: " + second + " second, qps: " + count.get() / second);

	}

}
