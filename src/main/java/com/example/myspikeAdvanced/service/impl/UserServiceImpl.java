package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mbg.dao.dataObject.UserDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.UserPasswordDO;
import com.example.myspikeAdvanced.mbg.mapper.UserDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.UserPasswordDOMapper;
import com.example.myspikeAdvanced.service.UserService;
import com.example.myspikeAdvanced.service.model.UserModel;
import com.example.myspikeAdvanced.validator.ValidatorImpl;
import com.example.myspikeAdvanced.validator.ValidatorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName UserServiceImpl
 * @create 2021-08-03 22:26
 * @description
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 通过用户id得到用户信息
     *
     * @param userId
     * @return com.example.myspikeAdvanced.service.model.UserModel
     * @Date 22:45 2021/8/3
     **/
    @Override
    public UserModel getUserById(Integer userId) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(userId);
        if (userDO == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userId);
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);
        return userModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        //用StringUtils.isEmpty是因为可以判断字符串是否是空串或者是null
        //if (StringUtils.isEmpty(userModel.getName())
        //        || userModel.getGender() == null
        //        || userModel.getAge() == null
        //        || StringUtils.isEmpty(userModel.getTelephone())) {
        //    throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        //}
        //if (StringUtils.isEmpty(userModel.getEncryptPassword())) {
        //    throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"密码加密错误");
        //}

        //使用ValidatorResult校验
        ValidatorResult result = validator.validate(userModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }
        UserDO userDO;
        //实现model->dataObject方法,因为前面我们给前端是userModel,所以传回来的时候要给它再转回去
        userDO = convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        }catch (DuplicateKeyException ex){
           throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户已注册");
        }
        userModel.setId(userDO.getId());
        UserPasswordDO userPasswordDO;
        userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
        return;
    }

    @Override
    public UserModel validateLogin(String telephone, String EncryptPassword) throws BusinessException {
        //通过用户手机获取用户信息
        UserDO userDO = userDOMapper.selectByTelephone(telephone);
        if (userDO==null) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO,userPasswordDO);
        //比对用户信息内加密的密码是否和传输进来的密码一致
        if (StringUtils.equals(EncryptPassword,userModel.getEncryptPassword())) {
            return userModel;
        }else {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

    }

    @Override
    public UserModel getUserByIdInCache(Integer userId) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_"+userId);
        if (userModel==null) {
            userModel = this.getUserById(userId);
            redisTemplate.opsForValue().set("user_validate_"+userId,userModel);
            redisTemplate.expire("user_validate_"+userId,10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    private UserPasswordDO convertPasswordFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncryptPassword(userModel.getEncryptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (userDO == null) {
            return null;
        }
        UserModel userModel = new UserModel();
        //把userdo的属性赋值到usermodel中
        BeanUtils.copyProperties(userDO, userModel);
        if (userPasswordDO != null) {
            userModel.setEncryptPassword(userPasswordDO.getEncryptPassword());
        }
        return userModel;
    }
}
