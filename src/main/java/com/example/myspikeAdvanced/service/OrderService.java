package com.example.myspikeAdvanced.service;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.service.model.OrderModel;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName OrderService
 * @create 2021-08-05 23:46
 * @description
 */
public interface OrderService {

    /**
     * 创建订单
     * 目前使用 1.通过前端url上传过来秒杀活动id,然后下单接口校验对应id是否属于对应商品且活动已开始
     * 2.直接在下单接口内判断对应的商品是否存在秒杀活动,若存在进行中的则以秒杀价格下单
     * @Date 23:48 2021/8/5
     * @param userId 用户id
     * @param itemId 哪一个商品
     * @param amount 商品数量
     * @return  com.example.myspikeAdvanced.service.model.OrderModel
     **/
    OrderModel createOrder(Integer userId,Integer itemId,Integer amount,Integer promoId) throws BusinessException;
}
