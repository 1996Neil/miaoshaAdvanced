package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mbg.dao.dataObject.OrderDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.SequenceDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.StockLogDO;
import com.example.myspikeAdvanced.mbg.mapper.OrderDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.SequenceDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.StockLogDOMapper;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.OrderService;
import com.example.myspikeAdvanced.service.UserService;
import com.example.myspikeAdvanced.service.model.ItemModel;
import com.example.myspikeAdvanced.service.model.OrderModel;
import com.example.myspikeAdvanced.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName OrderServiceImpl
 * @create 2021-08-05 23:48
 * @description
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDOMapper orderDOMapper;
    @Autowired
    private SequenceDOMapper sequenceDOMapper;
    @Autowired
    private StockLogDOMapper stockLogDOMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount, Integer promoId,String stockLogId) throws BusinessException {
        //1.??????????????????,???????????????????????????,??????????????????,????????????????????????
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "???????????????");
        }
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "???????????????");
        }
        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "??????????????????");
        }
        if (promoId != null) {
            //(1)??????????????????????????????????????????
            if (promoId.intValue() != itemModel.getPromoModel().getId()) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "?????????????????????");
                //2?????????????????????????????????
            } else if (itemModel.getPromoModel().getStatus().intValue() != 2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "??????????????????");
            }
        }
        //2.???????????????
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //????????????
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        //????????????id
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        //?????????????????????,?????????
        orderModel.setId(generateOrderNo());
        OrderDO orderDO = this.covertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);
        //?????????????????????
        itemService.increaseSales(itemId, amount);

        //?????????????????????????????????
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO==null) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
        //????????????
        return orderModel;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateOrderNo() {
        //????????????16???
        StringBuilder stringBuilder = new StringBuilder();
        //????????????????????????,?????????
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        //???????????????????????????
        int sequence = 0;
        //????????????sequence??????
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        //???sequence????????????????????????,???????????????????????????????????????????????????
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        //???????????????????????????????????????,???????????????0
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        //000001
        stringBuilder.append(sequenceStr);
        //??????????????????????????????
        stringBuilder.append("00");
        return stringBuilder.toString();
    }

    private OrderDO covertFromOrderModel(OrderModel orderModel) {
        if (orderModel == null) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
