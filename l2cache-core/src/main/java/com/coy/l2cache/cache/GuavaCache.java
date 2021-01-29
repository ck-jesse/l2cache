package com.coy.l2cache.cache;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSyncPolicy;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.load.CacheLoader;
import com.coy.l2cache.load.LoadFunction;
import com.coy.l2cache.load.ValueLoaderWarpper;
import com.coy.l2cache.schedule.NullValueClearSupport;
import com.coy.l2cache.schedule.NullValueCacheClearTask;
import com.coy.l2cache.schedule.RefreshExpiredCacheTask;
import com.coy.l2cache.schedule.RefreshSupport;
import com.coy.l2cache.sync.CacheMessage;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Guava Cache
 *
 * @author chenck
 * @date 2020/6/29 16:55
 */
public class GuavaCache extends AbstractAdaptingCache implements Level1Cache {

    private static final Logger logger = LoggerFactory.getLogger(GuavaCache.class);
    /**
     * guava config
     */
    private final CacheConfig.Guava guava;
    /**
     * 缓存加载器，用于异步加载缓存
     */
    private final CacheLoader cacheLoader;
    /**
     * 缓存同步策略
     */
    private final CacheSyncPolicy cacheSyncPolicy;
    /**
     * L1 Guava Cache
     */
    private Cache<Object, Object> guavaCache;
    /**
     * 存放NullValue的key，用于控制NullValue对象的有效时间
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Integer> nullValueCache;

    public GuavaCache(String cacheName, CacheConfig cacheConfig, CacheLoader cacheLoader, CacheSyncPolicy cacheSyncPolicy,
                      Cache<Object, Object> guavaCache) {
        super(cacheName, cacheConfig);
        this.guava = cacheConfig.getGuava();
        this.cacheLoader = cacheLoader;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.guavaCache = guavaCache;

        if (this.guava.isAutoRefreshExpireCache()) {
            // 定期刷新过期的缓存
            RefreshSupport.getInstance(this.guava.getRefreshPoolSize())
                    .scheduleWithFixedDelay(new RefreshExpiredCacheTask(this), 5,
                            this.guava.getRefreshPeriod(), TimeUnit.SECONDS);
        }

        if (this.isAllowNullValues()) {
            this.nullValueCache = Caffeine.newBuilder()
                    .expireAfterWrite(cacheConfig.getNullValueExpireTimeSeconds(), TimeUnit.SECONDS)
                    .maximumSize(cacheConfig.getNullValueMaxSize())
                    .removalListener((key, value, cause) -> {
                        logger.info("[NullValueCache] remove NullValue, removalCause={}, cacheName={}, key={}, value={}", cause, this.getCacheName(), key, value);
                        if (null != key) {
                            this.guavaCache.invalidate(key);
                            if (null != this.cacheSyncPolicy) {
                                this.cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR));
                            }
                        }
                    })
                    .build();
            cacheLoader.setNullValueCache(this.nullValueCache);

            // 定期清理 NullValue
            NullValueClearSupport.getInstance().scheduleWithFixedDelay(new NullValueCacheClearTask(this.getCacheName(), this.nullValueCache), 5,
                    cacheConfig.getNullValueClearPeriodSeconds(), TimeUnit.SECONDS);

            logger.info("[CaffeineCache] NullValueCache初始化成功, cacheName={}, expireTime={}s, maxSize={}, clearPeriodSeconds={}s", this.getCacheName(), cacheConfig.getNullValueExpireTimeSeconds(), cacheConfig.getNullValueMaxSize(), cacheConfig.getNullValueClearPeriodSeconds());
        }
    }

    @Override
    public String getCacheType() {
        return CacheType.GUAVA.name().toLowerCase();
    }

    @Override
    public Cache<Object, Object> getActualCache() {
        return this.guavaCache;
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
        return this.guavaCache instanceof LoadingCache && null != this.cacheLoader;
    }

    @Override
    public Object get(Object key) {
        if (isLoadingCache()) {
            try {
                // 如果是refreshAfterWrite策略，则只会阻塞加载数据的线程，其他线程返回旧值（如果是异步加载，则所有线程都返回旧值）
                Object value = ((LoadingCache) this.guavaCache).get(key);
                logger.debug("GuavaCache LoadingCache.get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
                return fromStoreValue(value);
            } catch (ExecutionException e) {
                throw new IllegalStateException("GuavaCache LoadingCache.get cache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
            }
        }
        return fromStoreValue(this.guavaCache.getIfPresent(key));
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (isLoadingCache()) {
            // 将Callable设置到自定义CacheLoader中，以便在load()中执行具体的业务方法来加载数据
            this.cacheLoader.addValueLoader(key, valueLoader);

            Object value = this.get(key);
            return (T) fromStoreValue(value);
        }

        try {
            // 同步加载数据，仅一个线程加载数据，其他线程均阻塞
            Object value = this.guavaCache.get(key, () -> {
                LoadFunction loadFunction = new LoadFunction(this.getInstanceId(), this.getCacheType(), this.getCacheName(),
                        null, this.getCacheSyncPolicy(), ValueLoaderWarpper.newInstance(this.getCacheName(), key, valueLoader), this.isAllowNullValues(), this.nullValueCache);
                return loadFunction.apply(key);
            });
            logger.debug("GuavaCache Cache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return (T) fromStoreValue(value);
        } catch (ExecutionException e) {
            throw new IllegalStateException("GuavaCache Cache.get(key, callable) cache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        guavaCache.put(key, toStoreValue(value));
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_REFRESH));
        }
    }

    @Override
    public void evict(Object key) {
        logger.debug("GuavaCache evict cache, cacheName={}, key={}", this.getCacheName(), key);
        guavaCache.invalidate(key);
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR));
        }
    }

    @Override
    public void clear() {
        logger.debug("GuavaCache clear cache, cacheName={}", this.getCacheName());
        guavaCache.invalidateAll();
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(null, CacheConsts.CACHE_CLEAR));
        }
    }

    @Override
    public boolean isExists(Object key) {
        boolean rslt = guavaCache.asMap().containsKey(key);
        logger.debug("[GuavaCache] key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), key, rslt);
        return rslt;
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("GuavaCache clear local cache, cacheName={}, key={}", this.getCacheName(), key);
        if (key == null) {
            guavaCache.invalidateAll();
        } else {
            guavaCache.invalidate(key);
        }
    }

    @Override
    public void refresh(Object key) {
        if (isLoadingCache()) {
            logger.debug("GuavaCache refresh cache, cacheName={}, key={}", this.getCacheName(), key);
            ((LoadingCache) guavaCache).refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) guavaCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("GuavaCache refreshAll cache, cacheName={}, key={}", this.getCacheName(), key);
                loadingCache.refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(Object key) {
        if (isLoadingCache()) {
            logger.debug("GuavaCache refreshExpireCache, cacheName={}, key={}", this.getCacheName(), key);
            try {
                // 通过LoadingCache.get(key)来刷新过期缓存
                ((LoadingCache) guavaCache).get(key);
            } catch (ExecutionException e) {
                logger.error("GuavaCache refreshExpireCache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
            }
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) guavaCache;
            Object value = null;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("[GuavaCache] refreshAllExpireCache, cacheName={}, key={}", this.getCacheName(), key);
                try {
                    value = loadingCache.get(key);// 通过LoadingCache.get(key)来刷新过期缓存

                    /*if (null == value) {
                        continue;
                    }
                    if (value instanceof NullValue) {
                        if (null == nullValueCache) {
                            continue;
                        }
                        // getIfPresent 触发淘汰
                        Object nullValue = nullValueCache.getIfPresent(key);
                        if (null != nullValue) {
                            continue;
                        }
                        logger.info("[GuavaCache] refreshAllExpireCache invalidate NullValue, cacheName={}, key={}", this.getCacheName(), key);
                        loadingCache.invalidate(key);
                        if (null != cacheSyncPolicy) {
                            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR));
                        }
                    }*/
                } catch (ExecutionException e) {
                    logger.error("[GuavaCache] refreshAllExpireCache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
                }
            }
            if (null != nullValueCache) {
                logger.debug("[GuavaCache] refreshAllExpireCache number of NullValue, cacheName={}, size={}", this.getCacheName(), nullValueCache.asMap().size());
            }
        }
    }

    private CacheMessage createMessage(Object key, String optType) {
        return new CacheMessage()
                .setInstanceId(this.getInstanceId())
                .setCacheType(this.getCacheType())
                .setCacheName(this.getCacheName())
                .setKey(key)
                .setOptType(optType);
    }

}
