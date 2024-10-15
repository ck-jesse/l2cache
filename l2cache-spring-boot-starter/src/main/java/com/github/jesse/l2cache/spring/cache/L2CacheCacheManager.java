package com.github.jesse.l2cache.spring.cache;

import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.cache.expire.DefaultCacheExpiredListener;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.spi.ServiceLoader;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * L2Cache Cache Manager
 *
 * @author chenck
 * @date 2020/7/6 11:18
 */
public class L2CacheCacheManager implements CacheManager {

    // 缓存Map<cacheName, Cache>
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

    private static final CacheExpiredListener defaultCacheExpiredListener = new DefaultCacheExpiredListener();

    /**
     * 是否支持动态创建缓存
     */
    private boolean dynamic = true;

    private final L2CacheConfig l2CacheConfig;

    private CacheExpiredListener expiredListener;

    private CacheSyncPolicy cacheSyncPolicy;

    private Object actualCacheClient;

    public L2CacheCacheManager(L2CacheConfig l2CacheConfig) {
        this(l2CacheConfig, null, defaultCacheExpiredListener);
    }

    public L2CacheCacheManager(L2CacheConfig l2CacheConfig, CacheSyncPolicy cacheSyncPolicy, CacheExpiredListener expiredListener) {
        this.dynamic = l2CacheConfig.isDynamic();
        this.l2CacheConfig = l2CacheConfig;
        if (null == expiredListener) {
            this.expiredListener = defaultCacheExpiredListener;
        } else {
            this.expiredListener = expiredListener;
        }
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = this.cacheMap.get(name);
        if (cache == null && this.dynamic) {
            synchronized (this.cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.l2CacheConfig, name);

                    cache = createL2CacheSpringCache(cacheConfig, name);
                    this.cacheMap.put(name, cache);
                }
            }
        }
        return cache;
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(this.cacheMap.keySet());
    }

    /**
     * Create a new L2CacheSpringCache instance for the specified cache name.
     *
     * @param cacheName the name of the cache
     * @return the Spring L2CacheSpringCache adapter (or a decorator thereof)
     */
    protected Cache createL2CacheSpringCache(L2CacheConfig.CacheConfig cacheConfig, String cacheName) {
        DefaultCacheExpiredListener expiredListener = new DefaultCacheExpiredListener();
        com.github.jesse.l2cache.Cache cache = this.getL2CacheInstance(cacheConfig.getCacheType(), cacheName, expiredListener);
        expiredListener.setCache(cache);
        return new L2CacheSpringCache(cacheName, cacheConfig, cache);
    }

    /**
     * get or create l2cache
     */
    private com.github.jesse.l2cache.Cache getL2CacheInstance(String cacheType, String cacheName, CacheExpiredListener expiredListener) {
        com.github.jesse.l2cache.Cache cache = CacheSupport.getCache(cacheType, cacheName);
        if (null != cache) {
            return cache;
        }
        // 基于SPI机制构建CacheBuilder
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, cacheType);
        cacheBuilder.setL2CacheConfig(this.l2CacheConfig);
        cacheBuilder.setExpiredListener(expiredListener);
        cacheBuilder.setCacheSyncPolicy(this.cacheSyncPolicy);
        cacheBuilder.setActualCacheClient(this.actualCacheClient);

        return CacheSupport.getCache(cacheType, cacheName, cacheBuilder);
    }

    public L2CacheConfig getL2CacheConfig() {
        return l2CacheConfig;
    }

    public CacheExpiredListener getExpiredListener() {
        return expiredListener;
    }

    public void setExpiredListener(CacheExpiredListener expiredListener) {
        this.expiredListener = expiredListener;
    }

    public CacheSyncPolicy getCacheSyncPolicy() {
        return cacheSyncPolicy;
    }

    public void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    public Object getActualCacheClient() {
        return actualCacheClient;
    }

    public void setActualCacheClient(Object actualCacheClient) {
        this.actualCacheClient = actualCacheClient;
    }
}
