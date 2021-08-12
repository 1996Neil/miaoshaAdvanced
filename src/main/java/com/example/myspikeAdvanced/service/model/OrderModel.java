package com.example.myspikeAdvanced.service.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName OrderModel
 * @create 2021-08-05 23:33
 * @description
 */
@Data
public class OrderModel implements Serializable {
    /**
     * 交易流水号
     **/
    private String id;
    /**
     * 用户id
     **/
    private Integer userId;
    /**
     * 商品id
     **/
    private Integer itemId;
    /**
     * 商品单价,若promoId非空,则表示是以秒杀商品方式下单
     **/
    private BigDecimal itemPrice;
    /**
     * 购买数量
     **/
    private Integer amount;
    /**
     * 订单金额,若promoId非空,则表示是以秒杀商品方式下单
     **/
    private BigDecimal orderPrice;
    /**
     * 若非空,则表示是以秒杀商品方式下单,秒杀商品id
     **/
    private Integer promoId;

}
