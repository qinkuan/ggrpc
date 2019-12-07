package com.ggrpc.client.provider.Controller;

import com.ggrpc.client.metrics.ServiceMeterManager;
import com.ggrpc.client.provider.DefaultProvider;
import com.ggrpc.client.provider.DefaultServiceProviderContainer;
import com.ggrpc.client.provider.flow.control.ServiceFlowControllerManager;
import com.ggrpc.client.provider.model.ServiceWrapper;
import com.ggrpc.common.exception.protocal.GGprotocol;
import com.ggrpc.common.transport.body.RequestCustomBody;
import com.ggrpc.common.transport.body.ResponseCustomBody;
import com.ggrpc.common.utils.Pair;
import com.ggrpc.common.utils.Status;
import com.ggrpc.common.utils.SystemClock;
import com.ggrpc.remoting.model.RemotingTransporter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;
import static com.ggrpc.common.utils.Reflects.fastInvoke;
import static com.ggrpc.common.utils.Reflects.findMatchingParameterTypes;
import static com.ggrpc.common.utils.Status.APP_FLOW_CONTROL;
import static com.ggrpc.common.utils.Status.BAD_REQUEST;
import static com.ggrpc.common.utils.Status.SERVICE_NOT_FOUND;

/**
 * 处理consumer rpc请求的核心控制器，并统计处理的次数
 */
public class ProviderRPCController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderRPCController.class);

    private DefaultProvider defaultProvider;

    public ProviderRPCController(DefaultProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }


    public void handlerRPCRequest(RemotingTransporter request, Channel channel) {


        String serviceName = null;
        RequestCustomBody body = null;
        int requestSize = 0;

        try {
            byte[] bytes = request.bytes();
            requestSize = bytes.length;
            request.bytes(null);

            body = serializerImpl().readObject(bytes, RequestCustomBody.class);

            request.setCustomHeader(body);
            serviceName = body.getServiceName();

            ServiceMeterManager.incrementRequestSize(serviceName, requestSize);
            ServiceMeterManager.incrementCallTimes(serviceName);

        } catch (Exception e) {
            rejected(BAD_REQUEST, channel, request,serviceName);
            return;
        }

        final Pair<DefaultServiceProviderContainer.CurrentServiceState, ServiceWrapper> pair = defaultProvider.getProviderController().getProviderContainer().lookupService(serviceName);
        if (pair == null || pair.getValue() == null) {
            rejected(SERVICE_NOT_FOUND, channel, request,serviceName);
            return;
        }

        // app flow control
        if(pair.getValue().isFlowController()){

            ServiceFlowControllerManager serviceFlowControllerManager = defaultProvider.getProviderController().getServiceFlowControllerManager();
            if (!serviceFlowControllerManager.isAllow(serviceName)) {
                rejected(APP_FLOW_CONTROL,channel, request,serviceName);
                return;
            }
        }


        process(pair,request,channel,serviceName,body.getTimestamp());
    }

    /**
     * RPC的核心处理
     * @param pair
     * @param request
     * @param channel
     * @param serviceName
     * @param beginTime
     */
    private void process(Pair<DefaultServiceProviderContainer.CurrentServiceState, ServiceWrapper> pair, final RemotingTransporter request, Channel channel, final String serviceName, final long beginTime) {

        Object invokeResult = null;

        DefaultServiceProviderContainer.CurrentServiceState currentServiceState = pair.getKey();
        ServiceWrapper serviceWrapper = pair.getValue();

        Object targetCallObj = serviceWrapper.getServiceProvider();

        Object[] args = ((RequestCustomBody)request.getCustomHeader()).getArgs();

        //判断服务是否已经被设定为自动降级，如果被设置为自动降级且有它自己的mock类的话，则将targetCallObj切换到mock方法上来
        if(currentServiceState.getHasDegrade().get() && serviceWrapper.getMockDegradeServiceProvider() != null){
            targetCallObj = serviceWrapper.getMockDegradeServiceProvider();
        }

        String methodName = serviceWrapper.getMethodName();
        List<Class<?>[]> parameterTypesList = serviceWrapper.getParamters();


        Class<?>[] parameterTypes = findMatchingParameterTypes(parameterTypesList, args);
        invokeResult = fastInvoke(targetCallObj, methodName, parameterTypes, args);

        ResponseCustomBody.ResultWrapper result = new ResponseCustomBody.ResultWrapper();
        result.setResult(invokeResult);
        ResponseCustomBody body = new ResponseCustomBody(Status.OK.value(), result);

        final RemotingTransporter response = RemotingTransporter.createResponseTransporter(GGprotocol.RPC_RESPONSE, body, request.getOpaque());

        channel.writeAndFlush(response).addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {

                long elapsed = SystemClock.millisClock().now() - beginTime;

                if (future.isSuccess()) {

                    ServiceMeterManager.incrementTotalTime(serviceName, elapsed);
                } else {
                    logger.info("request {} get failed response {}", request, response);
                }
            }
        });

    }

    // 拒绝策略执行方法，给客户端返回
    private void rejected(Status status, Channel channel, final RemotingTransporter request, String serviceName) {

        if(null != serviceName){
            ServiceMeterManager.incrementFailTimes(serviceName);
        }
        ResponseCustomBody.ResultWrapper result = new ResponseCustomBody.ResultWrapper();
        switch (status) {
            case BAD_REQUEST:
                result.setError("bad request");
            case SERVICE_NOT_FOUND:
                result.setError(((RequestCustomBody) request.getCustomHeader()).getServiceName() +" no service found");
                break;
            case APP_FLOW_CONTROL:
            case PROVIDER_FLOW_CONTROL:
                result.setError("over unit time call limit");
                break;
            default:
                logger.warn("Unexpected status.", status.description());
                return;
        }
        logger.warn("Service rejected: {}.", result.getError());

        ResponseCustomBody responseCustomBody = new ResponseCustomBody(status.value(), result);
        final RemotingTransporter response = RemotingTransporter.createResponseTransporter(GGprotocol.RPC_RESPONSE, responseCustomBody,
                request.getOpaque());

        channel.writeAndFlush(response).addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.info("request error {} get success response {}", request, response);
                } else {
                    logger.info("request error {} get failed response {}", request, response);
                }
            }
        });
    }


}
