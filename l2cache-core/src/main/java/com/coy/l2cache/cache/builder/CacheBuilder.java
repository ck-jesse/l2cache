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
     * 获取缓存配置
     */
    CacheConfig getCacheConfig();

    /**
     * 设置缓存配置
     */
    CacheBuilder setCacheConfig(CacheConfig cacheConfig);

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
}
