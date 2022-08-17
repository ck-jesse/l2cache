package com.jd.platform.hotkey.client.cache;

import com.jd.platform.hotkey.client.core.rule.KeyRuleHolder;

/**
 * 用户可以自定义cache
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class CacheFactory {
    private static final LocalCache DEFAULT_CACHE = new DefaultCaffeineCache();

    /**
     * 创建一个本地缓存实例
     */
    public static LocalCache build(int duration) {
        return new CaffeineCache(duration);
    }

    public static LocalCache getNonNullCache(String key) {
        LocalCache localCache = getCache(key);
        if (localCache == null) {
            return DEFAULT_CACHE;
        }
        return localCache;
    }

    public static LocalCache getCache(String key) {
        return KeyRuleHolder.findByKey(key);
    }

}
