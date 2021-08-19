package com.example.myspikeAdvanced.error;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName EmBussinessError
 * @create 2021-08-04 12:28
 * @description
 */
public enum EmBusinessError implements CommonError{
    /**
     *通用错误类型10001
     **/
    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),
    /**
     *通用错误类型10002
     **/
    UNKNOWN_ERROR(10002,"未知错误"),
    /**
     *20000开头为用户信息相关错误定义
     **/
    USER_NOT_EXIST(20001,"用户不存在"),
    USER_LOGIN_FAIL(20002,"用户手机号或密码不正确"),
    USER_NOT_LOGIN(20003,"用户还未登录"),
    //30000开头为交易信息错误定义
    STOCK_NOT_ENOUGH(30001,"库存不足"),
    MQ_SENT_FAILED(30002,"库存消息队列发送失败")
    ;

    private int errCode;
    private String errMsg;

    private EmBusinessError(int errCode, String errMsg) {
       this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
