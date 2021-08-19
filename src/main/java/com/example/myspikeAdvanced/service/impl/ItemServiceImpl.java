package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mbg.dao.dataObject.ItemDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.ItemStockDO;
import com.example.myspikeAdvanced.mbg.mapper.ItemDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.ItemStockDOMapper;
import com.example.myspikeAdvanced.mq.MqProducer;
import com.example.myspikeAdvanced.service.ItemService;
import com.example.myspikeAdvanced.service.PromoService;
import com.example.myspikeAdvanced.service.model.ItemModel;
import com.example.myspikeAdvanced.service.model.PromoModel;
import com.example.myspikeAdvanced.validator.ValidatorImpl;
import com.example.myspikeAdvanced.validator.ValidatorResult;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName ItemServiceImpl
 * @create 2021-08-05 16:30
 * @description
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private ItemDOMapper itemDOMapper;

    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    @Autowired
    private PromoService promoService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MqProducer mqProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //校验入参
        ValidatorResult result = validator.validate(itemModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }
        //转化itemModel->dataObject
        ItemDO itemDO = this.convertFromItemModel(itemModel);
        //写入数据库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = this.convertStockFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    private ItemDO convertFromItemModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    private ItemStockDO convertStockFromItemModel(ItemModel itemModel) {
        if (itemModel == null) {
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setStock(itemModel.getStock());
        itemStockDO.setItemId(itemModel.getId());
        return itemStockDO;
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOS = itemDOMapper.listItem();
        //将每一个itemDO映射成itemModel,这个方法可以好好学一下
        List<ItemModel> itemModelList = itemDOS.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = covertFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer itemId) {
        if (itemId == null) {
            return null;
        }
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(itemId);
        if (itemDO == null) {
            return null;
        }
        //操作获得库存方法
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemId);
        //将dataObject转化成itemModel
        ItemModel itemModel = covertFromDataObject(itemDO, itemStockDO);
        //获取活动商品信息
        PromoModel promoModel = promoService.getPromoById(itemId);
        if (promoModel!=null && promoModel.getStatus().intValue()!=3) {
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        Long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue() * -1);
        if (result>=0) {
            return true;
        }else {
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_validate_" + id);
        if (itemModel==null) {
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_" + id,itemModel);
            redisTemplate.expire("item_validate_" + id,10, TimeUnit.MINUTES);
        }
        return itemModel;
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount.intValue()*-1);
        return mqResult;
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        //不考虑redis失败
        return true;
    }

    private ItemModel covertFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }
}
