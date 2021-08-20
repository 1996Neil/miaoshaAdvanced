package com.example.myspikeAdvanced.mq;

import com.alibaba.fastjson.JSON;
import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.mbg.dao.dataObject.StockLogDO;
import com.example.myspikeAdvanced.mbg.mapper.StockLogDOMapper;
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
    @Autowired
    private StockLogDOMapper stockLogDOMapper;

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
                String stockLogId = (String) args.get("stockLogId");
                //真正要做的事,创建订单
                try {
                    orderService.createOrder(userId, itemId, amount, promoId,stockLogId);
                } catch (BusinessException e) {
                    //如果有异常,那么就回滚
                    e.printStackTrace();
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }
            /**
             * 如果executeLocalTransaction长时间不返回的话,mq就会启动checkLocalTransaction,由快到慢,7天后失效
             * @Date 20:18 2021/8/19
             * @param msg
             * @return  org.apache.rocketmq.client.producer.LocalTransactionState
             **/
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减库存成功,来判断要返回COMMIT_MESSAGE,ROLLBACK_MESSAGE,还是UNKNOW
                String jsonString = new String(msg.getBody());
                Map<String,Object> map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLodId");
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if (stockLogDO==null) {
                    return LocalTransactionState.UNKNOW;
                }else if (stockLogDO.getStatus().intValue()==2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                } else if (stockLogDO.getStatus().intValue()==1) {
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
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
    public boolean transactionAsyncReduceStock(Integer userId, Integer itemId, Integer amount,
                                               Integer promoId,String stockLogId) {
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId", itemId);
        bodyMap.put("amount", amount);
        bodyMap.put("stockLogId", stockLogId);
        Map<String, Object> args = new HashMap<>();
        args.put("itemId", itemId);
        args.put("amount", amount);
        args.put("userId", userId);
        args.put("promoId", promoId);
        args.put("stockLogId", stockLogId);
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
