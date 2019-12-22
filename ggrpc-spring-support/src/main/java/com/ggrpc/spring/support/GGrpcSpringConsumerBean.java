package com.ggrpc.spring.support;

import com.ggrpc.client.consumer.Consumer;
import com.ggrpc.client.consumer.ConsumerClient;
import com.ggrpc.client.consumer.proxy.ProxyFactory;
import com.ggrpc.common.utils.UnresolvedAddress;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class GGrpcSpringConsumerBean<T> implements FactoryBean<T>, InitializingBean {

    private transient T proxy;
    private Class<T> rpcClass;

    private ConsumerClient consumerClient;
    private String subscribeServiceName;
    private long timeoutMillis = 3000l;

    private UnresolvedAddress[] unresolvedAddresses;


    @Override
    public void afterPropertiesSet() throws Exception {

        if(null != subscribeServiceName){

            if(unresolvedAddresses == null || unresolvedAddresses.length == 0){

                Consumer.SubscribeManager subscribeManager = consumerClient.subscribeService(subscribeServiceName);
                if (!subscribeManager.waitForAvailable(timeoutMillis)) {
                    throw new Exception("no service provider");
                }
                proxy = ProxyFactory.factory(rpcClass).consumer(consumerClient).newProxyInstance();
            }else{
                proxy = ProxyFactory.factory(rpcClass).consumer(consumerClient).addProviderAddress(unresolvedAddresses).timeoutMillis(timeoutMillis).newProxyInstance();
            }
        }
    }

    @Override
    public T getObject() throws Exception {
        return proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return rpcClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public T getProxy() {
        return proxy;
    }

    public void setProxy(T proxy) {
        this.proxy = proxy;
    }

    public Class<T> getRpcClass() {
        return rpcClass;
    }

    public void setRpcClass(Class<T> rpcClass) {
        this.rpcClass = rpcClass;
    }

    public ConsumerClient getConsumerClient() {
        return consumerClient;
    }

    public void setConsumerClient(ConsumerClient consumerClient) {
        this.consumerClient = consumerClient;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public String getSubscribeServiceName() {
        return subscribeServiceName;
    }

    public void setSubscribeServiceName(String subscribeServiceName) {
        this.subscribeServiceName = subscribeServiceName;
    }

    public UnresolvedAddress[] getUnresolvedAddresses() {
        return unresolvedAddresses;
    }

    public void setUnresolvedAddresses(UnresolvedAddress[] unresolvedAddresses) {
        this.unresolvedAddresses = unresolvedAddresses;
    }
}
