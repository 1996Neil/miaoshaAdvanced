package com.example.myspikeAdvanced.controller.viewObject;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName ItemVO
 * @create 2021-08-05 17:39
 * @description
 */
@Data
public class ItemVO {

    /**
     * 商品id
     **/
    private Integer id;
    /**
     * 商品名称
     **/
    private String title;
    /**
     * 商品价格  这里用BigDecimal的原因是
     * double在前端会有一个精度问题,可能一个1.9的对象会变成1.9999,对于我们来说是没有必要的
     **/
    private BigDecimal price;
    /**
     * 商品库存
     **/
    private Integer stock;
    /**
     * 商品描述
     **/
    private String description;
    /**
     * 商品销量
     **/
    private Integer sales;
    /**
     * 商品图片
     **/
    private String imgUrl;
    /**
     * 记录商品是否在秒杀活动中,以及对应的状态.0表示没有秒杀活动 1表示活动还未开始,2表示正在活动中
     **/
    private Integer promoStatus;
    /**
     * 开始时间,用来做倒计时
     **/
    private String startDate;
    /**
     * 活动ID
     **/
    private Integer promoId;
    /**
     * 秒杀价格
     **/
    private BigDecimal promoPrice;
}
