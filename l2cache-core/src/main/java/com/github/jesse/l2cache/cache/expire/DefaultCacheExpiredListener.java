package com.github.jesse.l2cache.cache.expire;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.cache.Level2Cache;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.content.NullValue;
import com.github.jesse.l2cache.sync.CacheMessage;
import com.github.jesse.l2cache.util.CacheValueHashUtil;
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
        logger.info("level1Cache evict cache, removalCause={}, cacheName={}, key={}, value={}", removalCause, cache.getCacheName(), key, value);

        // 缓存过期时，发送同步消息（evict删除、put替换操作，在对应的方法中有显示发送同步消息）
        // refreshAfterWrite模式下：缓存过期，会执行load缓存，此时 removalCause=EXPLICIT，而不是EXPIRED
        // expireAfterWrite模式下：缓存过期，不会执行load缓存，此时 removalCause=EXPIRED
        if ("EXPIRED".equalsIgnoreCase(removalCause)) {
            if (cache instanceof CompositeCache) {
                Level1Cache level1Cache = ((CompositeCache) cache).getLevel1Cache();
                if (null == level1Cache) {
                    return;
                }
                // logger.info("level1Cache expired cache, publish message, removalCause={}, key={}, value={}", removalCause, key, value);
                // 计算缓存值的哈希，用于防止重复发送消息的控制
                String valueHash = CacheValueHashUtil.calcHash(value);
                level1Cache.getCacheSyncPolicy().publish(new CacheMessage(level1Cache.getInstanceId(), level1Cache.getCacheType(), level1Cache.getCacheName(), key, CacheConsts.CACHE_CLEAR, "expired", valueHash));
            }
        }

        if (!(value instanceof NullValue)) {
            return;
        }
        // 目的是在一级缓存过期时，同时将二级缓存中的NullValue也清理掉
        if (cache instanceof CompositeCache) {
            Level2Cache level2Cache = ((CompositeCache) cache).getLevel2Cache();
            if (null == level2Cache) {
                return;
            }
            level2Cache.evict(key);
            if (logger.isDebugEnabled()) {
                logger.debug("level2Cache evict expired cache, cacheName={}, key={}, value={}", cache.getCacheName(), key, value);
            }
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
