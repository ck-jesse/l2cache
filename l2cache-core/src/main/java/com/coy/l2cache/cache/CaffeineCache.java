package com.coy.l2cache.cache;

import com.coy.l2cache.cache.sync.CacheSyncPolicy;
import com.coy.l2cache.consts.CacheConsts;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
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
    private final String name;
    /**
     * caffeine 缓存加载器，用于异步加载缓存
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

    protected CaffeineCache(String name, CacheLoader cacheLoader, CacheSyncPolicy cacheSyncPolicy, Cache<Object, Object> caffeineCache) {
        this.name = name;
        this.cacheLoader = cacheLoader;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.caffeineCache = caffeineCache;
    }

    @Override
    public String getName() {
        return this.name;
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
    public boolean isLoadingCache() {
        return this.caffeineCache instanceof LoadingCache && null != this.cacheLoader;
    }

    @Override
    public Object get(Object key) {
        if (isLoadingCache()) {
            // 如果是refreshAfterWrite策略，则只会阻塞加载数据的线程，其他线程返回旧值（如果是异步加载，则所有线程都返回旧值）
            Object value = ((LoadingCache) this.caffeineCache).get(key);
            logger.debug("level1Cache LoadingCache.get cache, cacheName={}, key={}, value={}", this.getName(), key, value);
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

            Object value = get(key);
            logger.debug("level1Cache LoadingCache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getName(), key, value);
            return (T) value;
        }

        // 同步加载数据，仅一个线程加载数据，其他线程均阻塞
        Object value = this.caffeineCache.get(key, new LoadFunction(null, getCacheSyncPolicy(), valueLoader));
        logger.debug("level1Cache get(key, callable) cache, cacheName={}, key={}, value={}", this.getName(), key, value);
        return (T) value;
    }

    @Override
    public void put(Object key, Object value) {
        caffeineCache.put(key, value);
        cacheSyncPolicy.publish(key, CacheConsts.CACHE_REFRESH);
    }

    @Override
    public void evict(Object key) {
        caffeineCache.invalidate(key);
        cacheSyncPolicy.publish(key, CacheConsts.CACHE_CLEAR);
    }

    @Override
    public void clear() {
        caffeineCache.invalidateAll();
        cacheSyncPolicy.publish(null, CacheConsts.CACHE_CLEAR);
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
    public void refresh(Object key) {
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
    public void refreshExpireCache(Object key) {
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
