package com.coy.l2cache.cache;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheConfig;

public abstract class AbstractAdaptingCache implements Cache {

    /**
     * 缓存实例id
     */
    private String instanceId;
    /**
     * 缓存名字
     */
    private final String cacheName;
    /**
     * 是否允许为空
     */
    private final boolean allowNullValues;
    /**
     * NullValue的过期时间，单位秒
     */
    private long nullValueExpireTimeSeconds;

    public AbstractAdaptingCache(String cacheName, CacheConfig cacheConfig) {
        this.instanceId = cacheConfig.getInstanceId();
        this.cacheName = cacheName;
        this.allowNullValues = cacheConfig.isAllowNullValues();
        this.nullValueExpireTimeSeconds = cacheConfig.getNullValueExpireTimeSeconds();
        if (this.nullValueExpireTimeSeconds < 0) {
            this.nullValueExpireTimeSeconds = 60;
        }
    }

    @Override
    public boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    @Override
    public long getNullValueExpireTimeSeconds() {
        return this.nullValueExpireTimeSeconds;
    }

    @Override
    public String getInstanceId() {
        return this.instanceId;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

}
