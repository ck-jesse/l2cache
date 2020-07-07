package com.coy.l2cache.sync;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSyncPolicy;

/**
 * @author chenck
 * @date 2020/7/7 17:04
 */
public abstract class AbstractCacheSyncPolicy implements CacheSyncPolicy {

    private CacheConfig cacheConfig;
    private MessageListener cacheMessageListener;
    private Object actualClient;

    @Override
    public CacheConfig getCacheConfig() {
        return this.cacheConfig;
    }

    @Override
    public CacheSyncPolicy setCacheConfig(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
        return this;
    }

    @Override
    public MessageListener getCacheMessageListener() {
        return this.cacheMessageListener;
    }

    @Override
    public CacheSyncPolicy setCacheMessageListener(MessageListener cacheMessageListener) {
        this.cacheMessageListener = cacheMessageListener;
        return this;
    }

    @Override
    public Object getActualClient() {
        return this.actualClient;
    }

    @Override
    public CacheSyncPolicy setActualClient(Object actualClient) {
        this.actualClient = actualClient;
        return this;
    }

}
