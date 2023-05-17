package com.github.jesse.l2cache.builder;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.content.CustomCaffeineSpec;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.cache.CaffeineCache;
import com.github.jesse.l2cache.load.CustomCacheLoader;
import com.github.jesse.l2cache.util.pool.MdcForkJoinPool;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Caffeine Cache Builder
 *
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class CaffeineCacheBuilder extends AbstractCacheBuilder<CaffeineCache> {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineCacheBuilder.class);

    private static Caffeine<Object, Object> defaultCacheBuilder = Caffeine.newBuilder();

    private static Map<String, CustomCaffeineSpec> customCaffeineSpecMap = new HashMap<>();

    @Override
    public CaffeineCache build(String cacheName) {
        // 构建 CacheSpec
        CacheSpec cacheSpec = this.parseSpec(cacheName);

        // 创建CustomCacheLoader
        // 保证一个CaffeineCache对应一个CacheLoader，也就是cacheName维度进行隔离
        CacheLoader customCacheLoader = CustomCacheLoader.newInstance(this.getCacheConfig().getInstanceId(),
                CacheType.CAFFEINE.name().toLowerCase(), cacheName, cacheSpec.getMaxSize());
        customCacheLoader.setCacheSyncPolicy(this.getCacheSyncPolicy());
        customCacheLoader.setAllowNullValues(this.getCacheConfig().isAllowNullValues());

        Cache<Object, Object> cache = this.buildActualCache(cacheName, this.getCacheConfig(), customCacheLoader,
                this.getExpiredListener());

        return new CaffeineCache(cacheName, this.getCacheConfig(), customCacheLoader, this.getCacheSyncPolicy(), cache);
    }

    @Override
    public CacheSpec parseSpec(String cacheName) {
        this.buildCaffeineSpec(cacheName, this.getCacheConfig().getCaffeine());

        CacheSpec cacheSpec = new CacheSpec();
        CustomCaffeineSpec customCaffeineSpec = customCaffeineSpecMap.get(cacheName);
        cacheSpec.setExpireTime(customCaffeineSpec.getExpireTime());
        cacheSpec.setMaxSize((int) customCaffeineSpec.getMaximumSize());
        return cacheSpec;
    }

    /**
     * 构建实际缓存对象
     */
    protected Cache<Object, Object> buildActualCache(String cacheName, CacheConfig cacheConfig, CacheLoader cacheLoader,
                                                     CacheExpiredListener listener) {
        // 解析spec
        this.buildCaffeineSpec(cacheName, cacheConfig.getCaffeine());

        Caffeine<Object, Object> cacheBuilder = defaultCacheBuilder;
        CustomCaffeineSpec customCaffeineSpec = customCaffeineSpecMap.get(cacheName);
        if (null != customCaffeineSpec) {
            cacheBuilder = customCaffeineSpec.toBuilder();
        }

        if (null != listener) {
            cacheBuilder.removalListener((key, value, cause) -> {
                listener.onExpired(key, value, cause.name());
            });
        }

        if (cacheConfig.getCaffeine().isEnableMdcForkJoinPool()) {
            // 设置MdcForkJoinPool，替换默认的 ForkJoinPool.commonPool
            cacheBuilder.executor(MdcForkJoinPool.mdcCommonPool());
            logger.info("Caffeine enable MdcForkJoinPool, cacheName={}, mdcForkJoinPool={}", cacheName, MdcForkJoinPool.mdcCommonPool().toString());
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
        if (StrUtil.isBlank(cacheName)) {
            return caffeine.getDefaultSpec();
        }
        String spec = caffeine.getSpecs().get(cacheName);
        if (StrUtil.isBlank(spec)) {
            return caffeine.getDefaultSpec();
        }
        return spec;
    }

    /**
     * 获取自定义的 CaffeineSpec
     */
    private void buildCaffeineSpec(String cacheName, CacheConfig.Caffeine caffeine) {
        CustomCaffeineSpec customCaffeineSpec = customCaffeineSpecMap.get(cacheName);
        if (null != customCaffeineSpec) {
            return;
        }
        String spec = this.getSpec(cacheName, caffeine);
        if (StrUtil.isBlank(spec)) {
            throw new RuntimeException("please setting caffeine spec config");
        }
        customCaffeineSpecMap.put(cacheName, CustomCaffeineSpec.parse(spec));
    }

}
