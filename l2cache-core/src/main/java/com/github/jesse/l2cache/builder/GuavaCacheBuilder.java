package com.github.jesse.l2cache.builder;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.GuavaCache;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.cache.expire.CacheExpiry;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.CustomGuavaCacheBuilderSpec;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.load.CustomCacheLoader;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        // 构建 CacheSpec
        CacheSpec cacheSpec = this.parseSpec(cacheName);

        // 创建CustomCacheLoader
        // 保证一个GuavaCache对应一个CacheLoader，也就是cacheName维度进行隔离
        CacheLoader customCacheLoader = CustomCacheLoader.newInstance(L2CacheConfig.INSTANCE_ID,
                CacheType.GUAVA.name().toLowerCase(), cacheName, cacheSpec.getMaxSize());
        customCacheLoader.setCacheSyncPolicy(this.getCacheSyncPolicy());
        customCacheLoader.setAllowNullValues(cacheConfig.isAllowNullValues());

        Cache<Object, Object> cache = this.buildActualCache(cacheName, cacheConfig, customCacheLoader,
                this.getExpiredListener(), this.getCacheExpiry());

        return new GuavaCache(cacheName, cacheConfig, customCacheLoader, this.getCacheSyncPolicy(), cache);
    }

    @Override
    public CacheSpec parseSpec(String cacheName) {
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        this.buildGuavaCacheSpec(cacheName, cacheConfig.getGuava());

        CacheSpec cacheSpec = new CacheSpec();
        cacheSpec.setExpireTime(cacheBuilderSpec.getExpireTime());
        cacheSpec.setMaxSize(cacheBuilderSpec.getMaximumSize().intValue());
        return cacheSpec;
    }

    /**
     * 构建实际缓存对象
     */
    protected Cache<Object, Object> buildActualCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, CacheLoader cacheLoader,
                                                     CacheExpiredListener listener, CacheExpiry cacheExpiry) {
        // 解析spec
        this.buildGuavaCacheSpec(cacheName, cacheConfig.getGuava());

        // 判断是否启用Redis TTL同步过期策略
        if (cacheConfig.getGuava().isEnableUseL2TTL() && null != cacheExpiry) {
            logger.info("Enable Redis TTL sync expiry strategy, cacheName={}", cacheName);
            // TODO guava 暂未发现设置自定义过期策略的方法
            // cacheExpiry.setDefaultExpireTime(0);
        }

        if (null != listener) {
            cacheBuilder.removalListener(notification -> {
                listener.onExpired(notification.getKey(), notification.getValue(), notification.getCause().name());
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
    private String getSpec(String cacheName, L2CacheConfig.Guava guava) {
        if (StrUtil.isBlank(cacheName)) {
            return guava.getDefaultSpec();
        }
        String spec = guava.getSpecs().get(cacheName);
        if (StrUtil.isBlank(spec)) {
            return guava.getDefaultSpec();
        }
        return spec;
    }

    /**
     * 获取自定义的 GuavaCacheSpec
     */
    private void buildGuavaCacheSpec(String cacheName, L2CacheConfig.Guava guava) {
        if (null != cacheBuilder) {
            return;
        }
        String spec = this.getSpec(cacheName, guava);
        if (StrUtil.isBlank(spec)) {
            throw new RuntimeException("please setting guava cache spec config");
        }
        cacheBuilderSpec = CustomGuavaCacheBuilderSpec.parse(spec);
        cacheBuilder = cacheBuilderSpec.toCacheBuilder();
        //cacheBuilder = CacheBuilder.from(spec);
    }
}
