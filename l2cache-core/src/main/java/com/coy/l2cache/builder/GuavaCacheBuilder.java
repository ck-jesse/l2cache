package com.coy.l2cache.builder;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSpec;
import com.coy.l2cache.cache.GuavaCache;
import com.coy.l2cache.cache.expire.CacheExpiredListener;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.content.CustomCaffeineSpec;
import com.coy.l2cache.content.CustomGuavaCacheBuilderSpec;
import com.coy.l2cache.load.CacheLoader;
import com.coy.l2cache.load.CustomCacheLoader;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Guava Cache Builder
 *
 * @author chenck
 * @date 2020/7/7 20:22
 */
public class GuavaCacheBuilder extends AbstractCacheBuilder<GuavaCache> {

    private static final Logger logger = LoggerFactory.getLogger(GuavaCacheBuilder.class);

    private static CacheBuilder<Object, Object> defaultCacheBuilder = CacheBuilder.newBuilder();

    private CacheBuilder<Object, Object> cacheBuilder;
    private CustomGuavaCacheBuilderSpec cacheBuilderSpec;

    @Override
    public GuavaCache build(String cacheName) {
        // 创建CustomCacheLoader
        // 保证一个GuavaCache对应一个CacheLoader，也就是cacheName维度进行隔离
        CacheLoader customCacheLoader = CustomCacheLoader.newInstance(this.getCacheConfig().getInstanceId(),
                CacheType.GUAVA.name().toLowerCase(), cacheName);
        customCacheLoader.setCacheSyncPolicy(this.getCacheSyncPolicy());

        Cache<Object, Object> cache = this.buildActualCache(cacheName, this.getCacheConfig(), customCacheLoader,
                this.getExpiredListener());

        return new GuavaCache(cacheName, this.getCacheConfig(), customCacheLoader, this.getCacheSyncPolicy(), cache);
    }

    @Override
    public CacheSpec parseSpec(String cacheName) {
        this.buildGuavaCacheSpec(cacheName, this.getCacheConfig().getGuava());

        CacheSpec cacheSpec = new CacheSpec();
        cacheSpec.setExpireTime(cacheBuilderSpec.getExpireTime());
        cacheSpec.setMaxSize(cacheBuilderSpec.getMaximumSize().intValue());
        return cacheSpec;
    }

    /**
     * 构建实际缓存对象
     */
    protected Cache<Object, Object> buildActualCache(String cacheName, CacheConfig cacheConfig, CacheLoader cacheLoader,
                                                     CacheExpiredListener listener) {
        // 解析spec
        this.buildGuavaCacheSpec(cacheName, this.getCacheConfig().getGuava());

        if (null != listener) {
            cacheBuilder.removalListener(notification -> {
                listener.onExpired(notification.getKey(), notification.getValue());
            });
        }
        if (null == cacheLoader) {
            logger.info("create a native Guava Cache instance, cacheName={}", cacheName);
            return cacheBuilder.build();
        }

        logger.info("create a native Guava LoadingCache instance, cacheName={}", cacheName);
        return cacheBuilder.build(new com.google.common.cache.CacheLoader<Object, Object>() {
            @Override
            public Object load(Object key) throws Exception {
                return cacheLoader.load(key);
            }
        });
    }

    /**
     * 获取 spec
     */
    private String getSpec(String cacheName, CacheConfig.Guava guava) {
        if (!StringUtils.hasText(cacheName)) {
            return guava.getDefaultSpec();
        }
        String spec = guava.getSpecs().get(cacheName);
        if (!StringUtils.hasText(spec)) {
            return guava.getDefaultSpec();
        }
        return spec;
    }

    /**
     * 获取自定义的 GuavaCacheSpec
     */
    private void buildGuavaCacheSpec(String cacheName, CacheConfig.Guava guava) {
        if (null != cacheBuilder) {
            return;
        }
        String spec = this.getSpec(cacheName, guava);
        if (!StringUtils.hasText(spec)) {
            throw new RuntimeException("please setting guava cache spec config");
        }
        cacheBuilderSpec = CustomGuavaCacheBuilderSpec.parse(spec);
        cacheBuilder = cacheBuilderSpec.toCacheBuilder();
        //cacheBuilder = CacheBuilder.from(spec);
    }
}
