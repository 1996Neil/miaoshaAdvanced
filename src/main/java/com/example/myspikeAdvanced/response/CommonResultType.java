package com.example.myspikeAdvanced.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName CommonResultType
 * @create 2021-08-04 10:56
 * @description
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResultType {
    /**
     *表明对应请求的返回处理结果"success"或"fail"
     **/
    private String status;
    /**
     *若status=success,则data内返回前端锁需要的json字符串
     * 若status=fail,则data返回通用的错误码格式
     **/
    private Object data;

    /**
     * 创建一个通用的成功返回方法
     * @Date 11:05 2021/8/4
     * @param data
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     **/
    public static CommonResultType create(Object data){
        return new CommonResultType("success",data);
    }

    /**
     * 创建一个通用的失败返回方法
     * @Date 11:05 2021/8/4
     * @param data
     * @param status
     * @return  com.example.myspikeAdvanced.response.CommonResultType
     **/
    public static CommonResultType create(Object data,String status){
        return new CommonResultType(status,data);
    }
}
