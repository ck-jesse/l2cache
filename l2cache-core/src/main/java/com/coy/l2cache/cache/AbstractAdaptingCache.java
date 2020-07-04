package com.coy.l2cache.cache;

public abstract class AbstractAdaptingCache implements Cache {

    /**
     * 缓存名字
     */
    private final String cacheName;
    /**
     * 是否允许为空
     */
    private final boolean allowNullValues;

    public AbstractAdaptingCache(String cacheName, boolean allowNullValues) {
        this.cacheName = cacheName;
        this.allowNullValues = allowNullValues;
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
