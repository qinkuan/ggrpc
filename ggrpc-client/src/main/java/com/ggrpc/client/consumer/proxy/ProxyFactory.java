/**
 * FileName: ProxyFactory
 * Author:   yangqinkuan
 * Date:     2019-12-19 12:14
 * Description:
 */

package com.ggrpc.client.consumer.proxy;

import com.ggrpc.client.annotation.RPCConsumer;
import com.ggrpc.client.consumer.Consumer;
import com.ggrpc.common.loadbalance.LoadBalanceStrategy;
import com.ggrpc.common.utils.Proxies;
import com.ggrpc.common.utils.UnresolvedAddress;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @description 代理工厂类，用于对服务接口的编织，
 */
public class ProxyFactory<T> {
    private final Class<T> interfaceClass; 					//编织对象
    private Consumer consumer; 			   					//维护一个消费客户端
    private List<UnresolvedAddress> addresses;		        //该服务的直连url集合
    private long timeoutMillis;								//接口整理超时时间
    private Map<String, Long> methodsSpecialTimeoutMillis;  //每个方法特定的超时时间
    private LoadBalanceStrategy balanceStrategy;			//负载均衡的策略


    /**
     * 设置接口对象
     * @param interfaceClass
     */
    private ProxyFactory(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public static <I> ProxyFactory<I> factory(Class<I> interfaceClass) {
        ProxyFactory<I> factory = new ProxyFactory<>(interfaceClass);
        // 初始化数据
        factory.addresses = new ArrayList<UnresolvedAddress>();
        return factory;
    }


    /**
     * 设置该代理工厂的唯一的消费端
     * @param consumer
     * @return
     */
    public ProxyFactory<T> consumer(Consumer consumer) {
        this.consumer = consumer;
        return this;
    }
    /**
     * 增加直连对象的url
     * @param addresses
     * @return
     */
    public ProxyFactory<T> addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }
    /**
     * 设置直连的负载均衡的策略
     * @param balanceStrategy
     * @return
     */
    public ProxyFactory<T> loadBalance(LoadBalanceStrategy balanceStrategy){
        this.balanceStrategy = balanceStrategy;
        return this;
    }
    /**
     * @param timeoutMillis
     * @return
     */
    public ProxyFactory<T> timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public ProxyFactory<T> methodSpecialTimeoutMillis(String methodName, long timeoutMillis) {
        methodsSpecialTimeoutMillis.put(methodName, timeoutMillis);
        return this;
    }

    /**
     * 返回代理类
     * @return
     */
    public T newProxyInstance() {
        Method[] methods = interfaceClass.getMethods();
        if(methods == null || methods.length == 0){
            throw new UnsupportedOperationException("the interfaceClass has no any methods");
        }

        boolean isAnnotation = false;

        for(Method method : methods){
            // 消费者的服务名称
            RPCConsumer consumerAnnotation = method.getAnnotation(RPCConsumer.class);
            if(null != consumerAnnotation){
                isAnnotation = true;
            }
            String serviceName = consumerAnnotation.serviceName();

            if(addresses != null && addresses.size() > 0){
                for (UnresolvedAddress address : addresses) {
                    // 根据服务名称，保存channel 组
                    consumer.addChannelGroup(serviceName, consumer.group(address));

                }
            }
        }

        if(!isAnnotation){
            throw new UnsupportedOperationException("the interfaceClass no any annotation [@RPConsumer]");
        }

        Object handler = new SynInvoker(consumer, timeoutMillis, methodsSpecialTimeoutMillis, balanceStrategy);

        return Proxies.getDefault().newProxy(interfaceClass, handler);
    }
}
