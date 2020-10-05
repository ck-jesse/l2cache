package com.coy.l2cache.cache.expire;

import com.coy.l2cache.Cache;
import com.coy.l2cache.cache.CompositeCache;
import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.content.NullValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认缓存过期监听器
 * <p>
 * 主要是针对一级缓存定义该功能，如guava cache和caffeine
 *
 * @author chenck
 * @date 2020/7/2 20:18
 */
public class DefaultCacheExpiredListener implements CacheExpiredListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheExpiredListener.class);

    private Cache cache;

    @Override
    public void onExpired(Object key, Object value, String removalCause) {
        if (null == cache) {
            logger.info("level1Cache clear expired cache, removalCause={}, key={}, value={}", removalCause, key, value);
            return;
        }
        logger.info("level1Cache evict expired cache, removalCause={}, cacheName={}, key={}, value={}", removalCause, cache.getCacheName(), key, value);
        if (!(value instanceof NullValue)) {
            return;
        }
        // 目的是在一级缓存过期时，同时将二级缓存也清理掉
        if (cache instanceof CompositeCache) {
            Level2Cache level2Cache = ((CompositeCache) cache).getLevel2Cache();
            if (null == level2Cache) {
                return;
            }
            level2Cache.evict(key);
            logger.info("level2Cache evict expired cache, cacheName={}, key={}, value={}", cache.getCacheName(), key, value);
            return;
        }
        if (cache instanceof Level2Cache) {
            cache.evict(key);
            logger.info("level2Cache evict expired cache, cacheName={}, key={}, value={}", cache.getCacheName(), key, value);
        }
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }
}
