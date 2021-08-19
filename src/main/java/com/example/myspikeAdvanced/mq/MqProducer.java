package com.example.myspikeAdvanced.mq;

import com.alibaba.fastjson.JSON;
import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName MqProducer
 * @create 2021-08-16 16:36
 * @description
 */
@Component
public class MqProducer {
    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;
    @Autowired
    private OrderService orderService;

    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                Map<String, Object> args = (Map) arg;
                Integer itemId = (Integer) args.get("itemId");
                Integer amount = (Integer) args.get("amount");
                Integer userId = (Integer) args.get("userId");
                Integer promoId = (Integer) args.get("promoId");
                //真正要做的事,创建订单
                try {
                    orderService.createOrder(userId, itemId, amount, promoId);
                } catch (BusinessException e) {
                    //如果有异常,那么就回滚
                    e.printStackTrace();
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减库存成功,来判断要返回COMMIT_MESSAGE,ROLLBACK_MESSAGE,还是UNKNOW
                return null;
            }
        });
    }

    /**
     * 同步库存扣减消息
     *
     * @param itemId
     * @param amount
     * @return org.apache.rocketmq.client.producer.SendResult
     * @Date 17:37 2021/8/16
     **/
    public boolean asyncReduceStock(Integer itemId, Integer amount) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 事务型同步库存扣减消息
     *
     * @param itemId
     * @param amount
     * @return boolean
     * @Date 17:49 2021/8/19
     **/
    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer amount, Integer promoId) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        Map<String, Object> args = new HashMap<>();
        args.put("itemId", itemId);
        args.put("amount", amount);
        args.put("userId", userId);
        args.put("promoId", promoId);
        Message message = new Message(topicName, "increase",
                JSON.toJSON(bodyMap).toString().getBytes(StandardCharsets.UTF_8));
        TransactionSendResult sendResult = null;
        try {
            //由这个方法事务状态下发送消息,它会调用init()方法中executeLocalTransaction()这个方法
             sendResult = transactionMQProducer.sendMessageInTransaction(message, args);
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        if (sendResult.getLocalTransactionState()==LocalTransactionState.ROLLBACK_MESSAGE) {
            return false;
        }else if (sendResult.getLocalTransactionState()==LocalTransactionState.COMMIT_MESSAGE){
        return true;
        }else {
            return false;
        }
    }
}
