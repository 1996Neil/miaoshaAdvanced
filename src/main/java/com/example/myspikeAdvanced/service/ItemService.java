package com.example.myspikeAdvanced.service;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.service.model.ItemModel;

import java.util.List;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName itemService
 * @create 2021-08-05 16:26
 * @description
 */
public interface ItemService {

    /**
     * 创建商品
     * @param itemModel
     * @return
     * @throws BusinessException
     */
    ItemModel createItem(ItemModel itemModel) throws BusinessException;

    /**
     * 商品列表浏览
     * @Date 16:29 2021/8/5
     * @param
     * @return  java.util.List<com.example.myspikeAdvanced.service.model.ItemModel>
     **/
    List<ItemModel> listItem();

    /**
     * 商品详情浏览
     * @Date 16:29 2021/8/5
     * @param itemId
     * @return  com.example.myspikeAdvanced.service.model.ItemModel
     **/
    ItemModel getItemById(Integer itemId);
    /**
     * 扣减商品库存
     * @Date 0:00 2021/8/6
     * @param itemId
     * @param amount 商品数量
     * @return  boolean
     * @throws BusinessException
     **/
    boolean decreaseStock(Integer itemId,Integer amount)throws BusinessException;

    void increaseSales(Integer itemId,Integer amount)throws BusinessException;
    /**
     * item及promo model缓存模型
     * @Date 11:48 2021/8/16
     * @param id
     * @return  com.example.myspikeAdvanced.service.model.ItemModel
     **/
    ItemModel getItemByIdInCache(Integer id);
    /**
     * 异步扣减库存
     * @Date 16:45 2021/8/19
     * @param itemId
     * @param amount
     * @return  boolean
     **/
    boolean asyncDecreaseStock(Integer itemId,Integer amount);
    /**
     * 库存回补
     * @Date 16:49 2021/8/19
     * @param itemId
     * @param amount
     * @return  boolean
     **/
    boolean increaseStock(Integer itemId,Integer amount);
    /**
     * 初始化对应的流水
     * @Date 19:54 2021/8/19
     * @param itemId
     * @param amount
     * @return  void
     **/
    String initStockLog(Integer itemId,Integer amount);
}
