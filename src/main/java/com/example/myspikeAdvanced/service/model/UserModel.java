package com.example.myspikeAdvanced.service.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName UserModel
 * @create 2021-08-03 22:31
 * @description 在实际业务中,dao层就是最底层的映射,一张表一个对象,而model类则是整合dao层的对象变成一个完整的对象
 * 方便返回前端
 * NotBlank 是用户名不能是null并且不能是空串
 */
@Data
public class UserModel implements Serializable {

    private Integer id;
    @NotBlank(message = "用户名不能为空")
    private String name;

    /**
     * 1代表男性,2代表女性
     *
     * @mbg.generated
     * gender
     */
    @NotNull(message = "性别不能不填写")
    private Byte gender;
    @NotNull(message = "年龄不能不填写")
    @Min(value = 0,message = "年龄必须大于0岁")
    @Max(value = 150,message = "年龄必须小于150岁")
    private Integer age;
    @NotBlank(message = "手机号不能为空")
    private String telephone;

    /**
     * 注册方式 by phone by wechat by alipay
     *
     * @mbg.generated
     * register_mode
     */
    private String registerMode;

    /**
     * 第三方账户id
     *
     * @mbg.generated
     * third_part_id
     */
    private String thirdPartId;

    /**
     * 加密密码
     *
     * @mbg.generated
     * encrpt_password
     */
    @NotBlank(message = "密码不能为空")
    private String encryptPassword;

}
