package com.ggrpc.client.provider.Controller;

import com.ggrpc.client.provider.DefaultProvider;
import com.ggrpc.common.exception.remoting.RemotingException;
import com.ggrpc.common.transport.body.AckCustomBody;
import com.ggrpc.common.utils.SystemClock;
import com.ggrpc.remoting.model.RemotingTransporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ggrpc.common.serialization.SerializerHolder.serializerImpl;

/**
 * provider端专业去连接registry的管理控制对象，用来处理provider与registry一切交互事宜
 */
public class RegistryController {

    private static final Logger logger = LoggerFactory.getLogger(RegistryController.class);

    private DefaultProvider defaultProvider;

    private final ConcurrentMap<Long, MessageNonAck> messagesNonAcks = new ConcurrentHashMap<Long, MessageNonAck>();

    public RegistryController(DefaultProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    /**
     *
     *            registry的地址，多个地址格式是host1:port1,host2:port2
     * @throws InterruptedException
     * @throws RemotingException
     */
    public void publishedAndStartProvider() throws InterruptedException, RemotingException {

        // stack copy
        List<RemotingTransporter> transporters = defaultProvider.getPublishRemotingTransporters();

        if(null == transporters || transporters.isEmpty()){
            logger.warn("service is empty please call DefaultProvider #publishService method");
            return;
        }

        String address = defaultProvider.getRegistryAddress();

        if (address == null) {
            logger.warn("registry center address is empty please check your address");
            return;
        }
        String[] addresses = address.split(",");
        if (null != addresses && addresses.length > 0 ) {

            for (String eachAddress : addresses) {

                for (RemotingTransporter request : transporters) {
                    // 向指定的注册中心发布服务
                    pushPublishServiceToRegistry(request,eachAddress);

                }
            }
        }
    }

    private void pushPublishServiceToRegistry(RemotingTransporter request, String eachAddress) throws InterruptedException, RemotingException {
        logger.info("[{}] transporters matched", request);
        // 先假设没返回，悲观主义
        messagesNonAcks.put(request.getOpaque(), new MessageNonAck(request, eachAddress));
        RemotingTransporter remotingTransporter = defaultProvider.getNettyRemotingClient().invokeSync(eachAddress, request, 3000);
        if(null != remotingTransporter){
            AckCustomBody ackCustomBody = serializerImpl().readObject(remotingTransporter.bytes(), AckCustomBody.class);

            logger.info("received ack info [{}]", ackCustomBody);
            if(ackCustomBody.isSuccess()){
                messagesNonAcks.remove(ackCustomBody.getRequestId());
            }
        }else{
            logger.warn("registry center handler timeout");
        }
    }

    public void checkPublishFailMessage() throws InterruptedException, RemotingException{
        if(messagesNonAcks.keySet() != null && messagesNonAcks.keySet().size() > 0){
            logger.warn("have [{}] message send failed,send again",messagesNonAcks.keySet().size());
            for(MessageNonAck ack : messagesNonAcks.values()){
                pushPublishServiceToRegistry(ack.getMsg(), ack.getAddress());
            }
        }
    }


    static class MessageNonAck {

        private final long id;

        private final RemotingTransporter msg;
        private final String address;
        private final long timestamp = SystemClock.millisClock().now();

        public MessageNonAck(RemotingTransporter msg, String address) {
            this.msg = msg;
            this.address = address;

            id = msg.getOpaque();
        }

        public long getId() {
            return id;
        }

        public RemotingTransporter getMsg() {
            return msg;
        }

        public String getAddress() {
            return address;
        }

        public long getTimestamp() {
            return timestamp;
        }

    }
}
