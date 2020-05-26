package com.coy.l2cache.context;

import com.coy.l2cache.CaffeineRedisCacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存管理器
 *
 * @author chenck
 * @date 2020/4/28 19:54
 */
public class CaffeineRedisCacheManager extends AbstractCaffeineRedisCacheManager {

    private final Logger logger = LoggerFactory.getLogger(CaffeineRedisCacheManager.class);

    private boolean asyncCache;

    public CaffeineRedisCacheManager(RedisTemplate<Object, Object> redisTemplate, CaffeineRedisCacheProperties caffeineRedisCacheProperties) {
        super(redisTemplate, caffeineRedisCacheProperties);
        this.asyncCache = caffeineRedisCacheProperties.getCaffeine().isAsyncCache();
    }


    /**
     * Create a new CaffeineRedisCache instance for the specified cache name.
     *
     * @param name the name of the cache
     * @return the Spring CaffeineCache adapter (or a decorator thereof)
     */
    @Override
    protected Cache createCaffeineRedisCache(String name) {
        // 解析spec
        CustomCaffeineSpec customCaffeineSpec = getCaffeineRedisCacheProperties().getCaffeine().getCaffeineSpec(name);
        long expireTime = 0L;
        if (null != customCaffeineSpec) {
            expireTime = customCaffeineSpec.getExpireTime();
        }

        // CacheBuilder
        Caffeine<Object, Object> cacheBuilder = getDefaultCacheBuilder();
        if (null != customCaffeineSpec) {
            cacheBuilder = customCaffeineSpec.toBuilder();
        }

        // 移除监听器
        if (null != this.getRemovalListener()) {
            cacheBuilder.removalListener(this.getRemovalListener());
        }

        /*
        if ("refreshAfterWrite".equals(customCaffeineSpec.getExpireStrategy())) {
            cacheBuilder.expireAfter(new CustomExpiry(getRedisTemplate(), getCaffeineRedisCacheProperties(), name,
                    customCaffeineSpec.getExpireTime()));
        }*/

        CustomCacheLoader cacheLoader = new CustomCacheLoader();
        if (asyncCache) {
            logger.info("create a native async Caffeine Cache instance, name={}", name);
            return new AsyncCaffeineRedisCache(name, getRedisTemplate(), getCaffeineRedisCacheProperties(), expireTime,
                    cacheBuilder.buildAsync(cacheLoader), cacheLoader);
        } else {
            logger.info("create a native Caffeine Cache instance, name={}", name);
            return new CaffeineRedisCache(name, getRedisTemplate(), getCaffeineRedisCacheProperties(), expireTime,
                    cacheBuilder.build(cacheLoader), cacheLoader);
        }
    }

}
