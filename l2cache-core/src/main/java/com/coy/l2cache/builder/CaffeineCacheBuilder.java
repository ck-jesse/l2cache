package com.coy.l2cache.builder;

import com.coy.l2cache.cache.expire.CacheExpiredListener;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.load.CacheLoader;
import com.coy.l2cache.cache.CaffeineCache;
import com.coy.l2cache.load.CustomCacheLoader;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.content.CustomCaffeineSpec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class CaffeineCacheBuilder extends AbstractCacheBuilder<CaffeineCache> {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineCacheBuilder.class);

    private Caffeine<Object, Object> defaultCacheBuilder = Caffeine.newBuilder();

    @Override
    public CaffeineCache build(String cacheName) {
        // 创建CustomCacheLoader
        // 保证一个CaffeineCache对应一个CacheLoader，也就是cacheName维度进行隔离
        CacheLoader customCacheLoader = CustomCacheLoader.newInstance(this.getCacheConfig().getInstanceId(),
                CacheType.CAFFEINE.name().toLowerCase(), cacheName);
        customCacheLoader.setCacheSyncPolicy(this.getCacheSyncPolicy());

        Cache<Object, Object> cache = this.buildActualCache(cacheName, this.getCacheConfig(), customCacheLoader,
                this.getExpiredListener());

        return new CaffeineCache(cacheName, this.getCacheConfig(), customCacheLoader, this.getCacheSyncPolicy(), cache);
    }

    /**
     * 构建实际缓存对象
     */
    protected Cache<Object, Object> buildActualCache(String cacheName, CacheConfig cacheConfig, CacheLoader cacheLoader,
                                                     CacheExpiredListener listener) {
        // 解析spec
        CustomCaffeineSpec customCaffeineSpec = this.getCaffeineSpec(cacheName, cacheConfig.getCaffeine());

        Caffeine<Object, Object> cacheBuilder = defaultCacheBuilder;
        if (null != customCaffeineSpec) {
            cacheBuilder = customCaffeineSpec.toBuilder();
        }

        if (null != listener) {
            cacheBuilder.removalListener((key, value, cause) -> {
                listener.onExpired(key, value);
            });
        }
        if (null == cacheLoader) {
            logger.info("create a native Caffeine Cache instance, cacheName={}", cacheName);
            return cacheBuilder.build();
        }

        logger.info("create a native Caffeine LoadingCache instance, cacheName={}", cacheName);
        return cacheBuilder.build(key -> cacheLoader.load(key));
    }

    /**
     * 获取 spec
     */
    private String getSpec(String cacheName, CacheConfig.Caffeine caffeine) {
        if (!StringUtils.hasText(cacheName)) {
            return caffeine.getDefaultSpec();
        }
        String spec = caffeine.getSpecs().get(cacheName);
        if (!StringUtils.hasText(spec)) {
            return caffeine.getDefaultSpec();
        }
        return spec;
    }

    /**
     * 获取自定义的CaffeineSpec
     */
    private CustomCaffeineSpec getCaffeineSpec(String cacheName, CacheConfig.Caffeine caffeine) {
        String spec = this.getSpec(cacheName, caffeine);
        if (!StringUtils.hasText(spec)) {
            return null;
        }
        return CustomCaffeineSpec.parse(spec);
    }

}
