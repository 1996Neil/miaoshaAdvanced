package com.example.myspikeAdvanced.service;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.service.model.UserModel;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName UserService
 * @create 2021-08-03 21:50
 * @description
 */
public interface UserService {
    /**
     * 获取用户
     * @Date 21:12 2021/8/4
     * @param userId
     * @return  com.example.myspikeAdvanced.service.model.UserModel
     **/
    public UserModel getUserById(Integer userId);

    /**
     * 用户注册流程
     * @Date 21:12 2021/8/4
     * @param userModel
     * @return  void
     * @throws BusinessException
     **/
    void register(UserModel userModel) throws BusinessException;

    /**
     * 校验用户登录信息是否正确
     * @Date 11:34 2021/8/5
     * @param telephone 用户手机号
     * @param EncryptPassword 用户加密后的密码
     * @return  void
     * @throws BusinessException
     **/
    UserModel validateLogin(String telephone, String EncryptPassword) throws BusinessException;

    /**
     * 通过缓存获取用户信息
     * @Date 11:54 2021/8/16
     * @param userId
     * @return  com.example.myspikeAdvanced.service.model.UserModel
     **/
    UserModel getUserByIdInCache(Integer userId);
}
