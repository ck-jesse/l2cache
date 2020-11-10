package com.coy.l2cache.consts;

/**
 * 缓存范围
 * 可用于作为开关
 *
 * @author chenck
 * @date 2020/11/10 21:47
 */
public enum CacheRange {
    // 启用
    ENABLE,
    // 启用部分
    ENABLE_PART,
    // 停用
    DISABLE,
    // 停用部分
    DISABLE_PART,
    ;

    public static CacheRange getCacheRange(String range) {
        CacheRange[] ranges = CacheRange.values();
        for (CacheRange cacheRange : ranges) {
            if (cacheRange.name().equalsIgnoreCase(range)) {
                return cacheRange;
            }
        }
        return null;
    }
}
