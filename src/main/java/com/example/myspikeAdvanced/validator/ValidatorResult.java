package com.example.myspikeAdvanced.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName ValidatorResult
 * @create 2021-08-05 12:42
 * @description
 */
@Data
public class ValidatorResult {

    /**
     * 校验结果是否有错
     **/
    private boolean hasErrors = false;
    /**
     * 存放错误信息的map
     **/
    private Map<String,String> errorMsgMap = new HashMap<>();

    /**
     * 实现通用的通过格式化字符串信息获取错误结果的msg方法
     * @Date 12:45 2021/8/5
     * @return  java.lang.String
     **/
    public String getErrMsg(){
       return StringUtils.join(errorMsgMap.values().toArray(),",");
    }
}
