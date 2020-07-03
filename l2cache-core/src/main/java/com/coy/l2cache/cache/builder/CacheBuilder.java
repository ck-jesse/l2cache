package com.coy.l2cache.cache.builder;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CacheExpiredListener;
import com.coy.l2cache.cache.config.CacheConfig;
import com.coy.l2cache.cache.spi.SPI;
import com.coy.l2cache.cache.sync.CacheSyncPolicy;

/**
 * cache构建器
 *
 * @author chenck
 * @date 2020/7/1 20:43
 */
@SPI
public interface CacheBuilder<T extends Cache> {

    /**
     * 构建指定名称的cache对象
     */
    T build(String cacheName);

    /**
     * 复制属性
     */
    void copyFrom(CacheBuilder sourceBuilder);

    /**
     * 设置缓存配置
     */
    CacheBuilder cacheConfig(CacheConfig cacheConfig);

    /**
     * 设置缓存过期监听器
     */
    CacheBuilder expiredListener(CacheExpiredListener expiredListener);

    /**
     * 设置缓存同步策略
     */
    CacheBuilder cacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy);

    /**
     * 获取缓存配置
     */
    CacheConfig getCacheConfig();

    /**
     * 获取缓存过期监听器
     */
    CacheExpiredListener getExpiredListener();

    /**
     * 获取缓存同步策略
     */
    CacheSyncPolicy getCacheSyncPolicy();
}
