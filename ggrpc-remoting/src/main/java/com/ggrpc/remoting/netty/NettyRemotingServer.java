/**
 * FileName: NettyRemotingServer
 * Author:   yangqinkuan
 * Date:     2019-12-5 10:41
 * Description:
 */

package com.ggrpc.remoting.netty;

import com.ggrpc.common.exception.remoting.RemotingSendRequestException;
import com.ggrpc.common.exception.remoting.RemotingTimeoutException;
import com.ggrpc.common.utils.NamedThreadFactory;
import com.ggrpc.common.utils.NativeSupport;
import com.ggrpc.common.utils.Pair;
import com.ggrpc.remoting.NettyRemotingBase;
import com.ggrpc.remoting.RPCHook;
import com.ggrpc.remoting.model.NettyChannelInactiveProcessor;
import com.ggrpc.remoting.model.NettyRequestProcessor;
import com.ggrpc.remoting.model.RemotingTransporter;
import com.ggrpc.remoting.netty.decode.RemotingTransporterDecoder;
import com.ggrpc.remoting.netty.encode.RemotingTransporterEncoder;
import com.ggrpc.remoting.netty.idle.AcceptorIdleStateTrigger;
import com.ggrpc.remoting.netty.idle.IdleStateChecker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ggrpc.common.utils.Constants.AVAILABLE_PROCESSORS;
import static com.ggrpc.common.utils.Constants.READER_IDLE_TIME_SECONDS;

public class NettyRemotingServer extends NettyRemotingBase implements RemotingServer {

    private static final Logger logger = LoggerFactory.getLogger(NettyRemotingServer.class);

    private ServerBootstrap serverBootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private int workerNum;
    private int writeBufferLowWaterMark;
    private int writeBufferHighWaterMark;
    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("netty.acceptor.timer"));


    protected volatile ByteBufAllocator allocator;

    private final NettyServerConfig nettyServerConfig;
    // 默认时间处理线程池
    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private final ExecutorService publicExecutor;

    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();

    private RPCHook rpcHook;

    public NettyRemotingServer(){
        this(new NettyServerConfig());
    }

    public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
        this.nettyServerConfig = nettyServerConfig;
        if(null != nettyServerConfig){
            workerNum = nettyServerConfig.getServerWorkerThreads();
            writeBufferLowWaterMark = nettyServerConfig.getWriteBufferLowWaterMark();
            writeBufferHighWaterMark = nettyServerConfig.getWriteBufferHighWaterMark();
        }
        this.publicExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);


            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        init();
    }

    @Override
    public void init() {

        ThreadFactory bossFactory = new DefaultThreadFactory("netty.boss");
        ThreadFactory workerFactory = new DefaultThreadFactory("netty.worker");


        boss = initEventLoopGroup(1, bossFactory);


        if(workerNum <= 0){
            workerNum = Runtime.getRuntime().availableProcessors() << 1;
        }
        worker = initEventLoopGroup(workerNum, workerFactory);

        serverBootstrap = new ServerBootstrap().group(boss, worker);

        allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

        serverBootstrap.childOption(ChannelOption.ALLOCATOR, allocator)
                .childOption(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT);

        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(100);
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(100);
        }


        serverBootstrap.option(ChannelOption.SO_BACKLOG, 32768);
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, true);

        // child options
        serverBootstrap.childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false);


        if (writeBufferLowWaterMark >= 0 && writeBufferHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(writeBufferLowWaterMark, writeBufferHighWaterMark);
            serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }

    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
                AVAILABLE_PROCESSORS, new ThreadFactory() {

            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "NettyServerWorkerThread_" + this.threadIndex.incrementAndGet());
            }
        });
        // 判断操作系统，选择不同的Serversocket  其实就是epoll和select
        if (isNativeEt()) {
            serverBootstrap.channel(EpollServerSocketChannel.class);
        } else {
            serverBootstrap.channel(NioServerSocketChannel.class);
        }
        // 配置ip 端口地址
        serverBootstrap.localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()));
        serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                        // 时间处理线程组
                        defaultEventExecutorGroup,
                        // 自己的链路空闲链路检测
                        //new IdleStateChecker(timer, READER_IDLE_TIME_SECONDS, 0, 0),
                        // netty链路检测
                        new IdleStateHandler(READER_IDLE_TIME_SECONDS,0,0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        new RemotingTransporterDecoder()
                        ,new RemotingTransporterEncoder()
                        ,new NettyServerHandler());
            }
        });

        try {
            logger.info("netty bind [{}] serverBootstrap start...",this.nettyServerConfig.getListenPort());
            this.serverBootstrap.bind().sync();
            logger.info("netty start success at port [{}]",this.nettyServerConfig.getListenPort());
        }
        catch (InterruptedException e1) {
            logger.error("start serverBootstrap exception [{}]",e1.getMessage());
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }


    }

    @Override
    public void shutdown() {
        try {
            if (this.timer != null) {
                this.timer.stop();
            }

            this.boss.shutdownGracefully();

            this.worker.shutdownGracefully();

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        }
        catch (Exception e) {
            logger.error("NettyRemotingServer shutdown exception, ", e);
        }

        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            }
            catch (Exception e) {
                logger.error("NettyRemotingServer shutdown exception, ", e);
            }
        }
    }

    @Override
    public void registryRPCHook(RPCHook rpcHook) {
        this.rpcHook = rpcHook;
    }

    @Override
    public void registerProecessor(byte requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        ExecutorService _executor = executor;
        if (null == executor) {
            _executor = this.publicExecutor;
        }
        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>(processor, _executor);
        this.processorTable.put(requestCode, pair);
    }

    @Override
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor = new Pair<NettyRequestProcessor, ExecutorService>(processor, executor);
    }

    @Override
    public void registerChannelInactiveProcessor(NettyChannelInactiveProcessor processor, ExecutorService executor) {
        if(executor == null){
            executor = super.publicExecutor;
        }
        this.defaultChannelInactiveProcessor = new Pair<NettyChannelInactiveProcessor, ExecutorService>(processor, executor);
    }

    @Override
    public Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(int requestCode) {
        return processorTable.get(requestCode);
    }

    @Override
    public RemotingTransporter invokeSync(Channel channel, RemotingTransporter request, long timeoutMillis) throws InterruptedException,
            RemotingSendRequestException, RemotingTimeoutException {
        return super.invokeSyncImpl(channel, request, timeoutMillis);
    }

    @Override
    protected RPCHook getRPCHook() {
        return rpcHook;
    }


    private EventLoopGroup initEventLoopGroup(int workers, ThreadFactory bossFactory) {
        return isNativeEt() ? new EpollEventLoopGroup(workers, bossFactory) : new NioEventLoopGroup(workers, bossFactory);
    }

    private boolean isNativeEt() {
        return NativeSupport.isSupportNativeET();
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<RemotingTransporter> {
        // 事件处理
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemotingTransporter msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
        // 断线处理
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            processChannelInactive(ctx);
        }


    }

}
