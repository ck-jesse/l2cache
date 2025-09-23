package com.github.jesse.l2cache.builder;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.CaffeineCache;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.cache.expire.CacheExpiry;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.CustomCaffeineSpec;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.load.CustomCacheLoader;
import com.github.jesse.l2cache.util.ExpireTimeUtil;
import com.github.jesse.l2cache.util.pool.MdcForkJoinPool;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        // 构建 CacheSpec
        CacheSpec cacheSpec = this.parseSpec(cacheName);

        // 创建CustomCacheLoader
        // 保证一个CaffeineCache对应一个CacheLoader，也就是cacheName维度进行隔离
        CacheLoader customCacheLoader = CustomCacheLoader.newInstance(L2CacheConfig.INSTANCE_ID,
                CacheType.CAFFEINE.name().toLowerCase(), cacheName, cacheSpec.getMaxSize());
        customCacheLoader.setCacheSyncPolicy(this.getCacheSyncPolicy());
        customCacheLoader.setAllowNullValues(cacheConfig.isAllowNullValues());

        Cache<Object, Object> cache = this.buildActualCache(cacheName, cacheConfig, customCacheLoader,
                this.getExpiredListener(), this.getCacheExpiry());

        return new CaffeineCache(cacheName, cacheConfig, customCacheLoader, this.getCacheSyncPolicy(), cache);
    }

    @Override
    public CacheSpec parseSpec(String cacheName) {
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        this.buildCaffeineSpec(cacheName, cacheConfig.getCaffeine());

        CacheSpec cacheSpec = new CacheSpec();
        CustomCaffeineSpec customCaffeineSpec = customCaffeineSpecMap.get(cacheName);
        cacheSpec.setExpireTime(customCaffeineSpec.getExpireTime());
        cacheSpec.setMaxSize((int) customCaffeineSpec.getMaximumSize());
        return cacheSpec;
    }

    /**
     * 构建实际缓存对象
     */
    protected Cache<Object, Object> buildActualCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, CacheLoader cacheLoader,
                                                     CacheExpiredListener listener, CacheExpiry cacheExpiry) {
        // 解析spec
        this.buildCaffeineSpec(cacheName, cacheConfig.getCaffeine());

        Caffeine<Object, Object> cacheBuilder = defaultCacheBuilder;
        CustomCaffeineSpec customCaffeineSpec = customCaffeineSpecMap.get(cacheName);
        if (null != customCaffeineSpec) {
            cacheBuilder = customCaffeineSpec.toBuilder();
        }

        // 判断是否启用，获取缓存剩余过期时间策略
        if (cacheConfig.getCaffeine().isEnableCacheExpiry() && null != cacheExpiry) {
            // 设置默认剩余过期时间
            long defaultExpireTime = customCaffeineSpec.getExpireTime();
            cacheExpiry.setDefaultExpireTime(defaultExpireTime);

            // 获取过期策略设置
            String expireStrategy = customCaffeineSpec.getExpireStrategy();
            // 仅refreshAfterWrite，支持自定义过期策略
            if ("refreshAfterWrite".equalsIgnoreCase(expireStrategy)) {
                logger.info("[refreshAfterWrite] 启用获取L2中缓存剩余过期时间策略, cacheName={}, defaultExpireTime={}", cacheName, defaultExpireTime);

                // 设置自定义过期策略
                cacheBuilder.expireAfter(new Expiry<Object, Object>() {
                    /**
                     * @param currentTime 相对纳秒时间，类似 System.nanoTime() 返回相对的纳秒值只能用于计算时间间隔，不能直接转换为日期
                     */
                    @Override
                    public long expireAfterCreate(@NonNull Object key, @NonNull Object value, long currentTime) {
                        // 创建缓存时，设置新的过期时间（缓存过期后，走getOrLoad时，也属于创建缓存的场景，会走到该处）
                        return TimeUnit.MILLISECONDS.toNanos(cacheExpiry.getTtl(key, value));
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull Object key, @NonNull Object value, long currentTime, @NonNegative long currentDuration) {
                        // 更新缓存时，设置新的过期时间
                        long currentDurationMillis = TimeUnit.NANOSECONDS.toMillis(currentDuration);
                        if (logger.isDebugEnabled()) {
                            logger.debug("[expireAfterUpdate] key={}, currentDurationMillis={}ms, expireTimeStr={}", key, currentDurationMillis, ExpireTimeUtil.toStr(currentDurationMillis));
                        }
                        return TimeUnit.MILLISECONDS.toNanos(cacheExpiry.getTtl(key, value));
                    }

                    @Override
                    public long expireAfterRead(@NonNull Object key, @NonNull Object value, long currentTime, @NonNegative long currentDuration) {
                        // 读取缓存时，不改变过期时间，直接返回当前剩余时间
                        long currentDurationMillis = TimeUnit.NANOSECONDS.toMillis(currentDuration);
                        if (logger.isDebugEnabled()) {
                            logger.debug("[expireAfterRead] key={}, currentDurationMillis={}ms, expireTimeStr={}", key, currentDurationMillis, ExpireTimeUtil.toStr(currentDurationMillis));
                        }
                        return currentDuration;
                    }
                });
            } else {
                // 自定义过期策略 CacheExpiry，不能与 expiresAfterWrite和expiresAfterWrite 同时使用
                logger.info("[{}] 过期策略不能与自定义过期策略(CacheExpiry)同时使用, cacheName={}, defaultExpireTime={}", expireStrategy, cacheName, defaultExpireTime);
            }
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
    private String getSpec(String cacheName, L2CacheConfig.Caffeine caffeine) {
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
    private void buildCaffeineSpec(String cacheName, L2CacheConfig.Caffeine caffeine) {
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
