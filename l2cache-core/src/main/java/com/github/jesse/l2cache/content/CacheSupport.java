package com.github.jesse.l2cache.content;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.CacheSpec;

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
    private static final Map<String, ConcurrentHashMap<String, Cache>> CACHE_TYPE_CACHE_MAP = new ConcurrentHashMap<>(32);
    /**
     * 缓存配置容器
     * Map<cacheType,Map<cacheName,CacheSpec>>
     */
    private static final Map<String, ConcurrentHashMap<String, CacheSpec>> CACHE_TYPE_CACHESPC_MAP = new ConcurrentHashMap<>(32);

    private static final Object newMapLock = new Object();
    private static final Object buildCacheLock = new Object();

    /**
     * 获取缓存实例 Cache
     */
    public static Cache getCache(String cacheType, String cacheName) {
        ConcurrentHashMap<String, Cache> cacheMap = CACHE_TYPE_CACHE_MAP.get(cacheType);
        if (null == cacheMap) {
            return null;
        }
        return cacheMap.get(cacheName);
    }

    /**
     * 获取缓存配置 CacheSpec
     */
    public static CacheSpec getCacheSpec(String cacheType, String cacheName) {
        ConcurrentHashMap<String, CacheSpec> cacheSpecMap = CACHE_TYPE_CACHESPC_MAP.get(cacheType);
        if (null == cacheSpecMap) {
            return null;
        }
        return cacheSpecMap.get(cacheName);
    }

    /**
     * 获取或创建缓存实例
     */
    public static Cache getCache(String cacheType, String cacheName, CacheBuilder cacheBuilder) {
        if (StrUtil.isEmpty(cacheType)) {
            throw new IllegalArgumentException("缓存类型不能为空");
        }
        if (StrUtil.isEmpty(cacheName)) {
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

            // 构建CacheSpec对象
            buildCacheSpec(cacheType, cacheName, cacheBuilder);

            // 构建Cache对象
            cache = cacheBuilder.build(cacheName);
            cacheMap.put(cacheName, cache);
            return cache;
        }
    }

    /**
     * 构建CacheSpec对象
     */
    private static void buildCacheSpec(String cacheType, String cacheName, CacheBuilder cacheBuilder) {
        ConcurrentHashMap<String, CacheSpec> cacheSpecMap = CACHE_TYPE_CACHESPC_MAP.get(cacheType);
        if (null == cacheSpecMap) {
            cacheSpecMap = CACHE_TYPE_CACHESPC_MAP.get(cacheType);
            if (null == cacheSpecMap) {
                cacheSpecMap = new ConcurrentHashMap<>();
                CACHE_TYPE_CACHESPC_MAP.put(cacheType, cacheSpecMap);
            }
        }
        // 构建 CacheSpec
        CacheSpec cacheSpec = cacheBuilder.parseSpec(cacheName);
        if (null != cacheSpec) {
            cacheSpecMap.put(cacheName, cacheSpec);
        }
    }

}
