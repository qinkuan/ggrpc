package com.ggrpc.common.transport.body;

import com.ggrpc.common.exception.remoting.RemotingCommmonCustomException;
import com.ggrpc.common.rpc.MetricsReporter;

import java.util.List;

public class ProviderMetricsCustomBody implements CommonCustomBody{
    private List<MetricsReporter> metricsReporter;
    @Override
    public void checkFields() throws RemotingCommmonCustomException {

    }
    public List<MetricsReporter> getMetricsReporter() {
        return metricsReporter;
    }

    public void setMetricsReporter(List<MetricsReporter> metricsReporter) {
        this.metricsReporter = metricsReporter;
    }
}
