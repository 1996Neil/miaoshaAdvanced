package com.example.myspikeAdvanced.error;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName BusinessException
 * @create 2021-08-04 12:37
 * @description  包装器业务异常类实现
 */
public class BusinessException extends Exception implements CommonError{

    private CommonError commonError;
    /**
     * 直接接收EmBusinessError的传参用于构造业务异常
     * @Date 12:40 2021/8/4
     * @param commonError EmBusinessError的参数
     **/
    public BusinessException(CommonError commonError){
        super();
        this.commonError = commonError;
    }

    /**
     * 接收自定义error的方法用于构造业务异常
     * @Date 12:40 2021/8/4
     * @param commonError EmBusinessError的参数
     * @param errMsg 自定义错误信息
     **/
    public BusinessException(CommonError commonError,String errMsg){
        super();
        this.commonError = commonError;
        this.commonError.setErrMsg(errMsg);
    }
    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.commonError.getErrMsg();
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.commonError.setErrMsg(errMsg);
        return this;
    }
}
