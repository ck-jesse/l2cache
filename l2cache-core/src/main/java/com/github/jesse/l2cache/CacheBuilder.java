package com.github.jesse.l2cache;

import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.cache.expire.CacheExpiry;
import com.github.jesse.l2cache.spi.SPI;

import java.io.Serializable;

/**
 * cache构建器
 *
 * @author chenck
 * @date 2020/7/1 20:43
 */
@SPI
public interface CacheBuilder<T extends Cache> extends Serializable {

    /**
     * 构建指定名称的cache对象
     */
    T build(String cacheName);

    /**
     * 解析缓存配置，主要针对一级缓存，如guava cache、caffeine等
     */
    CacheSpec parseSpec(String cacheName);

    /**
     * 复制属性
     */
    void copyFrom(CacheBuilder sourceBuilder);

    /**
     * 获取缓存配置
     */
    L2CacheConfig getL2CacheConfig();

    /**
     * 设置缓存配置
     */
    CacheBuilder setL2CacheConfig(L2CacheConfig l2CacheConfig);

    /**
     * 获取缓存过期监听器
     */
    CacheExpiredListener getExpiredListener();

    /**
     * 设置缓存过期监听器
     */
    CacheBuilder setExpiredListener(CacheExpiredListener expiredListener);

    /**
     * 获取缓存同步策略
     */
    CacheSyncPolicy getCacheSyncPolicy();

    /**
     * 设置缓存同步策略
     */
    CacheBuilder setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy);

    /**
     * 获取真实的缓存Client实例
     * 注：主要用于二级缓存，一级缓存如果有需要可以使用
     */
    Object getActualCacheClient();

    /**
     * 设置真实的缓存Client实例
     * 注：主要是为了在使用二级缓存时留一个扩展点，可以直接设置应用中已经存在的缓存Client实例，如：RedissonClient、RedisTemplate 等
     */
    CacheBuilder setActualCacheClient(Object actualCacheClient);

    /**
     * 获取缓存剩余过期时间策略
     */
    CacheExpiry getCacheExpiry();

    /**
     * 设置缓存剩余过期时间策略
     */
    CacheBuilder setCacheExpiry(CacheExpiry cacheExpiry);
}
