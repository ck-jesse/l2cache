package com.coy.l2cache.content;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheBuilder;
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
     * Map<cacheType,Map<cacheName,Cache>>
     */
    private static final Map<String, ConcurrentHashMap<String, Cache>> CACHE_TYPE_CACHE_MAP = new ConcurrentHashMap<>(16);

    private static final Object newMapLock = new Object();
    private static final Object buildCacheLock = new Object();

    /**
     * 获取缓存实例
     */
    public static Cache getCache(String cacheType, String cacheName) {
        ConcurrentHashMap<String, Cache> cacheMap = CACHE_TYPE_CACHE_MAP.get(cacheType);
        if (null == cacheMap) {
            return null;
        }
        return cacheMap.get(cacheName);
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

        ConcurrentHashMap<String, Cache> cacheMap = CACHE_TYPE_CACHE_MAP.get(cacheType);
        if (null == cacheMap) {
            synchronized (newMapLock) {
                cacheMap = CACHE_TYPE_CACHE_MAP.get(cacheType);
                if (null == cacheMap) {
                    cacheMap = new ConcurrentHashMap<>();
                    CACHE_TYPE_CACHE_MAP.put(cacheType, cacheMap);
                }
            }
        }

        Cache cache = cacheMap.get(cacheName);
        if (null != cache) {
            return cache;
        }
        synchronized (buildCacheLock) {
            cache = cacheMap.get(cacheName);
            if (null != cache) {
                return cache;
            }
            cache = cacheBuilder.build(cacheName);
            cacheMap.put(cacheName, cache);
            return cache;
        }
    }

}
