package com.github.jesse.l2cache.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.hotkey.AutoDetectHotKeyCache;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.load.LoadFunction;
import com.github.jesse.l2cache.load.ValueLoaderWarpper;
import com.github.jesse.l2cache.schedule.NullValueCacheClearTask;
import com.github.jesse.l2cache.schedule.NullValueClearSupport;
import com.github.jesse.l2cache.schedule.RefreshExpiredCacheTask;
import com.github.jesse.l2cache.schedule.RefreshSupport;
import com.github.jesse.l2cache.sync.CacheMessage;
import com.github.jesse.l2cache.util.CacheValueHashUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    private final L2CacheConfig.Guava guava;
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

    public GuavaCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, CacheLoader cacheLoader, CacheSyncPolicy cacheSyncPolicy,
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
                                // 计算缓存值的哈希，用于防止重复发送消息的控制
                                String valueHash = CacheValueHashUtil.calcHash(value);
                                this.cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR, "", valueHash));
                            }
                        }
                    })
                    .build();
            cacheLoader.setNullValueCache(this.nullValueCache);

            // 定期清理 NullValue
            NullValueClearSupport.getInstance().scheduleWithFixedDelay(new NullValueCacheClearTask(this.getCacheName(), this.nullValueCache), 5,
                    cacheConfig.getNullValueClearPeriodSeconds(), TimeUnit.SECONDS);

            logger.info("NullValueCache初始化成功, cacheName={}, expireTime={}s, maxSize={}, clearPeriodSeconds={}s", this.getCacheName(), cacheConfig.getNullValueExpireTimeSeconds(), cacheConfig.getNullValueMaxSize(), cacheConfig.getNullValueClearPeriodSeconds());
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
                logger.debug("LoadingCache.get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
                return fromStoreValue(value);
            } catch (ExecutionException e) {
                throw new IllegalStateException("[GuavaCache] LoadingCache.get cache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
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
            logger.debug("Cache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return (T) fromStoreValue(value);
        } catch (ExecutionException e) {
            throw new IllegalStateException("[GuavaCache] Cache.get(key, callable) cache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        guavaCache.put(key, toStoreValue(value));
        logger.info("put cache, cacheName={}, cacheSize={}, key={}, value={}", this.getCacheName(), guavaCache.size(), key, toStoreValue(value));
        if (null != cacheSyncPolicy) {
            // 计算缓存值的哈希，用于防止重复发送消息的控制
            String valueHash = CacheValueHashUtil.calcHash(value);
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_REFRESH_CLEAR, "put", valueHash));
        }
    }

    @Override
    public long size() {
        return guavaCache.asMap().size();
    }

    @Override
    public Set<Object> keys() {
        return guavaCache.asMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return guavaCache.asMap().values();
    }

    @Override
    public void evict(Object key) {
        logger.info("evict cache, cacheName={}, key={}", this.getCacheName(), key);
        guavaCache.invalidate(key);
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR, "evict"));
        }
    }

    @Override
    public void clear() {
        logger.info("clear cache, cacheName={}", this.getCacheName());
        guavaCache.invalidateAll();
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(null, CacheConsts.CACHE_CLEAR, "clear"));
        }
    }

    @Override
    public boolean isExists(Object key) {
        boolean rslt = guavaCache.asMap().containsKey(key);
        logger.debug("key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), key, rslt);
        return rslt;
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("clear local cache, cacheName={}, key={}", this.getCacheName(), key);
        if (key == null) {
            guavaCache.invalidateAll();
        } else {
            guavaCache.invalidate(key);
        }

        // 移除热key标识
        AutoDetectHotKeyCache.evit(this.getCacheName(), key);
    }

    @Override
    public void refresh(Object key) {
        if (isLoadingCache()) {
            logger.debug("refresh cache, cacheName={}, key={}", this.getCacheName(), key);
            ((LoadingCache) guavaCache).refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) guavaCache;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAll cache, cacheName={}, key={}", this.getCacheName(), key);
                loadingCache.refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(Object key) {
        if (isLoadingCache()) {
            logger.debug("refreshExpireCache, cacheName={}, key={}", this.getCacheName(), key);
            try {
                // 通过LoadingCache.get(key)来刷新过期缓存
                ((LoadingCache) guavaCache).get(key);
            } catch (ExecutionException e) {
                logger.error("refreshExpireCache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
            }
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) guavaCache;
            Object value = null;
            for (Object key : loadingCache.asMap().keySet()) {
                logger.debug("refreshAllExpireCache, cacheName={}, key={}", this.getCacheName(), key);
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
                        logger.info("refreshAllExpireCache invalidate NullValue, cacheName={}, key={}", this.getCacheName(), key);
                        loadingCache.invalidate(key);
                        if (null != cacheSyncPolicy) {
                            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR));
                        }
                    }*/
                } catch (ExecutionException e) {
                    logger.error("refreshAllExpireCache error, cacheName=" + this.getCacheName() + ", key=" + key, e);
                }
            }
            if (null != nullValueCache) {
                logger.debug("refreshAllExpireCache number of NullValue, cacheName={}, size={}", this.getCacheName(), nullValueCache.asMap().size());
            }
        }
    }


    private CacheMessage createMessage(Object key, String optType, String desc) {
        return createMessage(key, optType, desc, null);
    }

    private CacheMessage createMessage(Object key, String optType, String desc, String cacheValueHash) {
        return new CacheMessage()
                .setInstanceId(this.getInstanceId())
                .setCacheType(this.getCacheType())
                .setCacheName(this.getCacheName())
                .setKey(key)
                .setOptType(optType)
                .setDesc(desc)
                .setCacheValueHash(cacheValueHash);
    }

    @Override
    public <K, V> Map<K, V> batchGet(Map<K, Object> keyMap, boolean returnNullValueKey) {
        // 命中列表
        Map<K, V> hitMap = new HashMap<>();

        keyMap.forEach((key, cacheKey) -> {
            // 仅仅获取
            Object value = this.guavaCache.getIfPresent(cacheKey);
            logger.debug("batchGet cache, cacheName={}, cacheKey={}, value={}", this.getCacheName(), cacheKey, value);

            // value=null表示key不存在，则不将key包含在返回数据中
            if (value == null) {
                return;
            }
            V warpValue = (V) fromStoreValue(value);
            if (warpValue != null) {
                hitMap.put(key, warpValue);
                return;
            }
            // value=NullValue，且returnNullValueKey=true，则将key包含在返回数据中
            // 目的：batchGetOrLoad中调用batchGet时，可以过滤掉值为NullValue的key，防止缓存穿透到下一层
            if (returnNullValueKey) {
                hitMap.put(key, null);
                logger.warn("batchGet cache, cacheName={}, cacheKey={}, value={}, returnNullValueKey={}", this.getCacheName(), cacheKey, value, returnNullValueKey);
                return;
            }
        });
        logger.info("batchGet cache, cacheName={}, cacheKeyMap={}, hitMap={}", this.getCacheName(), keyMap.values(), hitMap);
        return hitMap;
    }
}
