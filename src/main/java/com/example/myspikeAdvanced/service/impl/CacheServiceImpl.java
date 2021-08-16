package com.example.myspikeAdvanced.service.impl;

import com.example.myspikeAdvanced.service.CacheService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName CacheServiceImpl
 * @create 2021-08-13 16:21
 * @description
 */
@Service
public class CacheServiceImpl implements CacheService {

    private Cache<String,Object> commonCache = null;

    /**
     * 在spring bean执行前优先执行
     * @Date 16:29 2021/8/13
     * @return  void
     **/
    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存初始容量为10
                .initialCapacity(10)
                //设置缓存最大可以存储100个key,超过100个之后会按照lru策略移除缓存项
                .maximumSize(100)
                //设置写缓存后多少秒过期
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }
    @Override
    public void setCommonCache(String key, Object value) {
    commonCache.put(key,value);
    }

    @Override
    public Object getFromCommonCache(String key) {
        //如果不存在就返回null
        return commonCache.getIfPresent(key);
    }
}
