package com.jd.platform.hotkey.worker.cache;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每个APP都有一个自己的caffeine builder
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-16
 */
public class CaffeineCacheHolder {
    /**
     * key是appName，value是caffeine
     */
    private static final Map<String, Cache<String, Object>> CACHE_MAP = new ConcurrentHashMap<>();

    private static final String DEFAULT = "default";

    public static Cache<String, Object> getCache(String appName) {
        if (StrUtil.isEmpty(appName)) {
            if (CACHE_MAP.get(DEFAULT) == null) {
                Cache<String, Object> cache = CaffeineBuilder.buildAllKeyCache();
                CACHE_MAP.put(DEFAULT, cache);
            }
            return CACHE_MAP.get(DEFAULT);
        }
        if(CACHE_MAP.get(appName) == null) {
            Cache<String, Object> cache = CaffeineBuilder.buildAllKeyCache();
            CACHE_MAP.put(appName, cache);
        }
        return CACHE_MAP.get(appName);
    }

    /**
     * 清空某个app的缓存key
     */
    public static void clearCacheByAppName(String appName) {
        if(CACHE_MAP.get(appName) != null) {
            CACHE_MAP.get(appName).invalidateAll();
        }
    }

    /**
     * 获取每个app的caffeine容量
     */
    public static Map<String, Integer> getSize() {
        Map<String, Integer> map = new HashMap<>();
        for (String appName : CACHE_MAP.keySet()) {
            Cache cache = CACHE_MAP.get(appName);
            Map caffMap = cache.asMap();
//            long bytes = ObjectSizeCalculator.getObjectSize(caffMap);
            map.put(appName, caffMap.size());
        }
        return map;
    }

}
