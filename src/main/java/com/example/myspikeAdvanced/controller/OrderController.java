package com.example.myspikeAdvanced.controller;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mq.MqProducer;
import com.example.myspikeAdvanced.response.CommonResultType;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.OrderService;
import com.example.myspikeAdvanced.service.PromoService;
import com.example.myspikeAdvanced.service.model.UserModel;
import com.example.myspikeAdvanced.util.CodeUtil;
import com.google.common.util.concurrent.RateLimiter;
import com.sun.deploy.net.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

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

    private ExecutorService executorService;

    private RateLimiter orderRateLimiter;
    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(20);
        //一秒钟支持300个线程
        orderRateLimiter = RateLimiter.create(300);
    }

    @RequestMapping(value = "/generateVerifyCode")
    public void generateVerifyCode(HttpServletResponse response) throws BusinessException, IOException {
        //根据token获取用户信息
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户还未登录,不能生成验证码");
        }
        //获取用户的登录信息
        UserModel loginUser = (UserModel) redisTemplate.opsForValue().get(token);
        if (loginUser == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //拿到验证码,返回给前端,之后把验证码存到redis
        Map<String,Object> map = CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("verify_code_"+loginUser.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+loginUser.getId(),10,TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());
        System.out.println("验证码的值为："+map.get("code"));
    }

    /**
     * 获取秒杀令牌
     *
     * @param itemId
     * @param promoId
     * @return com.example.myspikeAdvanced.response.CommonResultType
     * @Date 13:25 2021/8/20
     **/
    @PostMapping(value = "/generateToken", consumes = {CONTENT_TYPE_FORMED})
    public CommonResultType generateToken(@RequestParam("itemId") Integer itemId,
                                          @RequestParam(value = "promoId") Integer promoId,
                                          @RequestParam(value = "verifyCode") String verifyCode) throws BusinessException {
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
        //校验验证码
        String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_" + loginUser.getId());
        if (redisVerifyCode==null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法");
        }
        if (!redisVerifyCode.equalsIgnoreCase(verifyCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"验证码错误");
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
                                        @RequestParam(value = "promoToken") String promoToken) throws BusinessException {
        //如果没有拿到对应的令牌
        if (!orderRateLimiter.tryAcquire()) {
            throw new BusinessException(EmBusinessError.RATE_LIMIT);
        }
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
        //同步调用线程池的submit方法
        //拥塞窗口为2的等待队列,用来队列化泄洪
        Future<Object> future = executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                //加入库存流水init状态
                String stockLogId = itemService.initStockLog(itemId, amount);
                //再去完成对应的下单事务型消息机制
                if (!mqProducer.transactionAsyncReduceStock(loginUser.getId(), itemId, amount, promoId, stockLogId)) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
                }
                return null;
            }
        });
        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }

        return CommonResultType.create(null);
    }
}
