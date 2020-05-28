package com.coy.l2cache.context;

import com.coy.l2cache.CaffeineRedisCacheProperties;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * 异步 CaffeineRedisCache
 *
 * @author chenck
 * @date 2020/4/28 19:55
 */
public class AsyncCaffeineRedisCache extends AbstractCaffeineRedisCache {

    private static final Logger logger = LoggerFactory.getLogger(AsyncCaffeineRedisCache.class);

    /**
     * Caffeine Cache
     */
    private final AsyncCache<Object, Object> caffeineCache;

    /**
     * Create a {@link AsyncCaffeineRedisCache} instance with the specified name and the
     * given internal {@link Cache} to use.
     *
     * @param name                         the name of the cache
     * @param redisTemplate                whether to accept and convert {@code null}values for this cache
     * @param caffeineRedisCacheProperties the properties for this cache
     * @param expireTime                   the expire time
     * @param caffeineCache                the backing Caffeine Cache instance
     * @param cacheLoader                  the backing Caffeine cacheLoader instance
     */
    public AsyncCaffeineRedisCache(String name, RedisTemplate<Object, Object> redisTemplate,
                                   CaffeineRedisCacheProperties caffeineRedisCacheProperties, long expireTime,
                                   AsyncCache<Object, Object> caffeineCache, CacheLoader<Object, Object> cacheLoader) {
        super(name, redisTemplate, caffeineRedisCacheProperties, expireTime, cacheLoader);
        Assert.notNull(caffeineCache, "AsyncCache must not be null");
        this.caffeineCache = caffeineCache;
    }

    @Override
    public AsyncCache<Object, Object> getNativeCache() {
        return this.caffeineCache;
    }

    @Override
    public Object get0(Object key) {
        try {
            return ((AsyncLoadingCache<Object, Object>) this.caffeineCache).get(key).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("AsyncLoadingCache.get(key, callable) error, cacheName=" + this.getName() + ", key=" + key, e);
            return null;
        }
    }

    @Override
    public Object get0(Object key, Callable<?> valueLoader) {
        try {
            return caffeineCache.get(key, new LoadFunction(this, valueLoader)).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("AsyncCache.get(key, callable) error, cacheName=" + this.getName() + ", key=" + key, e);
            return null;
        }
    }

    @Override
    public Object lookup0(Object key) {
        CompletableFuture future = caffeineCache.getIfPresent(key);
        if (null == future) {
            return null;
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("lookup0 error, cacheName=" + this.getName() + ", key=" + key, e);
        }
        return null;
    }

    @Override
    public void put0(Object key, Object value) {
        this.caffeineCache.put(key, CompletableFuture.completedFuture(value));
    }

    @Override
    public void evict0(Object key) {
        this.caffeineCache.synchronous().invalidate(key);
    }

    @Override
    public void clear0() {
        this.caffeineCache.synchronous().invalidateAll();
    }

    @Override
    public boolean isLoadingCache() {
        return caffeineCache instanceof AsyncLoadingCache;
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("clear local cache, name={}, key={}", this.getName(), key);
        if (key == null) {
            caffeineCache.synchronous().invalidateAll();
        } else {
            caffeineCache.synchronous().invalidate(key);
        }
    }

    @Override
    public void refresh(@NonNull Object key) {
        if (isLoadingCache()) {
            // 注：refresh方法会重新加载数据
            logger.info("refresh cache, name={}, key={}", this.getName(), key);
            AsyncLoadingCache loadingCache = (AsyncLoadingCache) caffeineCache;
            loadingCache.synchronous().refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            AsyncLoadingCache loadingCache = (AsyncLoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAll cache, name={}, key={}", this.getName(), key);
                loadingCache.synchronous().refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(@NonNull Object key) {
        if (isLoadingCache()) {
            logger.debug("refreshExpireCache cache, name={}, key={}", this.getName(), key);
            // 通过LoadingCache.get(key)来刷新过期缓存
            ((AsyncLoadingCache) caffeineCache).synchronous().get(key);
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            AsyncLoadingCache loadingCache = (AsyncLoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAllExpireCache cache, name={}, key={}", this.getName(), key);
                // 通过LoadingCache.get(key)来刷新过期缓存
                loadingCache.synchronous().get(key);
            }
        }
    }

}
