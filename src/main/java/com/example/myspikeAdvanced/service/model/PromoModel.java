package com.example.myspikeAdvanced.service.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName PromoModel
 * @create 2021-08-06 13:24
 * @description
 */
@Data
public class PromoModel implements Serializable {

    private Integer id;
    /**
     * 秒杀活动状态,1表示还未开始,2表示进行中,3表示已结束
     **/
    private Integer status;
    /**
     * 秒杀名称
     **/
    private String promoName;
    /**
     * 秒杀的开始时间
     **/
    private DateTime startTime;
    /**
     * 秒杀的结束时间
     **/
    private DateTime endTime;
    /**
     * 秒杀的商品名称
     **/
    private Integer itemId;
    /**
     * 秒杀的活动价格
     **/
    private BigDecimal promoItemPrice;
}
