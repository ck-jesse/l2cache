package com.github.jesse.l2cache.biz;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.exception.L2CacheException;

import java.util.List;
import java.util.Map;

/**
 * 业务维度的缓存接口
 * 作用：
 * 1）标准化业务系统中对缓存的增删改查操作，避免缓存操作与业务逻辑强耦合在一起。
 * 2）作为业务系统中的一个缓存层，对业务逻辑和l2cache组件而言，起到承上启下的作用，可简化业务开发，降低开发复杂度。
 * <p>
 * 分层调用链路：业务逻辑 -> CacheService -> l2cache
 * <p>
 * 注：
 * 1）该接口是从多个核心业务系统的实战开发中提炼而来，所以个人强烈建议业务系统接入l2cache组件时，基于该接口来开发。
 * 2）定义默认的方法，目的在于简化业务开发
 *
 * @param <K> 表示缓存key，可以是单个字段，也可以是一个自定义对象。特别注意：K为自定义对象时，需要重写equals()和hashcode()方法，目的：获取到缓存数据后，业务代码中可根据自定义DTO对象，找到其对应的缓存项。
 * @param <R> 表示返回的缓存数据
 * @author chenck
 * @date 2020/8/26 12:36
 */
public interface CacheService<K, R> {

    /**
     * 获取l2cache组件的Cache对象
     */
    default Cache getNativeL2cache() {
        // 默认直接抛出异常，也就是说强制开发人员在使用该方法时，必须实现该方法，否则直接抛异常
        throw new L2CacheException("未实现方法CacheService.getNativeL2cache()，请检查代码");
    }

    // ---------------------------------------------------
    // 第一部分：业务逻辑，业务开发时，仅仅只需实现如下几个方法
    // ---------------------------------------------------

    /**
     * 获取缓存名字
     */
    String getCacheName();

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
     * 说明：20220809 由于优化CacheService后，get和getOrLoad中的key统一使用buildCacheKey(key)，所以该问题变向解决了，为了降低开发的理解成本，所以返回类型再次从Object改为String
     *
     * @return 缓存key
     */
    String buildCacheKey(K key);

    /**
     * 查询单个缓存数据
     *
     * @author chenck
     * @date 2022/8/4 22:02
     */
    R queryData(K key);

    /**
     * 查询缓存数据列表
     * 注：返回的缓存数据列表，需要根据指定的格式来组装，由于不同的业务场景，可能组装的数据不一样，所以，下放到业务中去实现
     */
    Map<K, R> queryDataList(List<K> keyList);

    // ---------------------------------------------------
    // 第二部分：缓存操作，将缓存操作封装为默认方法实现，简化业务开发
    // ---------------------------------------------------

    /**
     * 获取缓存（如果存在，则获取并返回）
     * 注：仅仅只是获取，若缓存项不存在，则不会加载
     *
     * @see CacheService#getNativeL2cache()
     * @see CacheService#buildCacheKey(Object)
     */
    default R get(K key) {
        return (R) this.getNativeL2cache().getIfPresent(this.buildCacheKey(key));
    }

    /**
     * 获取或加载缓存
     * 注：若缓存不存在，则从加载并设置到缓存，并返回
     */
    default R getOrLoad(K key) {
        return this.getNativeL2cache().get(this.buildCacheKey(key), () -> this.queryData(key));
    }

    /**
     * 设置指定key的缓存项
     */
    default R put(K key, R value) {
        this.getNativeL2cache().put(this.buildCacheKey(key), value);
        return value;
    }

    /**
     * 仅当之前没有存储指定key的value时，才存储由指定key映射的指定value
     */
    default boolean putIfAbsent(K key, R value) {
        return false;
    }

    /**
     * 重新加载缓存（存在则替换，不存在则设置）
     */
    default R reload(K key) {
        R value = this.queryData(key);
        this.getNativeL2cache().put(this.buildCacheKey(key), value);
        return value;
    }


    /**
     * 淘汰缓存
     */
    default void evict(K key) {
        this.getNativeL2cache().evict(this.buildCacheKey(key));
    }

    /**
     * 判断key是否存在
     *
     * @see CacheService#getNativeL2cache()
     * @see CacheService#buildCacheKey(Object)
     */
    default boolean isExists(K key) {
        return this.getNativeL2cache().isExists(this.buildCacheKey(key));
    }

    /**
     * 批量get
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     * @see Cache#batchGet
     */
    default Map<K, R> batchGet(List<K> keyList) {
        // K 为基本数据类型时，可以直接调用 batchGet(keyList)方法，来优化代码，减少调用buildCacheKey方法
        return this.getNativeL2cache().batchGet(keyList, k -> this.buildCacheKey(k));
    }

    /**
     * 批量get或load
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     * @see Cache#batchGetOrLoad
     */
    default Map<K, R> batchGetOrLoad(List<K> keyList) {
        return this.getNativeL2cache().batchGetOrLoad(keyList, k -> this.buildCacheKey(k), notHitCacheKeyList -> this.queryDataList(notHitCacheKeyList));
    }

    /**
     * 批量重新加载缓存（存在则替换，不存在则设置）
     */
    default Map<K, R> batchReload(List<K> keyList) {
        // 抽取公共逻辑，简化开发
        Map<K, R> value = this.queryDataList(keyList);
        this.getNativeL2cache().batchPut(value, k -> this.buildCacheKey(k));
        return value;
    }

}
