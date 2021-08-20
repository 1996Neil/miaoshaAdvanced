package com.example.myspikeAdvanced.service;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.service.model.PromoModel;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName PromoService
 * @create 2021-08-06 13:35
 * @description
 */
public interface PromoService {
    /**
     * 获取秒杀商品
     * @Date 13:37 2021/8/6
     * @param itemId 商品id
     * @return  com.example.myspikeAdvanced.service.model.PromoModel
     **/
    PromoModel getPromoById(Integer itemId);
    /**
     * 活动发布
     * @Date 14:39 2021/8/16
     * @param promoId
     * @return  void
     **/
    void publishPromo(Integer promoId);
    /**
     * 生成秒杀用的令牌
     * @Date 12:47 2021/8/20
     * @param promoId 秒杀商品id
     * @param itemId
     * @param userId
     * @return  java.lang.String
     **/
    String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId);
}
