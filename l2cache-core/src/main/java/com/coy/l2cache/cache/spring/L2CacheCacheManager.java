package com.coy.l2cache.cache.spring;

import com.coy.l2cache.cache.CacheExpiredListener;
import com.coy.l2cache.cache.DefaultCacheExpiredListener;
import com.coy.l2cache.cache.builder.CacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import com.coy.l2cache.cache.provider.CacheSupport;
import com.coy.l2cache.cache.spi.ServiceLoader;
import com.coy.l2cache.cache.sync.CacheSyncPolicy;
import com.coy.l2cache.context.spring.AbstractCaffeineRedisCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 组合缓存管理
 *
 * @author chenck
 * @date 2020/7/6 11:18
 */
public class L2CacheCacheManager implements CacheManager {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCaffeineRedisCacheManager.class);

    // 缓存Map<cacheName, Cache>
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

    /**
     * 是否支持动态创建缓存
     */
    private boolean dynamic = true;

    private final CacheConfig cacheConfig;

    private final CacheExpiredListener expiredListener;

    private final CacheSyncPolicy cacheSyncPolicy;

    public L2CacheCacheManager(CacheConfig cacheConfig) {
        this(cacheConfig, null, new DefaultCacheExpiredListener());
    }

    public L2CacheCacheManager(CacheConfig cacheConfig, CacheSyncPolicy cacheSyncPolicy, CacheExpiredListener expiredListener) {
        this.dynamic = cacheConfig.isDynamic();
        this.cacheConfig = cacheConfig;
        this.expiredListener = expiredListener;
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = this.cacheMap.get(name);
        if (cache == null && this.dynamic) {
            synchronized (this.cacheMap) {
                cache = this.cacheMap.get(name);
                if (cache == null) {
                    cache = createL2CacheSpringCache(cacheConfig.getCacheType(), name);
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
    protected Cache createL2CacheSpringCache(String cacheType, String cacheName) {
        com.coy.l2cache.cache.Cache l2cache = this.getL2CacheInstance(cacheType, cacheName);
        return new L2CacheSpringCache(cacheName, cacheConfig, l2cache);
    }

    /**
     * get or create l2cache
     */
    private com.coy.l2cache.cache.Cache getL2CacheInstance(String cacheType, String cacheName) {
        com.coy.l2cache.cache.Cache l2cache = CacheSupport.getCache(cacheType, cacheName);
        if (null != l2cache) {
            return l2cache;
        }
        // 基于SPI机制构建CacheBuilder
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, cacheType);
        cacheBuilder.setCacheConfig(this.cacheConfig);
        cacheBuilder.setExpiredListener(this.expiredListener);
        cacheBuilder.setCacheSyncPolicy(this.cacheSyncPolicy);

        return CacheSupport.getCache(cacheType, cacheName, cacheBuilder);
    }
}
