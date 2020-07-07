package com.coy.l2cache.builder;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.content.RedissonSupport;
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

    @Override
    public RedissonCache build(String cacheName) {

        RedissonClient redissonClient = this.getRedissonClient(this.getCacheConfig());

        return this.buildActualCache(cacheName, this.getCacheConfig(), redissonClient);
    }

    /**
     * 获取 RedissonClient 实例
     * 注：主要目的是适配基于setActualCacheClient()扩展点设置的 RedissonClient，避免重复创建 RedissonClient
     */
    protected RedissonClient getRedissonClient(CacheConfig cacheConfig) {
        Object actualCacheClient = this.getActualCacheClient();
        if (null != actualCacheClient && actualCacheClient instanceof RedissonClient) {
            logger.info("use setting RedissonClient instance");
            return (RedissonClient) actualCacheClient;
        }

        logger.info("get or create RedissonClient instance by cache config");
        return RedissonSupport.getRedisson(cacheConfig);
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
