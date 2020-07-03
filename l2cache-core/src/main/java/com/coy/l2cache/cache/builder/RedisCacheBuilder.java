package com.coy.l2cache.cache.builder;

import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.cache.config.CacheConfig;
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

        // 使用提供的配置创建Redisson实例
        if (null == redissonClient) {
            redissonClient = Redisson.create(redis.getRedissonConfig());
        }

        return this.build(cacheName, redis, redissonClient);
    }

    protected RedissonCache build(String cacheName, CacheConfig.Redis redis, RedissonClient redissonClient) {
        if (redis.getMaxIdleTime() == 0 && redis.getExpireTime() == 0 && redis.getMaxSize() == 0) {
            RMap<Object, Object> map = redissonClient.getMap(cacheName);
            logger.info("create a Redisson RMap instance, cacheName={}", cacheName);
            return new RedissonCache(cacheName, redis, map);
        }

        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(cacheName);
        mapCache.setMaxSize(redis.getMaxSize());
        logger.info("create a Redisson RMapCache instance, cacheName={}", cacheName);
        return new RedissonCache(cacheName, redis, mapCache);
    }
}
