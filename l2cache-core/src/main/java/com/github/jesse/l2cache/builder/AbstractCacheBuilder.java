package com.github.jesse.l2cache.builder;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.cache.expire.CacheExpiry;

/**
 * @author chenck
 * @date 2020/7/2 11:44
 */
public abstract class AbstractCacheBuilder<T extends Cache> implements CacheBuilder {

    private L2CacheConfig l2CacheConfig;

    private CacheExpiredListener expiredListener;

    private CacheSyncPolicy cacheSyncPolicy;

    private CacheExpiry cacheExpiry;

    private volatile Object actualCacheClient;

    // 暂不开放自定义CacheLoader，默认一个cacheName对应一个CacheLoader
    //private CacheLoader cacheLoader;

    @Override
    public CacheSpec parseSpec(String cacheName) {
        return null;
    }

    @Override
    public void copyFrom(CacheBuilder sourceBuilder) {
        this.setL2CacheConfig(sourceBuilder.getL2CacheConfig());
        this.setExpiredListener(sourceBuilder.getExpiredListener());
        this.setCacheSyncPolicy(sourceBuilder.getCacheSyncPolicy());
        this.setActualCacheClient(sourceBuilder.getActualCacheClient());
        this.setCacheExpiry(sourceBuilder.getCacheExpiry());
    }

    @Override
    public CacheBuilder setL2CacheConfig(L2CacheConfig l2CacheConfig) {
        this.l2CacheConfig = l2CacheConfig;
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
    public CacheExpiry getCacheExpiry() {
        return this.cacheExpiry;
    }

    @Override
    public CacheBuilder setCacheExpiry(CacheExpiry cacheExpiry) {
        this.cacheExpiry = cacheExpiry;
        return this;
    }

    @Override
    public L2CacheConfig getL2CacheConfig() {
        return this.l2CacheConfig;
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
