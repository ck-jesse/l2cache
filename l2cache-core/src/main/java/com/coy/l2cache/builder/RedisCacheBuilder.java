package com.coy.l2cache.builder;

import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.config.CacheConfig;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class RedisCacheBuilder extends AbstractCacheBuilder<RedissonCache> {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheBuilder.class);

    RedissonClient redissonClient;

    @Override
    public RedissonCache build(String cacheName) {
        CacheConfig.Redis redis = this.getCacheConfig().getRedis();

        RedissonClient redissonClient = this.getRedissonClient(redis);

        return this.buildActualCache(cacheName, this.getCacheConfig(), redissonClient);
    }

    /**
     * 获取 RedissonClient 实例
     * 注：主要目的是适配基于setActualCacheClient()扩展点设置的缓存Client实例，避免重复创建 RedissonClient
     */
    protected RedissonClient getRedissonClient(CacheConfig.Redis redis) {
        if (null != redissonClient) {
            return redissonClient;
        }
        Object actualCacheClient = this.getActualCacheClient();
        if (null != actualCacheClient) {
            logger.info("使用设置的缓存Client实例");
            redissonClient = (RedissonClient) actualCacheClient;
            return redissonClient;
        }
        synchronized (this) {
            actualCacheClient = this.getActualCacheClient();
            if (null != actualCacheClient) {
                logger.info("使用设置的缓存Client实例");
                redissonClient = (RedissonClient) actualCacheClient;
                return redissonClient;
            }

            logger.info("使用提供的配置创建Redisson实例");
            redissonClient = Redisson.create(redis.getRedissonConfig());
            setActualCacheClient(redissonClient);
            return redissonClient;
        }
    }


    protected RedissonCache buildActualCache(String cacheName, CacheConfig cacheConfig, RedissonClient redissonClient) {
        CacheConfig.Redis redis = this.getCacheConfig().getRedis();
        if (redis.getMaxIdleTime() == 0 && redis.getExpireTime() == 0 && redis.getMaxSize() == 0) {
            RMap<Object, Object> map = redissonClient.getMap(cacheName);
            logger.info("create a Redisson RMap instance, cacheName={}", cacheName);
            return new RedissonCache(cacheName, cacheConfig, map);
        }

        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(cacheName);
        mapCache.setMaxSize(redis.getMaxSize());
        logger.info("create a Redisson RMapCache instance, cacheName={}", cacheName);
        return new RedissonCache(cacheName, cacheConfig, mapCache);
    }
}
