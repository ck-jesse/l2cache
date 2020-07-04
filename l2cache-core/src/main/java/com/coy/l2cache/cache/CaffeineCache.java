package com.coy.l2cache.cache;

import com.coy.l2cache.cache.sync.CacheSyncPolicy;
import com.coy.l2cache.consts.CacheConsts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * Caffeine Cache
 *
 * @author chenck
 * @date 2020/6/29 16:37
 */
public class CaffeineCache implements L1Cache {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineCache.class);

    /**
     * 缓存名字
     */
    private final String cacheName;
    /**
     * 缓存加载器，用于异步加载缓存
     */
    private final CacheLoader cacheLoader;
    /**
     * 缓存同步策略
     */
    private final CacheSyncPolicy cacheSyncPolicy;
    /**
     * L1 Caffeine
     */
    private final Cache<Object, Object> caffeineCache;

    public CaffeineCache(String cacheName, CacheLoader cacheLoader, Cache<Object, Object> caffeineCache) {
        this(cacheName, cacheLoader, null, caffeineCache);
    }

    public CaffeineCache(String cacheName, CacheLoader cacheLoader, CacheSyncPolicy cacheSyncPolicy, Cache<Object, Object> caffeineCache) {
        this.cacheName = cacheName;
        this.cacheLoader = cacheLoader;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.caffeineCache = caffeineCache;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    @Override
    public String getLevel() {
        return "1";
    }

    @Override
    public Cache<Object, Object> getActualCache() {
        return this.caffeineCache;
    }

    @Override
    public CacheSyncPolicy getCacheSyncPolicy() {
        return this.cacheSyncPolicy;
    }

    @Override
    public CacheLoader getCacheLoader() {
        return this.cacheLoader;
    }

    @Override
    public boolean isLoadingCache() {
        return this.caffeineCache instanceof LoadingCache && null != this.cacheLoader;
    }

    @Override
    public Object get(Object key) {
        if (isLoadingCache()) {
            // 如果是refreshAfterWrite策略，则只会阻塞加载数据的线程，其他线程返回旧值（如果是异步加载，则所有线程都返回旧值）
            Object value = ((LoadingCache) this.caffeineCache).get(key);
            logger.debug("CaffeineCache LoadingCache.get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return value;
        }
        return this.caffeineCache.getIfPresent(key);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (isLoadingCache()) {
            if (this.cacheLoader instanceof CustomCacheLoader) {
                // 将Callable设置到自定义CacheLoader中，以便在load()中执行具体的业务方法来加载数据
                ((CustomCacheLoader) this.cacheLoader).addValueLoader(key, valueLoader);
            }

            Object value = this.get(key);
            return (T) value;
        }

        // 同步加载数据，仅一个线程加载数据，其他线程均阻塞
        Object value = this.caffeineCache.get(key, new LoadFunction(this.cacheName, null, this.getCacheSyncPolicy(), valueLoader));
        logger.debug("CaffeineCache Cache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
        return (T) value;
    }

    @Override
    public void put(Object key, Object value) {
        caffeineCache.put(key, value);
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(key, CacheConsts.CACHE_REFRESH);
        }
    }

    @Override
    public void evict(Object key) {
        logger.debug("evict cache, name={}, key={}", this.getCacheName(), key);
        caffeineCache.invalidate(key);
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(key, CacheConsts.CACHE_CLEAR);
        }
    }

    @Override
    public void clear() {
        logger.debug("clear cache, name={}", this.getCacheName());
        caffeineCache.invalidateAll();
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(null, CacheConsts.CACHE_CLEAR);
        }
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("clear local cache, name={}, key={}", this.getCacheName(), key);
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
        }
    }

    @Override
    public void refresh(Object key) {
        if (isLoadingCache()) {
            logger.debug("refresh cache, name={}, key={}", this.getCacheName(), key);
            ((LoadingCache) caffeineCache).refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAll cache, name={}, key={}", this.getCacheName(), key);
                loadingCache.refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(Object key) {
        if (isLoadingCache()) {
            logger.debug("refreshExpireCache cache, name={}, key={}", this.getCacheName(), key);
            // 通过LoadingCache.get(key)来刷新过期缓存
            ((LoadingCache) caffeineCache).get(key);
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAllExpireCache cache, name={}, key={}", this.getCacheName(), key);
                // 通过LoadingCache.get(key)来刷新过期缓存
                loadingCache.get(key);
            }
        }
    }
}
