package com.example.myspikeAdvanced.controller;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mq.MqProducer;
import com.example.myspikeAdvanced.response.CommonResultType;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.OrderService;
import com.example.myspikeAdvanced.service.model.OrderModel;
import com.example.myspikeAdvanced.service.model.UserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName OrderController
 * @create 2021-08-06 12:04
 * @description
 */
@RestController
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MqProducer mqProducer;
    @Autowired
    private ItemService itemService;

    /**
     * 创建订单
     * @Date 12:15 2021/8/6
     * @param itemId
     * @param amount 商品数量
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     * @throws BusinessException
     **/
    @PostMapping(value = "/createOrder", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId",required = false) Integer promoId) throws BusinessException {

        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"token为空");
        }
        UserModel loginUser = (UserModel) redisTemplate.opsForValue().get(token);
        if (loginUser==null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //判断库存是否已售罄,若对应的售罄key存在,则直接返回下单失败
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //加入库存流水init状态
        String stockLogId = itemService.initStockLog(itemId, amount);
        //再去完成对应的下单事务型消息机制
        if (!mqProducer.transactionAsyncReduceStock(loginUser.getId(), itemId, amount, promoId,stockLogId)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"下单失败");
        }
        return CommonResultType.create(null);
    }
}
