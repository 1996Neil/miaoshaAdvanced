package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.error.BusinessException;
import com.example.myspikeAdvanced.error.EmBusinessError;
import com.example.myspikeAdvanced.mbg.dao.dataObject.ItemDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.ItemStockDO;
import com.example.myspikeAdvanced.mbg.dao.dataObject.StockLogDO;
import com.example.myspikeAdvanced.mbg.mapper.ItemDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.ItemStockDOMapper;
import com.example.myspikeAdvanced.mbg.mapper.StockLogDOMapper;
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
import java.util.UUID;
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
    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        //????????????
        ValidatorResult result = validator.validate(itemModel);
        if (result.isHasErrors()) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }
        //??????itemModel->dataObject
        ItemDO itemDO = this.convertFromItemModel(itemModel);
        //???????????????
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO = this.convertStockFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        //???????????????????????????
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
        //????????????itemDO?????????itemModel,?????????????????????????????????
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
        //????????????????????????
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemId);
        //???dataObject?????????itemModel
        ItemModel itemModel = covertFromDataObject(itemDO, itemStockDO);
        //????????????????????????
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
        if (result>0) {
            return true;
        }else if (result==0){
            //???????????????????????????
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");
            return true;
        } else{
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
        boolean mqResult = mqProducer.asyncReduceStock(itemId, amount.intValue());
        return mqResult;
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount.intValue());
        //?????????redis??????
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO =new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-",""));
        stockLogDO.setStatus(1);

        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }

    private ItemModel covertFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }
}
