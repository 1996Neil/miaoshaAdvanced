package com.example.myspikeAdvanced.error;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName CommonError
 * @create 2021-08-04 12:25
 * @description
 */
public interface CommonError {
    /**
     * 得到错误码
     * @Date 12:27 2021/8/4
     * @return  int
     **/
    public int getErrCode();
    /**
     * 得到错误信息
     * @Date 12:27 2021/8/4
     * @return  java.lang.String
     **/
    public String getErrMsg();
    /**
     * 设置错误信息
     * @Date 12:35 2021/8/4
     * @param errMsg 自定义错误信息
     * @return  com.example.myspikeAdvanced.error.CommonError
     **/
    public CommonError setErrMsg(String errMsg);
}
