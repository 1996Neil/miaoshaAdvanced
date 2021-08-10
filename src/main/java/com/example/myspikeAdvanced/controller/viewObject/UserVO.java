package com.example.myspikeAdvanced.controller.viewObject;

import lombok.Data;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName UserVO
 * @create 2021-08-03 22:50
 * @description  一些无关字段没有必要,比如登录方式,也不可以返回给前端 比如密码
 */
@Data
public class UserVO {
    private Integer id;

    private String name;

    /**
     * 1代表男性,2代表女性
     *
     * @mbg.generated
     * gender
     */
    private Byte gender;

    private Integer age;

    private String telephone;
}
