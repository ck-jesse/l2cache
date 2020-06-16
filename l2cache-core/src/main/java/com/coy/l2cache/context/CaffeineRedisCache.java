package com.coy.l2cache.context;

import com.coy.l2cache.CaffeineRedisCacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

/**
 * 同步 CaffeineRedisCache
 *
 * @author chenck
 * @date 2020/4/28 19:55
 */
public class CaffeineRedisCache extends AbstractCaffeineRedisCache {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineRedisCache.class);

    /**
     * Caffeine Cache
     */
    // L1
    private final Cache<Object, Object> caffeineCache;

    public CaffeineRedisCache(String name, RedisTemplate<Object, Object> redisTemplate,
                              CaffeineRedisCacheProperties caffeineRedisCacheProperties, long expireTime,
                              Cache<Object, Object> caffeineCache, CacheLoader<Object, Object> cacheLoader) {
        super(name, redisTemplate, caffeineRedisCacheProperties, expireTime, cacheLoader);
        Assert.notNull(caffeineCache, "Cache must not be null");
        this.caffeineCache = caffeineCache;
    }

    @Override
    public Cache<Object, Object> getNativeCache() {
        return this.caffeineCache;
    }

    @Override
    public Object get0(Object key) {
        return ((LoadingCache<Object, Object>) this.caffeineCache).get(key);
    }

    @Override
    public Object get0(Object key, Callable<?> valueLoader) {
        return this.caffeineCache.get(key, new LoadFunction(this, valueLoader));
    }

    @Override
    public Object lookup0(Object key) {
        return caffeineCache.getIfPresent(key);
    }

    @Override
    public void put0(Object key, Object value) {
        caffeineCache.put(key, value);
    }

    @Override
    public void evict0(Object key) {
        caffeineCache.invalidate(key);
    }

    @Override
    public void clear0() {
        caffeineCache.invalidateAll();
    }

    @Override
    public boolean isLoadingCache() {
        return caffeineCache instanceof LoadingCache;
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("clear local cache, name={}, key={}", this.getName(), key);
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
        }
    }

    @Override
    public void refresh(@NonNull Object key) {
        if (isLoadingCache()) {
            logger.debug("refresh cache, name={}, key={}", this.getName(), key);
            ((LoadingCache) caffeineCache).refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAll cache, name={}, key={}", this.getName(), key);
                loadingCache.refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(@NonNull Object key) {
        if (isLoadingCache()) {
            logger.debug("refreshExpireCache cache, name={}, key={}", this.getName(), key);
            // 通过LoadingCache.get(key)来刷新过期缓存
            ((LoadingCache) caffeineCache).get(key);
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAllExpireCache cache, name={}, key={}", this.getName(), key);
                // 通过LoadingCache.get(key)来刷新过期缓存
                loadingCache.get(key);
            }
        }
    }

}
