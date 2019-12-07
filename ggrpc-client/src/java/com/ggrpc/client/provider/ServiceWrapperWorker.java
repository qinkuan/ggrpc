package com.ggrpc.client.provider;

import com.ggrpc.client.interceptor.ProviderProxyHandler;
import com.ggrpc.client.provider.model.ServiceWrapper;

import java.util.List;

public interface ServiceWrapperWorker {

    ServiceWrapperWorker provider(Object serviceProvider);

    ServiceWrapperWorker provider(ProviderProxyHandler proxyHandler, Object serviceProvider);

    List<ServiceWrapper> create();
}
