package com.example.myspikeAdvanced.service;

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
}
