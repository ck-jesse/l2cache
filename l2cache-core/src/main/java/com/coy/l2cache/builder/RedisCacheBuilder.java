package com.coy.l2cache.builder;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSpec;
import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.content.CacheSupport;
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
        if (!redis.isAllowExpire()) {
            RMap<Object, Object> map = redissonClient.getMap(cacheName);// 无过期时间
            logger.info("create a Redisson RMap instance, cacheName={}, isAllowExpire={}", cacheName, redis.isAllowExpire());
            return new RedissonCache(cacheName, cacheConfig, map);
        }

        // 获取一级缓存对应CacheSpec
        // 二级缓存的过期时间和最大缓存数量从一级缓存上取，保证一级缓存和二级缓存的配置一致
        CacheSpec cacheSpec = CacheSupport.getCacheSpec(cacheConfig.getComposite().getL1CacheType(), cacheName);
        if (null != cacheSpec && (cacheSpec.getExpireTime() > 0 || cacheSpec.getMaxSize() > 0)) {
            RMapCache<Object, Object> mapCache = redissonClient.getMapCache(cacheName);// 有过期时间
            if (cacheSpec.getMaxSize() > 0) {
                mapCache.setMaxSize(cacheSpec.getMaxSize());
                redis.setMaxSize(cacheSpec.getMaxSize());// 覆盖默认值
            }
            redis.setExpireTime(cacheSpec.getExpireTime());// 覆盖默认值
            logger.info("create a Redisson RMapCache instance, 采用一级缓存上expireTime和maxSize, cacheName={}, cacheSpec={}", cacheName, cacheSpec);
            return new RedissonCache(cacheName, cacheConfig, mapCache);
        }

        // 默认的配置
        if (redis.getMaxIdleTime() <= 0 && redis.getExpireTime() <= 0 && redis.getMaxSize() <= 0) {
            RMap<Object, Object> map = redissonClient.getMap(cacheName);// 无过期时间
            logger.info("create a Redisson RMap instance default, cacheName={}", cacheName);
            return new RedissonCache(cacheName, cacheConfig, map);
        }

        RMapCache<Object, Object> mapCache = redissonClient.getMapCache(cacheName);// 有过期时间
        if (redis.getMaxSize() > 0) {
            mapCache.setMaxSize(redis.getMaxSize());
        }
        logger.info("create a Redisson RMapCache instance default, cacheName={}", cacheName);
        return new RedissonCache(cacheName, cacheConfig, mapCache);
    }
}
