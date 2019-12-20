/**
 * FileName: ConsumerConfig
 * Author:   yangqinkuan
 * Date:     2019-12-9 11:50
 * Description:
 */

package com.ggrpc.client.consumer;

public class ConsumerConfig {
    // 重试注册连接的次数
    private int retryConnectionRegistryTimes = 4;
    // 最大重试连接次数
    private long maxRetryConnectionRegsitryTime = 5000;
    // 注册超时时间
    private long registryTimeout = 3000;

    public int getRetryConnectionRegistryTimes() {
        return retryConnectionRegistryTimes;
    }

    public void setRetryConnectionRegistryTimes(int retryConnectionRegistryTimes) {
        this.retryConnectionRegistryTimes = retryConnectionRegistryTimes;
    }

    public long getMaxRetryConnectionRegsitryTime() {
        return maxRetryConnectionRegsitryTime;
    }

    public void setMaxRetryConnectionRegsitryTime(long maxRetryConnectionRegsitryTime) {
        this.maxRetryConnectionRegsitryTime = maxRetryConnectionRegsitryTime;
    }

    public long getRegistryTimeout() {
        return registryTimeout;
    }

    public void setRegistryTimeout(long registryTimeout) {
        this.registryTimeout = registryTimeout;
    }


}
