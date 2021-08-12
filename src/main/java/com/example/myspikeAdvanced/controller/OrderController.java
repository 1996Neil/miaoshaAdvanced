package com.example.myspikeAdvanced.controller;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.response.CommonResultType;
import com.example.myspikeAdvanced.service.OrderService;
import com.example.myspikeAdvanced.service.model.OrderModel;
import com.example.myspikeAdvanced.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
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
        //判断用户登录
        Boolean isLogin = (Boolean) this.httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (isLogin==null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //从session中拿到用户信息
        UserModel loginUser = (UserModel) this.httpServletRequest.getSession().getAttribute("LOGIN_USER");
        //创建订单
        OrderModel orderModel = orderService.createOrder(loginUser.getId(), itemId, amount,promoId);
        return CommonResultType.create(orderModel);
    }
}
