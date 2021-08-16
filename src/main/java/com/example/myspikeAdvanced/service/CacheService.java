package com.example.myspikeAdvanced.service;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName CacheService
 * @create 2021-08-13 16:19
 * @description 封装本地缓存操作类
 */
public interface CacheService {

    /**
     * 存方法
     * @Date 16:21 2021/8/13
     * @param key
     * @param value
     * @return  void
     **/
    void setCommonCache(String key,Object value);

    /**
     * 取方法
     * @Date 16:21 2021/8/13
     * @param key
     * @return  java.lang.Object
     **/
    Object getFromCommonCache(String key);
}
