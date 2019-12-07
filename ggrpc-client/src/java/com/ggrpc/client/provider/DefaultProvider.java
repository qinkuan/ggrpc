package com.ggrpc.client.provider;

import com.ggrpc.common.exception.remoting.RemotingException;
import com.ggrpc.common.transport.body.PublishServiceCustomBody;
import com.ggrpc.remoting.model.RemotingTransporter;
import com.ggrpc.remoting.netty.NettyClientConfig;
import com.ggrpc.remoting.netty.NettyRemotingClient;
import com.ggrpc.remoting.netty.NettyRemotingServer;
import com.ggrpc.remoting.netty.NettyServerConfig;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class DefaultProvider implements Provider{
    private static final Logger logger = LoggerFactory.getLogger(DefaultProvider.class);

    private NettyClientConfig clientConfig;               // 向注册中心连接的netty client配置
    private NettyServerConfig serverConfig; 			  // 等待服务提供者连接的netty server的配置
    private NettyRemotingClient nettyRemotingClient; 	  // 连接monitor和注册中心
    private NettyRemotingServer nettyRemotingServer;      // 等待被Consumer连接
    private NettyRemotingServer nettyRemotingVipServer;   // 等待被Consumer VIP连接
    private ProviderRegistryController providerController;// provider端向注册中心连接的业务逻辑的控制器
    private ProviderRPCController providerRPCController;  // consumer端远程调用的核心控制器
    private ExecutorService remotingExecutor;             // RPC调用的核心线程执行器
    private ExecutorService remotingVipExecutor; 		  // RPC调用VIP的核心线程执行器
    private Channel monitorChannel; 					  // 连接monitor端的channel

    /********* 要发布的服务的信息 ***********/
    private List<RemotingTransporter> publishRemotingTransporters;
    /************ 全局发布的信息 ************/
    private ConcurrentMap<String, PublishServiceCustomBody> globalPublishService = new ConcurrentHashMap<String, PublishServiceCustomBody>();
    /***** 注册中心的地址 ******/
    private String registryAddress;
    /******* 服务暴露给consumer的地址 ********/
    private int exposePort;
    /************* 监控中心的monitor的地址 *****************/
    private String monitorAddress;
    /*********** 要提供的服务 ***************/
    private Object[] obj;

    // 当前provider端状态是否健康，也就是说如果注册宕机后，该provider端的实例信息是失效，这是需要重新发送注册信息,因为默认状态下start就是发送，只有channel
    // inactive的时候说明短线了，需要重新发布信息
    private boolean ProviderStateIsHealthy = true;

    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("provider-timer"));

    public DefaultProvider() {
        this.clientConfig = new NettyClientConfig();
        this.serverConfig = new NettyServerConfig();
        providerController = new ProviderRegistryController(this);
        providerRPCController = new ProviderRPCController(this);
        initialize();
    }











    @Override
    public void start() throws InterruptedException, RemotingException {

    }

    @Override
    public void publishedAndStartProvider() throws InterruptedException, RemotingException {

    }

    @Override
    public Provider serviceListenPort(int exposePort) {
        return null;
    }

    @Override
    public Provider registryAddress(String registryAddress) {
        return null;
    }

    @Override
    public Provider monitorAddress(String monitorAddress) {
        return null;
    }

    @Override
    public Provider publishService(Object... obj) {
        return null;
    }

    @Override
    public void handlerRPCRequest(RemotingTransporter request, Channel channel) {

    }
}
