package com.coy.l2cache.cache;

import com.coy.l2cache.cache.sync.CacheSyncPolicy;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;
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
     * 用户保证并发场景下对于不同的key找到对应的Callable进行数据加载
     */
    private static final Map<Object, Callable<?>> VALUE_LOADER_CACHE = new ConcurrentReferenceHashMap<>();

    private Cache level2Cache;
    private CacheSyncPolicy cacheSyncPolicy;

    /**
     * 设置加载数据的处理器
     * 注：在获取缓存时动态设置valueLoader，来达到实现不同缓存调用不同的加载数据逻辑的目的。
     */
    public void addValueLoader(Object key, Callable<?> valueLoader) {
        Callable<?> oldCallable = VALUE_LOADER_CACHE.get(key);
        if (null == oldCallable) {
            VALUE_LOADER_CACHE.put(key, valueLoader);
        }
    }

    public void setLevel2Cache(Cache level2Cache) {
        this.level2Cache = level2Cache;
    }

    public Cache getLevel2Cache() {
        return level2Cache;
    }

    public CacheSyncPolicy getCacheSyncPolicy() {
        return cacheSyncPolicy;
    }

    public void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    public LoadFunction newLoadFunction(Callable<?> valueLoader) {
        return new LoadFunction(level2Cache, cacheSyncPolicy, valueLoader);
    }

    @Nullable
    @Override
    public Object load(@NonNull Object key) throws Exception {
        // 直接返回null，目的是使后续逻辑去执行具体的加载数据方法，然后put到缓存
        Callable<?> valueLoader = VALUE_LOADER_CACHE.get(key);
        if (null == valueLoader) {
            logger.info("[CustomCacheLoader] valueLoader is null direct return null, key={}", key);
            return null;
        }

        if (null == level2Cache) {
            logger.info("[CustomCacheLoader] level2Cache is null direct return null, key={}", key);
            return null;
        }
        LoadFunction loadFunction = new LoadFunction(level2Cache, cacheSyncPolicy, valueLoader);
        return loadFunction.apply(key);
    }

}
