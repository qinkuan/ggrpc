/**
 * FileName: BaseRemotingService
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:10
 * Description:
 */

package com.ggrpc.remoting.netty;

import com.ggrpc.remoting.RPCHook;

public interface BaseRemotingService {
    /**
     * Netty的一些参数的初始化
     */
    void init();
    /**
     * 启动Netty方法
     */
    void start();

    /**
     * 关闭Netty C/S实例
     */
    void shutdown();

    /**
     * 注入钩子，Netty在处理的过程中可以嵌入一些方法，增加代码的灵活性
     */
    void registryRPCHook(RPCHook rpcHook);
}
