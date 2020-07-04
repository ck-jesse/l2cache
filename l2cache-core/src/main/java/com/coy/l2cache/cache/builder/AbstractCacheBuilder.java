package com.coy.l2cache.cache.builder;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CacheExpiredListener;
import com.coy.l2cache.cache.config.CacheConfig;
import com.coy.l2cache.cache.sync.CacheSyncPolicy;

/**
 * @author chenck
 * @date 2020/7/2 11:44
 */
public abstract class AbstractCacheBuilder<T extends Cache> implements CacheBuilder {

    private CacheConfig cacheConfig;

    private CacheExpiredListener expiredListener;

    private CacheSyncPolicy cacheSyncPolicy;

    // 暂不开放自定义CacheLoader，默认一个cacheName对应一个CacheLoader
    //private CacheLoader cacheLoader;

    @Override
    public void copyFrom(CacheBuilder sourceBuilder) {
        this.setCacheConfig(sourceBuilder.getCacheConfig());
        this.setExpiredListener(sourceBuilder.getExpiredListener());
        this.setCacheSyncPolicy(sourceBuilder.getCacheSyncPolicy());
        //this.cacheLoader(sourceBuilder.getCacheLoader());
    }

    @Override
    public CacheBuilder setCacheConfig(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
        return this;
    }

    @Override
    public CacheBuilder setExpiredListener(CacheExpiredListener expiredListener) {
        this.expiredListener = expiredListener;
        return this;
    }

    @Override
    public CacheBuilder setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {
        this.cacheSyncPolicy = cacheSyncPolicy;
        return this;
    }

    @Override
    public CacheConfig getCacheConfig() {
        return this.cacheConfig;
    }

    @Override
    public CacheExpiredListener getExpiredListener() {
        return this.expiredListener;
    }

    @Override
    public CacheSyncPolicy getCacheSyncPolicy() {
        return this.cacheSyncPolicy;
    }

    /*
    public CacheLoader getCacheLoader() {
        return cacheLoader;
    }

    public CacheBuilder cacheLoader(CacheLoader cacheLoader) {
        this.cacheLoader = cacheLoader;
        return this;
    }
    */
}
