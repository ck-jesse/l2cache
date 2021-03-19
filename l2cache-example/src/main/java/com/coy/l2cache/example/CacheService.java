package com.coy.l2cache.example;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存 接口
 * 基于业务维度标准化缓存操作
 *
 * @param <K> 表示相关的业务要素，可以是单个字段，也可以是一个对象。由开发人员在实现类中自己定义。
 * @param <R> 表示返回的缓存数据
 * @author chenck
 * @date 2020/8/26 12:36
 */
public interface CacheService<K, R> {

    /**
     * 获取缓存名字
     */
    String getCacheName();

    /**
     * 构建缓存key
     */
    default String buildCacheKey(K key) {
        return null;
    }

    /**
     * 获取缓存
     */
    R get(K key);

    /**
     * 获取或加载缓存，若缓存不存在，则从加载并设置到缓存，并返回
     */
    R getOrLoad(K key);

    /**
     * 设置指定key的缓存项
     */
    R put(K key, R value);

    /**
     * 仅当之前没有存储指定key的value时，才存储由指定key映射的指定value
     */
    default boolean putIfAbsent(K key, R value) {
        return false;
    }

    /**
     * 重新加载缓存（存在则替换，不存在则设置）
     */
    R reload(K key);

    /**
     * 淘汰缓存
     */
    void evict(K key);

    /**
     * 判断key是否存在
     */
    boolean isExists(K key);

    /**
     * 在redis中批量Key查询结果
     * @param keyMap 查询参数
     * @return 返回结果 Map<入参,出参>
     */
    default Map<String,R> batchGet(Map<String,K> keyMap) {
        return new HashMap<>();
    }
}
