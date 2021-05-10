package com.coy.l2cache.load;

import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.CacheSyncPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.Callable;

/**
 * 自定义CacheLoader
 * <p>
 * 目的：主要是为了在使用refreshAfterWrite策略的特性：仅加载数据的线程阻塞，其他线程返回旧值
 * 结合@Cacheable(sync=true)，在高并发场景下可提供更佳的性能。
 *
 * @author chenck
 * @date 2020/5/9 14:28
 */
public class CustomCacheLoader implements CacheLoader<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(CustomCacheLoader.class);

    /**
     * <key, Callable>
     * 用于保证并发场景下对于不同的key找到对应的Callable进行数据加载
     * 注：ConcurrentReferenceHashMap是一个实现软/弱引用的map，防止OOM出现
     * 注：由 ConcurrentReferenceHashMap 优化为 Caffeine.Cache，并设置最大元素梳理，避免元素过多占用内存过多，导致频繁的gc
     * Caffeine 基于大小的淘汰机制，因为是异步线程池的方式来执行的清理任务，所以在大量不同key访问的情况下，清理任务可能出现堆积的情况，也就是说极端情况下也会出现缓存未被及时清理掉占用大量内存的情况出现
     */
    private Cache<Object, ValueLoaderWarpper> valueLoaderCache;
    private String instanceId;
    private String cacheType;
    private String cacheName;
    private Level2Cache level2Cache;
    private CacheSyncPolicy cacheSyncPolicy;
    private boolean allowNullValues;
    private Cache<Object, Integer> nullValueCache;

    private CustomCacheLoader(String instanceId, String cacheType, String cacheName, Integer maxSize) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        if (null == maxSize || maxSize <= 0) {
            maxSize = 1000;
        }
        valueLoaderCache = Caffeine.newBuilder()
                .removalListener((key, value, cause) -> {
                    logger.info("[CustomCacheLoader]valueLoader is Recycled, cacheName={}, cause={}, key={}, valueLoader={}", cacheName, cause, key, value);
                })
                .maximumSize(maxSize)
                .build();
    }

    /**
     * create CacheLoader instance
     */
    public static CustomCacheLoader newInstance(String instanceId, String cacheType, String cacheName, Integer maxSize) {
        return new CustomCacheLoader(instanceId, cacheType, cacheName, maxSize);
    }

    @Override
    public void setLevel2Cache(Level2Cache level2Cache) {
        this.level2Cache = level2Cache;
    }

    @Override
    public void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    @Override
    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }

    @Override
    public void setNullValueCache(Cache<Object, Integer> nullValueCache) {
        this.nullValueCache = nullValueCache;
    }

    @Override
    public void addValueLoader(Object key, Callable<?> valueLoader) {
        ValueLoaderWarpper warpper = valueLoaderCache.getIfPresent(key);
        if (null == warpper) {
            valueLoaderCache.put(key, ValueLoaderWarpper.newInstance(this.cacheName, key, valueLoader));
        } else {
            if (null == warpper.getValueLoader()) {
                logger.info("[CustomCacheLoader]ValueLoaderWarpper.valueLoader is null and set a new valueLoader, cacheName={}, key={}, valueLoader={}", cacheName, key, valueLoader);
                warpper.setValueLoader(valueLoader);
            }
        }
    }

    @Override
    public void delValueLoader(Object key) {
        valueLoaderCache.invalidate(key);
    }

    @Override
    public Object load(KeyWarpper<Object> keyWarpper) {
        try {
            if (null != keyWarpper.getMdcContextMap()) {
                MDC.setContextMap(keyWarpper.getMdcContextMap());
            }
            // 直接返回null，目的是使spring cache后续逻辑去执行具体的加载数据方法，然后put到缓存
            ValueLoaderWarpper valueLoader = valueLoaderCache.getIfPresent(keyWarpper.getKey());

            LoadFunction loadFunction = new LoadFunction(this.instanceId, this.cacheType, cacheName, level2Cache, cacheSyncPolicy, valueLoader, this.allowNullValues, this.nullValueCache);
            return loadFunction.apply(keyWarpper.getKey());
        } finally {
            if (null != keyWarpper.getMdcContextMap()) {
                MDC.clear();
            }
        }
    }

    @Override
    public ValueLoaderWarpper getValueLoaderWarpper(Object key) {
        return valueLoaderCache.getIfPresent(key);
    }

}
