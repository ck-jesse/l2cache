package com.coy.l2cache.cache;

import com.coy.l2cache.cache.config.CacheConfig;

public abstract class AbstractAdaptingCache implements Cache {

    /**
     * 缓存名字
     */
    private final String cacheName;
    /**
     * 是否允许为空
     */
    private final boolean allowNullValues;

    public AbstractAdaptingCache(String cacheName, CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.allowNullValues = cacheConfig.isAllowNullValues();
    }

    @Override
    public boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

}
