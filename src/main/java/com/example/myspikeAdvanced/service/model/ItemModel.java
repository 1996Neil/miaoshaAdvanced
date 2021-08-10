package com.example.myspikeAdvanced.service.model;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName ItemModel
 * @create 2021-08-05 16:02
 * @description 领域模型,在开发中往往是先设计除领域模型再设计数据库模型
 */
@Data
public class ItemModel {

    /**
     * 商品id
     **/
    private Integer id;
    /**
     * 商品名称
     **/
    @NotBlank(message = "商品名称不能为空")
    private String title;
    /**
     * 商品价格  这里用BigDecimal的原因是
     * double在前端会有一个精度问题,可能一个1.9的对象会变成1.9999,对于我们来说是没有必要的
     **/
    @NotNull(message = "商品价格不能为空")
    @Min(value = 0,message = "商品价格不能为0")
    private BigDecimal price;
    /**
     * 商品库存
     **/
    @NotNull(message = "商品库存不能为空")
    private Integer stock;
    /**
     * 商品描述
     **/
    @NotBlank(message = "商品描述不能为空")
    private String description;
    /**
     * 商品销量
     **/
    private Integer sales;
    /**
     * 商品图片
     **/
    @NotBlank(message = "商品图片不能为空")
    private String imgUrl;
    /**
     * 使用聚合模型,如果promoModel不为空,说明其还拥有未结束的秒杀活动
     **/
    private PromoModel promoModel;
}
