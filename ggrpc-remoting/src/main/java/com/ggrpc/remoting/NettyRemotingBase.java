/**
 * FileName: NettyRemotingBase
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:56
 * Description:
 */

package com.ggrpc.remoting;

import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.common.utils.Pair;
import com.ggrpc.remoting.model.NettyChannelInactiveProcessor;
import com.ggrpc.remoting.model.NettyRequestProcessor;
import com.ggrpc.remoting.model.RemotingResponse;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class NettyRemotingBase {
    private static final Logger logger = LoggerFactory.getLogger(NettyRemotingBase.class);
    /******key为请求的opaque value是远程返回的结果封装类******/
    protected final ConcurrentHashMap<Long, RemotingResponse> responseTable = new ConcurrentHashMap<Long, RemotingResponse>(256);

    //如果使用者没有对创建的Netty网络段注入某个特定请求的处理器的时候，默认使用该默认的处理器
    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    //netty网络段channelInactive事件发生的处理器
    protected Pair<NettyChannelInactiveProcessor,ExecutorService> defaultChannelInactiveProcessor;

    protected final ExecutorService publicExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);


        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
        }
    });

    //注入的某个requestCode对应的处理器放入到HashMap中，键值对一一匹配
    protected final HashMap<Byte/* request code */, Pair<NettyRequestProcessor, ExecutorService>> processorTable =
            new HashMap<>(64);

    //远程端的调用具体实现
    public RemotingTransporter invokeSyncImpl(final Channel channel, final RemotingTransporter request, final long timeoutMillis) throws RemotingTimeoutException, RemotingSendRequestException, InterruptedException{

        try {
            //构造一个请求的封装体，请求Id和请求结果一一对应
            final RemotingResponse remotingResponse = new RemotingResponse(request.getOpaque(), timeoutMillis, null);
            //将请求放入一个"篮子"中，等远程端填充该篮子中嗷嗷待哺的每一个结果集
            this.responseTable.put(request.getOpaque(), remotingResponse);
            //发送请求
            channel.writeAndFlush(request).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()){
                    //如果发送对象成功，则设置成功
                    remotingResponse.setSendRequestOK(true);
                    return;
                }else{
                    remotingResponse.setSendRequestOK(false);
                }
                //如果请求发送直接失败，则默认将其从responseTable这个篮子中移除
                responseTable.remove(request.getOpaque());
                //失败的异常信息
                remotingResponse.setCause(future.cause());
                //设置当前请求的返回主体返回体是null(请求失败的情况下，返回的结果肯定是null)
                remotingResponse.putResponse(null);
                logger.warn("use channel [{}] send msg [{}] failed and failed reason is [{}]",channel,request,future.cause().getMessage());
            });

            RemotingTransporter remotingTransporter = remotingResponse.waitResponse();
            if(null == remotingTransporter){
                //如果发送是成功的，则说明远程端，处理超时了
                if (remotingResponse.isSendRequestOK()) {
                    throw new RemotingTimeoutException(ConnectionUtils.parseChannelRemoteAddr(channel),
                            timeoutMillis, remotingResponse.getCause());
                }else{
                    throw new RemotingSendRequestException(ConnectionUtils.parseChannelRemoteAddr(channel),
                            remotingResponse.getCause());
                }
            }
            return remotingTransporter;
        }finally {
            //最后不管怎么样，都需要将其从篮子中移除出来，否则篮子会撑爆的
            this.responseTable.remove(request.getOpaque());
        }
    }
}
