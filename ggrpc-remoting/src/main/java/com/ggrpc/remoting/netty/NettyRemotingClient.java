/**
 * FileName: NettyRemotingClient
 * Author:   yangqinkuan
 * Date:     2019-12-5 9:54
 * Description:
 */

package com.ggrpc.remoting.netty;

import com.ggrpc.common.exception.remoting.RemotingException;
import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.common.utils.NamedThreadFactory;
import com.ggrpc.common.utils.NativeSupport;
import com.ggrpc.common.utils.Pair;
import com.ggrpc.remoting.ConnectionUtils;
import com.ggrpc.remoting.NettyRemotingBase;
import com.ggrpc.remoting.RPCHook;
import com.ggrpc.remoting.model.NettyChannelInactiveProcessor;
import com.ggrpc.remoting.model.NettyRequestProcessor;
import com.ggrpc.remoting.model.RemotingTransporter;
import com.ggrpc.remoting.netty.decode.RemotingTransporterDecoder;
import com.ggrpc.remoting.netty.encode.RemotingTransporterEncoder;
import com.ggrpc.remoting.netty.idle.ConnectorIdleStateTrigger;
import com.ggrpc.remoting.netty.idle.IdleStateChecker;
import com.ggrpc.remoting.watcher.ConnectionWatchdog;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.ggrpc.common.utils.Constants.READER_IDLE_TIME_SECONDS;
import static com.ggrpc.common.utils.Constants.WRITER_IDLE_TIME_SECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
// 客户端netty连接
public class NettyRemotingClient extends NettyRemotingBase implements RemotingClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyRemotingClient.class);

    private Bootstrap bootstrap;

    private EventLoopGroup worker;

    private int nWorkers;

    private volatile ByteBufAllocator allocator;

    private final Lock lockChannelTables = new ReentrantLock();

    private static final long LockTimeoutMillis = 3000;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final NettyClientConfig nettyClientConfig;

    private volatile int writeBufferHighWaterMark = -1;
    private volatile int writeBufferLowWaterMark = -1;
    // 心跳拦截器
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();

    protected HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("netty.timer"));

    private RPCHook rpcHook;
    /* addr 存某个服务器对应得Channel*/
    private final ConcurrentHashMap<String, ChannelWrapper> channelTables = new ConcurrentHashMap<>();

    private boolean isReconnect = true;

    public NettyRemotingClient(NettyClientConfig nettyClientConfig) {
        this.nettyClientConfig = nettyClientConfig;
        if (null != nettyClientConfig) {
            nWorkers = nettyClientConfig.getClientWorkerThreads();
            writeBufferLowWaterMark = nettyClientConfig.getWriteBufferLowWaterMark();
            writeBufferHighWaterMark = nettyClientConfig.getWriteBufferHighWaterMark();
        }
        // 对服务器配置参数进行初始化
        init();
    }

    @Override
    public void start() {

        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(//
                nettyClientConfig.getClientWorkerThreads(), //
                new ThreadFactory() {

                    private AtomicInteger threadIndex = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });

        if (isNativeEt()) {
            bootstrap.channel(EpollSocketChannel.class);
        } else {
            bootstrap.channel(NioSocketChannel.class);
        }

        // 重连狗(重连处理器) 主要用于短线重连
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new RemotingTransporterDecoder(), //
                        new RemotingTransporterEncoder(), //
                        //new IdleStateChecker(timer, 0, WRITER_IDLE_TIME_SECONDS, 0),//
                        new IdleStateHandler(0,WRITER_IDLE_TIME_SECONDS,0, TimeUnit.SECONDS),
                        idleStateTrigger, new NettyClientHandler() };
            }
        };
        watchdog.setReconnect(isReconnect);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(//
                        defaultEventExecutorGroup, //
                        watchdog.handlers());
            }
        });
    }

    // 进行参数配置
    @Override
    public void init() {
        ThreadFactory workerFactory = new DefaultThreadFactory("netty.client");
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new Bootstrap().group(worker);

        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(100);
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(100);
        }

        bootstrap.option(ChannelOption.ALLOCATOR, allocator).option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) SECONDS.toMillis(3));

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.ALLOW_HALF_CLOSURE, false);

        if (writeBufferLowWaterMark >= 0 && writeBufferHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(writeBufferLowWaterMark, writeBufferHighWaterMark);
            bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }
    }

    public Channel getAndCreateChannel(final String addr) throws InterruptedException {

        if (null == addr) {
            logger.warn("address is null");
            return null;
        }

        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        return this.createChannel(addr);
    }
    // 创建一个连接
    public Channel createChannel(String addr) throws InterruptedException {

        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        // 缓存中没有lock住 channelTables
        if (this.lockChannelTables.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
            try {
                boolean createNewConnection = false;
                cw = this.channelTables.get(addr);
                if (cw != null) {
                    // 校验channel的状态
                    if (cw.isOK()) {
                        return cw.getChannel();
                    } else if (!cw.getChannelFuture().isDone()) {
                        createNewConnection = false;
                    } else {
                        // 如果缓存中channel的状态不正确的情况下，则将此不健康的channel从缓存中移除，重新创建
                        this.channelTables.remove(addr);
                        createNewConnection = true;
                    }
                } else {
                    createNewConnection = true;
                }

                // 注意这边的bootstrap在MQClientInstance.java中的this.mQClientAPIImpl.start()的这个方法初始化过，这边只需要connect一下就可以连接了
                if (createNewConnection) {
                    ChannelFuture channelFuture = this.bootstrap.connect(ConnectionUtils.string2SocketAddress(addr));
                    System.out.println("我在这里连接到注册中心拉啊啊啊啊啊啊");
                    logger.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
                    // 将返回的Netty对象的ChannelFuture对象编织成一个cw
                    cw = new ChannelWrapper(channelFuture);
                    // 放入缓存
                    this.channelTables.put(addr, cw);
                }
            } catch (Exception e) {
                logger.error("createChannel: create channel exception", e);
            } finally {
                // 释放锁
                this.lockChannelTables.unlock();
            }
        } else {
            logger.warn("createChannel: try to lock channel table, but timeout, {}ms", LockTimeoutMillis);
        }

        if (cw != null) {
            ChannelFuture channelFuture = cw.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMillis())) {
                if (cw.isOK()) {
                    logger.info("createChannel: connect remote host[{}] success, {}", addr, channelFuture.toString());
                    // 返回Netty原生的Channel，一个信息发射利器
                    return cw.getChannel();
                } else {
                    logger.warn("createChannel: connect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                logger.warn("createChannel: connect remote host[{}] timeout {}ms, {}", addr, this.nettyClientConfig.getConnectTimeoutMillis(),
                        channelFuture.toString());
            }
        }

        return null;
    }

    @Override
    public void registerProcessor(byte requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService executorThis = executor;
        if (null == executor) {
            executorThis = this.publicExecutor;
        }

        Pair<NettyRequestProcessor, ExecutorService> pair =
                new Pair<>(processor, executorThis);
        this.processorTable.put(requestCode, pair);
    }

    @Override
    public boolean isChannelWriteable(String addr) {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.isWriteable();
        }
        return true;
    }
    // 客户端handler
    class NettyClientHandler extends SimpleChannelInboundHandler<RemotingTransporter> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingTransporter msg) throws Exception {
            // 对数据进行处理
            processMessageReceived(ctx, msg);
        }


        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // channleInactive
            processChannelInactive(ctx);
        }
    }

    @Override
    public void shutdown() {
        try {
            this.timer.stop();
            this.timer = null;
            for (ChannelWrapper cw : this.channelTables.values()) {
                this.closeChannel(null, cw.getChannel());
            }

            this.channelTables.clear();

            this.worker.shutdownGracefully();

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            logger.error("NettyRemotingClient shutdown exception, ", e);
        }

        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
                logger.error("NettyRemotingClient shutdown exception, ", e);
            }
        }
    }

    @Override
    public void registryRPCHook(RPCHook rocHook) {
        this.rpcHook = rpcHook;
    }


    @Override
    protected RPCHook getRPCHook() {
        return rpcHook;
    }

    private EventLoopGroup initEventLoopGroup(int nWorkers, ThreadFactory workerFactory) {
        return isNativeEt() ? new EpollEventLoopGroup(nWorkers, workerFactory) : new NioEventLoopGroup(nWorkers, workerFactory);
    }

    private boolean isNativeEt() {
        return NativeSupport.isSupportNativeET();
    }

    @Override
    public RemotingTransporter invokeSync(String addr, RemotingTransporter request, long timeoutMillis) throws InterruptedException, RemotingException {
        // 获取到指定的channel,如果没有则创建
        final Channel channel = this.getAndCreateChannel(addr);
        if (channel != null && channel.isActive()) {
            try {
                // 回调前置钩子
                if (this.rpcHook != null) {
                    this.rpcHook.doBeforeRequest(addr, request);
                }
                // 有了channel，有了request，request中也有了请求的Request.Code和Topic值，那么就是万事具备了，channel.writeAndFlush(request)就OK了
                RemotingTransporter response = this.invokeSyncImpl(channel, request, timeoutMillis);
                // 后置回调钩子
                if (this.rpcHook != null) {
                    this.rpcHook.doAfterResponse(ConnectionUtils.parseChannelRemoteAddr(channel), request, response);
                }
                return response;
            } catch (RemotingSendRequestException e) {
                logger.warn("invokeSync: send request exception, so close the channel[{}]", addr);
                this.closeChannel(addr, channel);
                throw e;
            } catch (RemotingTimeoutException e) {
                logger.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
                throw e;
            }
        } else {
            // 如果该channel是不健康的(创建的时候也许是好的，放入到缓存table的时候也是好的，就是在用的时候，它不行了，尼玛，不好意思，就需要将你从table中移除掉)
            this.closeChannel(addr, channel);
            throw new RemotingException(addr + " connection exception");
        }
    }

    private void closeChannel(String addr, Channel channel) {
        if (null == channel)
            return;

        final String addrRemote = null == addr ? ConnectionUtils.parseChannelRemoteAddr(channel) : addr;

        try {
            if (this.lockChannelTables.tryLock(LockTimeoutMillis, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    final ChannelWrapper prevCW = this.channelTables.get(addrRemote);

                    logger.info("closeChannel: begin close the channel[{}] Found: {}", addrRemote, (prevCW != null));

                    if (null == prevCW) {
                        logger.info("closeChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    } else if (prevCW.getChannel() != channel) {
                        logger.info("closeChannel: the channel[{}] has been closed before, and has been created again, nothing to do.", addrRemote);
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        logger.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                    }

                    ConnectionUtils.closeChannel(channel);
                } catch (Exception e) {
                    logger.error("closeChannel: close the channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                logger.warn("closeChannel: try to lock channel table, but timeout, {}ms", LockTimeoutMillis);
            }
        } catch (InterruptedException e) {
            logger.error("closeChannel exception", e);
        }
    }

    // channel的编织包装类
    class ChannelWrapper {

        private final ChannelFuture channelFuture;

        public ChannelWrapper(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        public boolean isOK() {
            return (this.channelFuture.channel() != null && this.channelFuture.channel().isActive());
        }

        public boolean isWriteable() {
            return this.channelFuture.channel().isWritable();
        }

        private Channel getChannel() {
            return this.channelFuture.channel();
        }

        public ChannelFuture getChannelFuture() {
            return channelFuture;
        }
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    public void setreconnect(boolean isReconnect) {
        this.isReconnect = isReconnect;
    }

    @Override
    public void registerChannelInactiveProcessor(NettyChannelInactiveProcessor processor, ExecutorService executor) {
        if(null == executor){
            executor = this.publicExecutor;
        }
        this.defaultChannelInactiveProcessor = new Pair<NettyChannelInactiveProcessor, ExecutorService>(processor, executor);
    }
}
