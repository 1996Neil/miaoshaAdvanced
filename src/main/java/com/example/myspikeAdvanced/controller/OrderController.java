package com.example.myspikeAdvanced.controller;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mq.MqProducer;
import com.example.myspikeAdvanced.response.CommonResultType;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.OrderService;
import com.example.myspikeAdvanced.service.PromoService;
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
    @Autowired
    private PromoService promoService;

    /**
     * 获取秒杀令牌
     * @Date 13:25 2021/8/20
     * @param itemId
     * @param promoId
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     **/
    @PostMapping(value = "/generateToken", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType generateToken(@RequestParam("itemId") Integer itemId,
                                          @RequestParam(value = "promoId") Integer promoId) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "token为空");
        }
        //获取用户的登录信息
        UserModel loginUser = (UserModel) redisTemplate.opsForValue().get(token);
        if (loginUser == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, loginUser.getId());
        if (promoToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失效");
        }
        //返回对应的结果
        return CommonResultType.create(promoToken);
    }

    /**
     * 创建订单
     *
     * @param itemId
     * @param amount 商品数量
     * @return com.example.myspikeAdvanced.response.CommonResultType
     * @throws BusinessException
     * @Date 12:15 2021/8/6
     **/
    @PostMapping(value = "/createOrder", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType createOrder(@RequestParam("itemId") Integer itemId,
                                        @RequestParam("amount") Integer amount,
                                        @RequestParam(value = "promoId", required = false) Integer promoId,
                                        @RequestParam(value = "promoToken", required = false) String promoToken) throws BusinessException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "token为空");
        }
        //获取用户的登录信息
        UserModel loginUser = (UserModel) redisTemplate.opsForValue().get(token);
        if (loginUser == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //校验秒杀令牌是否正确
        if (promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userId_" + loginUser.getId() + "_itemId_" + itemId);
            if (inRedisPromoToken == null) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
            if (!StringUtils.equals(promoToken, inRedisPromoToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        //加入库存流水init状态
        String stockLogId = itemService.initStockLog(itemId, amount);
        //再去完成对应的下单事务型消息机制
        if (!mqProducer.transactionAsyncReduceStock(loginUser.getId(), itemId, amount, promoId, stockLogId)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        }
        return CommonResultType.create(null);
    }
}
