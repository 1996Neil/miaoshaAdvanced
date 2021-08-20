package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mbg.dao.dataObject.PromoDO;
import com.example.myspikeAdvanced.mbg.mapper.PromoDOMapper;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.PromoService;
import com.example.myspikeAdvanced.service.UserService;
import com.example.myspikeAdvanced.service.model.ItemModel;
import com.example.myspikeAdvanced.service.model.PromoModel;
import com.example.myspikeAdvanced.service.model.UserModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName PromoServiceImpl
 * @create 2021-08-06 13:38
 * @description
 */
@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;

    @Override
    public PromoModel getPromoById(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByByItemId(itemId);
        //dataObject->model
        PromoModel promoModel = covertFromDataObject(promoDO);
        if (promoModel == null) {
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        //如果秒杀开始时间比现在晚,那就是还没开始
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
            //如果秒杀结束时间比现在早,那就是已经结束
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        //通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0) {
            return;
        }
        //将秒杀库存同步到redis中
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
        //秒杀大闸,设置令牌数在库存5倍
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue()*5);
    }

    @Override
    public String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId) {
        //判断库存是否已售罄,若对应的售罄key存在,则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            return null;
        }
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        //dataObject->model
        PromoModel promoModel = covertFromDataObject(promoDO);
        if (promoModel == null) {
            return null;
        }
        //判断当前时间是否秒杀活动即将开始或正在进行
        //如果秒杀开始时间比现在晚,那就是还没开始
        if (promoModel.getStartTime().isAfterNow()) {
            promoModel.setStatus(1);
            //如果秒杀结束时间比现在早,那就是已经结束
        } else if (promoModel.getEndTime().isBeforeNow()) {
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }
        //1.判断商品信息是否存在
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (itemModel == null) {
            return null;
        }
        //判断用户是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (userModel == null) {
            return null;
        }
        //判断活动是否正在进行
        if (promoModel.getStatus().intValue() != 2) {
            return null;
        }
        //获取秒杀大闸的数量
        long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId,-1);
        if (result<0) {
            return null;
        }
        //生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set("promo_token_" + promoId + "_userId_" + userId + "_itemId_" + itemId, token);
        redisTemplate.expire("promo_token_" + promoId + "_userId_" + userId + "_itemId_" + itemId, 5, TimeUnit.MINUTES);
        return token;
    }

    private PromoModel covertFromDataObject(PromoDO promoDO) {
        if (promoDO == null) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promoDO.getStartDate()));
        promoModel.setEndTime(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
