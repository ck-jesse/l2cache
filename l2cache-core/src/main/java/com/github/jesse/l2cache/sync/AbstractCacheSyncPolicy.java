package com.github.jesse.l2cache.sync;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import org.redisson.api.RedissonClient;

/**
 * @author chenck
 * @date 2020/7/7 17:04
 */
public abstract class AbstractCacheSyncPolicy implements CacheSyncPolicy {

    private L2CacheConfig cacheConfig;
    private MessageListener cacheMessageListener;
    private RedissonClient actualClient;

    @Override
    public L2CacheConfig getL2CacheConfig() {
        return this.cacheConfig;
    }

    @Override
    public CacheSyncPolicy setCacheConfig(L2CacheConfig cacheConfig) {
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
    public RedissonClient getActualClient() {
        return this.actualClient;
    }

    @Override
    public CacheSyncPolicy setActualClient(RedissonClient actualClient) {
        this.actualClient = actualClient;
        return this;
    }

}
