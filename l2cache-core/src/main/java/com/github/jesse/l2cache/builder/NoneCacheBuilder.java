package com.github.jesse.l2cache.builder;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.NoneCache;

/**
 * @author chenck
 * @date 2020/7/2 20:49
 */
public class NoneCacheBuilder extends AbstractCacheBuilder<NoneCache> {
    @Override
    public Cache build(String cacheName) {
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);
        
        return new NoneCache(cacheName, cacheConfig);
    }
}
