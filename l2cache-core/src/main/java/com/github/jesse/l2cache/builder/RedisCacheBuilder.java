package com.github.jesse.l2cache.builder;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSpec;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.content.RedissonSupport;
import com.github.jesse.l2cache.cache.RedissonRBucketCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class RedisCacheBuilder extends AbstractCacheBuilder<RedissonRBucketCache> {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheBuilder.class);

    @Override
    public RedissonRBucketCache build(String cacheName) {

        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        RedissonClient redissonClient = this.getRedissonClient(this.getL2CacheConfig());

        return this.buildActualCache(cacheName, cacheConfig, redissonClient);
    }

    /**
     * 获取 RedissonClient 实例
     * 注：主要目的是适配基于setActualCacheClient()扩展点设置的 RedissonClient，避免重复创建 RedissonClient
     */
    protected RedissonClient getRedissonClient(L2CacheConfig cacheConfig) {
        Object actualCacheClient = this.getActualCacheClient();
        if (null != actualCacheClient && actualCacheClient instanceof RedissonClient) {
            logger.info("multiplexing RedissonClient instance");
            return (RedissonClient) actualCacheClient;
        }

        logger.info("get or create RedissonClient instance by cache config");
        return RedissonSupport.getRedisson(cacheConfig);
    }


    protected RedissonRBucketCache buildActualCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, RedissonClient redissonClient) {
        L2CacheConfig.Redis redis = cacheConfig.getRedis();

        Long redisExpireTime = redis.getExpireTimeCacheNameMap().getOrDefault(cacheName, redis.getExpireTime());

        // 不使用一级缓存的过期时间来替换二级缓存的过期时间
        if (!cacheConfig.isUseL1ReplaceL2ExpireTime()) {
            logger.info("create a RedissonRBucketCache instance, 采用CacheConfig.Redis的值, cacheName={}, redisExpireTime={}", cacheName, redisExpireTime);
            return new RedissonRBucketCache(cacheName, cacheConfig, redissonClient);
        }

        // 使用一级缓存的过期时间来替换二级缓存的过期时间。简化缓存配置，且保证一级缓存和二级缓存的配置一致。
        CacheSpec cacheSpec = CacheSupport.getCacheSpec(cacheConfig.getComposite().getL1CacheType(), cacheName);
        if (null != cacheSpec) {
            // 覆盖CacheConfig.Redis的默认值
            if (cacheSpec.getExpireTime() <= 0) {
                redis.getExpireTimeCacheNameMap().put(cacheName, 0L);// 0 表示无过期时间
            } else {
                redis.getExpireTimeCacheNameMap().put(cacheName, cacheSpec.getExpireTime());
            }
            logger.info("create a RedissonRBucketCache instance, 采用一级缓存上的expireTime, 覆盖CacheConfig.Redis的默认值, cacheName={}, redisExpireTime={}, expireTime={}", cacheName, redisExpireTime, cacheSpec.getExpireTime());
        } else {
            logger.info("create a RedissonRBucketCache instance, 采用CacheConfig.Redis的值, 因为一级缓存的CacheSpec为空，cacheName={}, redisExpireTime={}", cacheName, redisExpireTime);
        }
        return new RedissonRBucketCache(cacheName, cacheConfig, redissonClient);
    }
}
