package com.coy.l2cache.builder;

import com.coy.l2cache.Cache;
import com.coy.l2cache.cache.NoneCache;

/**
 * @author chenck
 * @date 2020/7/2 20:49
 */
public class NoneCacheBuilder extends AbstractCacheBuilder<NoneCache> {
    @Override
    public Cache build(String cacheName) {
        return new NoneCache(cacheName, this.getCacheConfig());
    }
}
