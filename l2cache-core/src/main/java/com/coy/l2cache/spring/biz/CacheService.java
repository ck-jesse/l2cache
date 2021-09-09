package com.coy.l2cache.spring.biz;

import com.coy.l2cache.Cache;
import com.coy.l2cache.exception.L2CacheException;

import java.util.List;
import java.util.Map;

/**
 * 业务维度的缓存接口
 * 作用：
 * 1）标准化业务系统中对缓存的增删改查操作，避免缓存操作与业务逻辑强耦合在一起。
 * 2）作为业务系统中的一个缓存层，对业务逻辑和l2cache组件而言，起到承上启下的作用，可简化业务开发，降低开发复杂度。
 * 分层调用链路：业务逻辑 -> CacheService -> l2cache
 * <p>
 * 注：该接口是从多个核心业务系统的实战开发中提炼而来，所以个人强烈建议业务系统接入l2cache组件时，基于该接口来开发。
 *
 * @param <K> 表示缓存key，可以是单个字段，也可以是一个自定义对象。特别注意：K为自定义对象时，需要重写equals()和hashcode()方法，目的：获取到缓存数据后，业务代码中可根据自定义DTO对象，找到其对应的缓存项。
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
     * 获取l2cache组件的Cache对象
     */
    default Cache getNativeL2cache() {
        // 默认直接抛出异常，也就是说强制开发人员在使用该方法时，必须实现该方法，否则直接抛异常
        throw new L2CacheException("未实现方法CacheService.getNativeL2cache()，请检查代码");
    }

    /**
     * 构建缓存key
     * 注：
     * 1.该方法可自定义拼接DTO中多个字段作为一个缓存key
     * 2.如果构建的缓存key是被l2cache的Cache对象所使用，则key中无需拼接CacheName，因为CacheName会在l2cache中自动构建好。
     * 3.如果构建的缓存key不是被l2cache的Cache对象所使用，则key中需要拼接CacheName，适用于直接通过 RedissonClient 等来操作缓存的场景。
     * <p>
     * 前提：key是Integer类型
     * 问题还原：
     * 1）先通过CacheService.getOrLoad(key)加载数据到本地缓存，此时，本地缓存中key的类型为Integer
     * 2）再通过CacheService.get(buildCacheKey(key))方法获取缓存数据，由于buildCacheKey(key)的将key转换为了String类型，所以根据这个String类型的key无法从本地缓存获取到缓存数据，会在本地缓存中再次缓存一个key为String类型的缓存，也就是本来是一个缓存，现在变为了2个缓存，一个key为Integer类型，一个key为String类型
     * 解决方案：CacheService.buildCacheKey()方法返回类型从String修改为Object即可
     *
     * @return 缓存key
     */
    default Object buildCacheKey(K key) {
        // 如果key为基础数据类型，则直接返回 key，实现类中无需实现该方法，简化开发
        if (key instanceof CharSequence || key instanceof Number) {
            return key;
        }
        // 如果key为自定义DTO，则强制开发人员在使用该方法时，必须实现该方法，否则直接抛异常（通常根据自定义DTO构建的缓存key的类型为String）
        throw new L2CacheException("未实现方法CacheService.buildCacheKey()，请检查代码");
    }

    /**
     * 获取缓存（如果存在，则获取并返回）
     * 注：仅仅只是获取，若缓存项不存在，则不会加
     *
     * @see CacheService#getNativeL2cache()
     * @see CacheService#buildCacheKey(Object)
     */
    default R get(K key) {
        Cache l2cache = getNativeL2cache();
        if (null == l2cache) {
            throw new L2CacheException("未获取到l2cache的Cache对象，请检查缓存配置是否正确");
        }
        return (R) l2cache.getIfPresent(this.buildCacheKey(key));
    }

    /**
     * 获取或加载缓存
     * 注：若缓存不存在，则从加载并设置到缓存，并返回
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
     *
     * @see CacheService#getNativeL2cache()
     * @see CacheService#buildCacheKey(Object)
     */
    default boolean isExists(K key) {
        Cache l2cache = getNativeL2cache();
        if (null == l2cache) {
            throw new L2CacheException("未获取到l2cache的Cache对象，请检查缓存配置是否正确");
        }
        return l2cache.isExists(this.buildCacheKey(key));
    }

    /**
     * 批量get
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     * @see Cache#batchGet
     */
    default Map<K, R> batchGet(List<K> keyList) {
        Cache l2cache = getNativeL2cache();
        if (null == l2cache) {
            throw new L2CacheException("未获取到l2cache的Cache对象，请检查缓存配置是否正确");
        }
        return l2cache.batchGet(keyList);
    }

    /**
     * 批量get或load
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     * @see Cache#batchGetOrLoad
     */
    default Map<K, R> batchGetOrLoad(List<K> keyList) {
        // 默认直接抛出异常，也就是说强制开发人员在使用该方法时，必须实现该方法，否则直接抛异常
        throw new L2CacheException("未实现方法CacheService.batchGetOrLoad()，请检查代码");
    }
}
