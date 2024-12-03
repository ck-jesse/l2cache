package com.github.jesse.l2cache.biz.mutil;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多redis实例场景的支持
 *
 * @author chenck
 * @date 2024/12/3 15:21
 */
public class CacheNameRedissonClientSupport {

    private static final Logger LOG = LoggerFactory.getLogger(CacheNameRedissonClientSupport.class);

    /**
     * 缓存名称与RedissonClient实例id的映射Map
     * <key, value> = <cacheName, RedissonClient实例id>
     */
    private static final Map<String, String> CACHE_NAME_REDISSON_CLIENT_MAP = new ConcurrentHashMap<>();

    /**
     * RedissonClient实例id与RedissonClient实例的映射Map
     * <key, value> = <RedissonClient实例id, RedissonClient>
     */
    private static final Map<String, RedissonClient> REDISSON_CLIENT_MAP = new ConcurrentHashMap<>();

    public static void putCacheNameRedissonClient(String cacheName, String redissonClientInstanceId) {
        CACHE_NAME_REDISSON_CLIENT_MAP.put(cacheName, redissonClientInstanceId);
    }

    public static String getCacheNameRedissonClient(String cacheName) {
        return CACHE_NAME_REDISSON_CLIENT_MAP.get(cacheName);
    }

    public static void putRedissonClient(String beanId, RedissonClient redissonClient) {
        REDISSON_CLIENT_MAP.put(beanId, redissonClient);
    }

    public static RedissonClient getRedissonClient(String cacheName) {
        String redissonClientInstanceId = CACHE_NAME_REDISSON_CLIENT_MAP.get(cacheName);
        if (null == redissonClientInstanceId) {
            return null;
        }
        RedissonClient redissonClient = REDISSON_CLIENT_MAP.get(redissonClientInstanceId);
        if (null != redissonClient) {
            LOG.info("[获取RedissonClient实例][多redis实例场景] 获取指定的RedissonClient实例, cacheName={}, instanceId={}", cacheName, redissonClientInstanceId);
        }
        return redissonClient;
    }

}
