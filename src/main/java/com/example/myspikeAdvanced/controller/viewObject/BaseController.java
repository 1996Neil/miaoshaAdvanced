package com.example.myspikeAdvanced.controller.viewObject;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.response.CommonResultType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName BaseController
 * @create 2021-08-04 13:43
 * @description
 */
@RestController
public class BaseController {
    /**
     * 声明ajax的content类型
     **/
    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

    /**
     * 定义ExceptionHandler解决未被controller层吸收的exception
     * 设计思想:controller异常是业务处理的最后一道关口,如果处理掉exception就会给前端一个很好的钩子
     * ResponseStatus(HttpStatus.OK) 对于我们定义的BusinessException这是我们业务逻辑
     * 上的错误,而不是服务端不能处理的错误,所以我们要定义即便抛出Exception,我们也要捕获并且返回200,也就是请求成功
     * <p>
     * RestController自带ResponseBody 成功处理
     *
     * @param httpServletRequest
     * @param ex                 我们定义的异常信息
     * @return java.lang.Object
     * @Date 12:58 2021/8/4
     **/
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Object handlerException(HttpServletRequest httpServletRequest, Exception ex) {
        //1.把异常传入通用处理对象返回 状态"fail",data (10001,"用户不存在")
        //CommonResultType commonResultType = new CommonResultType();
        //commonResultType.setStatus("fail");
        //commonResultType.setData(ex);
        Map<String, Object> responseData = new HashMap<>();
        if (ex instanceof BusinessException) {
            //2.因为返回的是Exception的json序列化,所以我们要强转成BusinessException来进行处理
            BusinessException businessException = (BusinessException) ex;
            responseData.put("errCode", businessException.getErrCode());
            responseData.put("errMsg", businessException.getErrMsg());
        } else {
            //这里为什么不直接返回UNKNOWN_ERROR呢,因为ResponseBody会自动序列化,达不到我们需要的键值对的需求
            responseData.put("errCode", EmBusinessError.UNKNOWN_ERROR.getErrCode());
            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrMsg());
        }
        return CommonResultType.create(responseData, "fail");
    }
}
