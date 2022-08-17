package com.github.jesse.l2cache.builder;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;

/**
 * @author chenck
 * @date 2020/7/2 11:44
 */
public abstract class AbstractCacheBuilder<T extends Cache> implements CacheBuilder {

    private CacheConfig cacheConfig;

    private CacheExpiredListener expiredListener;

    private CacheSyncPolicy cacheSyncPolicy;

    private volatile Object actualCacheClient;

    // 暂不开放自定义CacheLoader，默认一个cacheName对应一个CacheLoader
    //private CacheLoader cacheLoader;

    @Override
    public CacheSpec parseSpec(String cacheName) {
        return null;
    }

    @Override
    public void copyFrom(CacheBuilder sourceBuilder) {
        this.setCacheConfig(sourceBuilder.getCacheConfig());
        this.setExpiredListener(sourceBuilder.getExpiredListener());
        this.setCacheSyncPolicy(sourceBuilder.getCacheSyncPolicy());
        this.setActualCacheClient(sourceBuilder.getActualCacheClient());
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

    @Override
    public Object getActualCacheClient() {
        return this.actualCacheClient;
    }

    @Override
    public CacheBuilder setActualCacheClient(Object actualCacheClient) {
        this.actualCacheClient = actualCacheClient;
        return this;
    }

}
