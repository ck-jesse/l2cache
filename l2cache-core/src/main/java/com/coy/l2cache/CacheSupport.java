package com.coy.l2cache;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存容器
 *
 * @author chenck
 * @date 2020/7/2 15:15
 */
public class CacheSupport {

    /**
     * 缓存容器
     * <key,value>=<cacheType_cacheName, Cache>
     */
    private static final Map<String, Cache> CACHE_MAP = new ConcurrentHashMap<>(16);

    private static final Object lock = new Object();

    /**
     * 获取缓存实例
     */
    public static Cache getCache(String cacheType, String cacheName) {
        return CACHE_MAP.get(buildKey(cacheType, cacheName));
    }

    /**
     * 获取或创建缓存实例
     */
    public static Cache getCache(String cacheType, String cacheName, CacheBuilder cacheBuilder) {
        if (StringUtils.isEmpty(cacheType)) {
            throw new IllegalArgumentException("缓存类型不能为空");
        }
        if (StringUtils.isEmpty(cacheName)) {
            throw new IllegalArgumentException("缓存名称不能为空");
        }
        String key = buildKey(cacheType, cacheName);
        Cache cache = CACHE_MAP.get(key);
        if (null != cache) {
            return cache;
        }
        synchronized (lock) {
            cache = CACHE_MAP.get(key);
            if (null != cache) {
                return cache;
            }
            cache = cacheBuilder.build(cacheName);
            CACHE_MAP.put(key, cache);
            return cache;
        }
    }

    private static String buildKey(String cacheType, String cacheName) {
        return cacheType + "_" + cacheName;
    }
}
