package com.coy.l2cache.cache;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * 组合缓存器
 *
 * @author chenck
 * @date 2020/6/29 17:32
 */
public class CompositeCache extends AbstractAdaptingCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(CompositeCache.class);

    private final CacheConfig.Composite composite;
    /**
     * 一级缓存
     */
    private final Level1Cache level1Cache;

    /**
     * 二级缓存
     */
    private final Level2Cache level2Cache;

    public CompositeCache(String cacheName, CacheConfig cacheConfig, Level1Cache level1Cache, Level2Cache level2Cache) {
        super(cacheName, cacheConfig);
        this.composite = cacheConfig.getComposite();
        this.level1Cache = level1Cache;
        this.level2Cache = level2Cache;
        if (level1Cache.isLoadingCache()) {
            // 设置level2Cache到CustomCacheLoader中，以便CacheLoader中直接操作level2Cache
            level1Cache.getCacheLoader().setLevel2Cache(level2Cache);
        }
    }

    @Override
    public String getCacheType() {
        return CacheType.COMPOSITE.name().toLowerCase();
    }

    @Override
    public CompositeCache getActualCache() {
        return this;
    }

    @Override
    public Object get(Object key) {
        // L1为LoadingCache，则会在CacheLoader中对L2进行了存取操作，所以此处直接返回
        if (level1Cache.isLoadingCache()) {
            return level1Cache.get(key);
        }

        // 从L1获取缓存
        Object value = level1Cache.get(key);
        if (value != null) {
            logger.debug("level1Cache get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return value;
        }

        // 从L2获取缓存
        value = level2Cache.get(key);
        if (value != null) {
            logger.debug("level2Cache get cache and put in level1Cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            level1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        // LoadFunction.apply()中封装了L2获取缓存的逻辑，所以此处只需要调用level1Cache.get(key, valueLoader)
        return level1Cache.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        level2Cache.put(key, value);
        level1Cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        logger.debug("[CompositeCache] evict cache, cacheName={}, key={}", this.getCacheName(), key);
        // 先清除L2中缓存数据，然后清除L1中的缓存，避免短时间内如果先清除L1缓存后其他请求会再从L2里加载到L1中
        level2Cache.evict(key);
        level1Cache.evict(key);
    }

    @Override
    public void clear() {
        logger.debug("[CompositeCache] clear all cache, cacheName={}", this.getCacheName());
        // 先清除L2中缓存数据，然后清除L1中的缓存，避免短时间内如果先清除L1缓存后其他请求会再从L2里加载到L1中
        level2Cache.clear();
        level1Cache.clear();
    }

    public Level1Cache getLevel1Cache() {
        return level1Cache;
    }

    public Level2Cache getLevel2Cache() {
        return level2Cache;
    }
}
