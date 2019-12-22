package com.ggrpc.monitor;

/**
 * 
 * @description 监控中心，单节点，宕机不影响主体业务处理
 */
public interface MonitorNode {
	
	void start();

}
