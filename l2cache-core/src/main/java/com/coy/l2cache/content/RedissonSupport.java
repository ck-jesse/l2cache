package com.coy.l2cache.content;

import com.coy.l2cache.CacheConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此 RedissonClient 容器的作用：将 RedissonClient 共享，以便在 RedisCacheSyncPolicy 和 RedissonCache 中重复使用。
 *
 * @author chenck
 * @date 2020/7/7 16:06
 */
public class RedissonSupport {

    private static final Map<String, RedissonClient> MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * 获取或创建缓存实例
     */
    public static RedissonClient getRedisson(CacheConfig cacheConfig) {
        RedissonClient redissonClient = MAP.get(cacheConfig.getInstanceId());
        if (null != redissonClient) {
            return redissonClient;
        }
        synchronized (lock) {
            redissonClient = MAP.get(cacheConfig.getInstanceId());
            if (null != redissonClient) {
                return redissonClient;
            }
            Config config = cacheConfig.getRedis().getRedissonConfig();
            if (null == config) {
                // 默认走本地redis，方便测试
                redissonClient = Redisson.create();
            } else {
                redissonClient = Redisson.create(config);
            }
            MAP.put(cacheConfig.getInstanceId(), redissonClient);
            return redissonClient;
        }
    }
}
