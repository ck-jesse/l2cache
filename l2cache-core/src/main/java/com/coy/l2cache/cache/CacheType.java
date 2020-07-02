package com.coy.l2cache.cache;

/**
 * 缓存类型
 *
 * @author chenck
 * @date 2020/7/2 12:06
 */
public enum CacheType {
    NONE, COMPOSITE, CAFFEINE, GUAVA, REDIS,
    ;

    public static CacheType getCacheType(String type) {
        CacheType[] types = CacheType.values();
        for (CacheType cacheType : types) {
            if (cacheType.name().equalsIgnoreCase(type)) {
                return cacheType;
            }
        }
        return null;
    }
}
