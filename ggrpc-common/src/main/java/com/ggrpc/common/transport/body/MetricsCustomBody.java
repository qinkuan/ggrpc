package com.ggrpc.common.transport.body;

import com.ggrpc.common.exception.remoting.RemotingCommmonCustomException;
import com.ggrpc.common.metrics.ServiceMetrics;

import java.util.List;

public class MetricsCustomBody implements CommonCustomBody {
	
	private List<ServiceMetrics> serviceMetricses;

	@Override
	public void checkFields() throws RemotingCommmonCustomException {
	}

	public List<ServiceMetrics> getServiceMetricses() {
		return serviceMetricses;
	}

	public void setServiceMetricses(List<ServiceMetrics> serviceMetricses) {
		this.serviceMetricses = serviceMetricses;
	}
	
	
	

}