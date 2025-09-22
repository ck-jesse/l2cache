package com.github.jesse.l2cache.cache;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.content.NullValue;
import com.github.jesse.l2cache.hotkey.AutoDetectHotKeyCache;
import com.github.jesse.l2cache.schedule.NullValueCacheClearTask;
import com.github.jesse.l2cache.schedule.NullValueClearSupport;
import com.github.jesse.l2cache.schedule.RefreshExpiredCacheTask;
import com.github.jesse.l2cache.schedule.RefreshSupport;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.load.LoadFunction;
import com.github.jesse.l2cache.load.ValueLoaderWarpper;
import com.github.jesse.l2cache.sync.CacheMessage;
import com.github.jesse.l2cache.util.CacheValueHashUtil;
import com.github.jesse.l2cache.util.LogUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.jesse.l2cache.util.pool.MdcForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine Cache
 *
 * @author chenck
 * @date 2020/6/29 16:37
 */
public class CaffeineCache extends AbstractAdaptingCache implements Level1Cache {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineCache.class);

    /**
     * caffeine config
     */
    private final L2CacheConfig.Caffeine caffeine;
    /**
     * 缓存加载器，用于异步加载缓存
     */
    private final CacheLoader cacheLoader;
    /**
     * 缓存同步策略
     */
    private final CacheSyncPolicy cacheSyncPolicy;
    /**
     * L1 Caffeine Cache
     */
    private final Cache<Object, Object> caffeineCache;
    /**
     * 存放NullValue的key，用于控制NullValue对象的有效时间
     */
    private Cache<Object, Integer> nullValueCache;

    public CaffeineCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, CacheLoader cacheLoader, CacheSyncPolicy cacheSyncPolicy,
                         Cache<Object, Object> caffeineCache) {
        super(cacheName, cacheConfig);
        this.caffeine = cacheConfig.getCaffeine();
        this.cacheLoader = cacheLoader;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.caffeineCache = caffeineCache;

        if (this.caffeine.isAutoRefreshExpireCache()) {
            // 定期刷新过期的缓存
            RefreshSupport.getInstance(this.caffeine.getRefreshPoolSize())
                    .scheduleWithFixedDelay(new RefreshExpiredCacheTask(this), 5,
                            this.caffeine.getRefreshPeriod(), TimeUnit.SECONDS);
        }
        if (this.isAllowNullValues()) {
            this.nullValueCache = Caffeine.newBuilder()
                    .executor(new MdcForkJoinPool("RemoveNullValue"))
                    .expireAfterWrite(cacheConfig.getNullValueExpireTimeSeconds(), TimeUnit.SECONDS)
                    .maximumSize(cacheConfig.getNullValueMaxSize())
                    .removalListener((key, value, cause) -> {
                        logger.info("[NullValueCache] remove NullValue, removalCause={}, cacheName={}, key={}, value={}", cause, this.getCacheName(), key, value);
                        if (null != key) {
                            this.caffeineCache.invalidate(key);
                            if (null != this.cacheSyncPolicy) {
                                // 计算缓存值的哈希，用于防止重复发送消息的控制
                                String valueHash = CacheValueHashUtil.calcHash(value);
                                this.cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR, "RemoveNullValue", valueHash));
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
        return CacheType.CAFFEINE.name().toLowerCase();
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
            if (logger.isDebugEnabled()) {
                logger.debug("LoadingCache.get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            }
            return fromStoreValue(value);
        }
        return fromStoreValue(this.caffeineCache.getIfPresent(key));
    }

    @Override
    public Object getIfPresent(Object key) {
        return fromStoreValue(this.caffeineCache.getIfPresent(key));
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (isLoadingCache()) {
            // 将Callable设置到自定义CacheLoader中，以便在load()中执行具体的业务方法来加载数据
            this.cacheLoader.addValueLoader(key, valueLoader);

            Object value = ((LoadingCache) this.caffeineCache).get(key);
            if (logger.isDebugEnabled()) {
                logger.debug("LoadingCache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            }
            return (T) fromStoreValue(value);
        }

        // 同步加载数据，仅一个线程加载数据，其他线程均阻塞
        Object value = this.caffeineCache.get(key, new LoadFunction(this.getInstanceId(), this.getCacheType(), this.getCacheName(),
                null, this.getCacheSyncPolicy(), ValueLoaderWarpper.newInstance(this.getCacheName(), key, valueLoader), this.isAllowNullValues(), this.nullValueCache));
        if (logger.isDebugEnabled()) {
            logger.debug("Cache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
        }
        return (T) fromStoreValue(value);
    }

    @Override
    public void put(Object key, Object value) {
        this.put(key, value, true);
    }

    @Override
    public void put(Object key, Object value, boolean publishMessage) {
        if (!isAllowNullValues() && value == null) {
            caffeineCache.invalidate(key);
            return;
        }
        caffeineCache.put(key, toStoreValue(value));
        logger.info("put cache, cacheName={}, cacheSize={}, publishMsg={}, key={}, value={}", this.getCacheName(), caffeineCache.estimatedSize(), publishMessage, key, toStoreValue(value));

        // 允许null值，且值为空，则记录到nullValueCache，用于淘汰NullValue
        if (this.isAllowNullValues() && (value == null || value instanceof NullValue)) {
            if (null != nullValueCache) {
                nullValueCache.put(key, 1);
            }
        }

        // 主动put更新缓存，或从数据库重载缓存时，才发送同步消息
        if (publishMessage && null != cacheSyncPolicy) {
            // 计算缓存值的哈希，用于防止重复发送消息的控制
            String valueHash = CacheValueHashUtil.calcHash(value);
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_REFRESH_CLEAR, "put", valueHash));
        }
    }

    @Override
    public <V> void batchPut(Map<Object, V> dataMap) {
        this.batchPut(dataMap, true);
    }

    @Override
    public <V> void batchPut(Map<Object, V> dataMap, boolean publishMessage) {
        if (CollectionUtil.isEmpty(dataMap)) {
            return;
        }
        dataMap.forEach((key, value) -> {
            this.put(key, value, publishMessage);
        });
        logger.info("[{}] batchPut cache, cacheName={}, publishMsg={}, size={}", this.getClass().getSimpleName(), this.getCacheName(), publishMessage, dataMap.size());
    }

    @Override
    public long size() {
        return caffeineCache.asMap().size();
    }

    @Override
    public Set<Object> keys() {
        return caffeineCache.asMap().keySet();
    }

    @Override
    public Collection<Object> values() {
        return caffeineCache.asMap().values();
    }

    @Override
    public void evict(Object key) {
        logger.info("evict cache, cacheName={}, key={}", this.getCacheName(), key);
        Object value = this.getIfPresent(key);
        caffeineCache.invalidate(key);
        if (null != nullValueCache) {
            nullValueCache.invalidate(key);
        }

        // 移除热key标识
        AutoDetectHotKeyCache.evit(this.getCacheName(), key);

        if (null != cacheSyncPolicy) {
            // 计算缓存值的哈希，用于防止重复发送消息的控制
            String valueHash = CacheValueHashUtil.calcHash(value);
            cacheSyncPolicy.publish(createMessage(key, CacheConsts.CACHE_CLEAR, "evict", valueHash));
        }
    }

    @Override
    public void clear() {
        logger.info("clear cache, cacheName={}, deleteCount={}", this.getCacheName(), caffeineCache.asMap().size());
        caffeineCache.invalidateAll();
        if (null != nullValueCache) {
            nullValueCache.invalidateAll();
        }
        if (null != cacheSyncPolicy) {
            cacheSyncPolicy.publish(createMessage(null, CacheConsts.CACHE_CLEAR, "clear"));
        }
    }

    @Override
    public boolean isExists(Object key) {
        boolean rslt = caffeineCache.asMap().containsKey(key);
        if (logger.isDebugEnabled()) {
            logger.debug("key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), key, rslt);
        }
        return rslt;
    }

    @Override
    public void clearLocalCache(Object key) {
        logger.info("clear local cache, cacheName={}, key={}", this.getCacheName(), key);
        if (key == null) {
            caffeineCache.invalidateAll();
            if (null != nullValueCache) {
                nullValueCache.invalidateAll();
            }
        } else {
            caffeineCache.invalidate(key);
            if (null != nullValueCache) {
                nullValueCache.invalidate(key);
            }

            // 移除热key标识
            AutoDetectHotKeyCache.evit(this.getCacheName(), key);
        }
    }

    @Override
    public void refresh(Object key) {
        if (isLoadingCache()) {
            // LoadingCache.refresh() 为异步执行方法，若有同一个key的大量refresh请求，ForkJoinPool线程池处理不过来时，在线程池队列中会堆积大量的refresh任务，随着时间推移最终导致OOM。
            // 此处对valueLoader进行包装，通过一个key维度的原子性计数器，来控制同一时刻一个key只会存在一个refresh任务，而该任务在等待执行或者执行过程中，新来的refresh任务将会丢弃。
            ValueLoaderWarpper valueLoader = this.cacheLoader.getValueLoaderWarpper(key);
            if (null == valueLoader) {
                // 添加一个 valueLoader 为null的ValueLoaderWarpper对象
                // 一定程度上解决在 valueLoader 被gc回收后，若有大量refresh的请求，会堆积到ForkJoinPool的队列中的问题
                // valueLoader=null时，可以从redis加载数据
                LogUtil.log(logger, cacheConfig.getLogLevel(), "[CaffeineCache][refresh] add a null ValueLoader, cacheName={}, key={}", this.getCacheName(), key);
                this.cacheLoader.addValueLoader(key, null);
                valueLoader = this.cacheLoader.getValueLoaderWarpper(key);
            }
            int waitRefreshNum = valueLoader.getAndIncrement();
            if (waitRefreshNum > 0) {
                logger.info("[CaffeineCache][refresh] not do refresh, cacheName={}, key={}, waitRefreshNum={}", this.getCacheName(), key, waitRefreshNum);
                return;
            }
            LogUtil.log(logger, cacheConfig.getLogLevel(), "[CaffeineCache][refresh] do refresh, cacheName={}, key={}, waitRefreshNum={}", this.getCacheName(), key, waitRefreshNum);
            ((LoadingCache) caffeineCache).refresh(key);
        }
    }

    @Override
    public void refreshAll() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            for (Object key : loadingCache.asMap().keySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("refreshAll cache, cacheName={}, key={}", this.getCacheName(), key);
                }
                this.refresh(key);
            }
        }
    }

    @Override
    public void refreshExpireCache(Object key) {
        if (isLoadingCache()) {
            if (logger.isDebugEnabled()) {
                logger.debug("refreshExpireCache, cacheName={}, key={}", this.getCacheName(), key);
            }
            // 通过LoadingCache.get(key)来刷新过期缓存
            ((LoadingCache) caffeineCache).get(key);
        }
    }

    @Override
    public void refreshAllExpireCache() {
        if (isLoadingCache()) {
            LoadingCache loadingCache = (LoadingCache) caffeineCache;
            if (null != nullValueCache) {
                logger.info("refreshAllExpireCache, cacheName={}, size={}, NullValueSize={}, stats={}", this.getCacheName(), loadingCache.estimatedSize(), nullValueCache.estimatedSize(), loadingCache.stats());
            } else {
                logger.info("refreshAllExpireCache, cacheName={}, size={}, stats={}", this.getCacheName(), loadingCache.estimatedSize(), loadingCache.stats());
            }
            Object value = null;
            for (Object key : loadingCache.asMap().keySet()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("refreshAllExpireCache, cacheName={}, key={}", this.getCacheName(), key);
                }
                value = loadingCache.get(key);// 通过LoadingCache.get(key)来刷新过期缓存
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
            // refreshAfterWrite模式下：获取存在但已过期的缓存会触发load【此情况需特别注意，当valueLoader为null时需要处理好，否则存在缓存NullValue的可能】
            // refreshAfterWrite模式下：获取存在但未过期的缓存不会触发load
            // refreshAfterWrite模式下：获取不存在的缓存不会触发load
            // expireAfterWrite模式下：获取存在但已过期/存在但未过期/不存在的缓存，均不会触发load
            Object value = this.caffeineCache.getIfPresent(cacheKey);
            if (logger.isDebugEnabled()) {
                logger.debug("batchGet cache, cacheName={}, cacheKey={}, value={}", this.getCacheName(), cacheKey, value);
            }

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
                LogUtil.log(logger, cacheConfig.getLogLevel(), "[CaffeineCache] batchGet cache, cacheName={}, cacheKey={}, value={}, returnNullValueKey={}", this.getCacheName(), cacheKey, value, returnNullValueKey);
                return;
            }
        });
        LogUtil.log(logger, caffeine.getBatchGetLogLevel(), "[CaffeineCache] batchGet cache, cacheName={}, cacheKeyMapSize={}, hitMapSize={}, hitMap={}", this.getCacheName(), keyMap.size(), hitMap.size(), hitMap);
        return hitMap;
    }

}
