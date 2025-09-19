package com.github.jesse.l2cache.hotkey;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.util.pool.MdcForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 用于缓存自动探测出来的热key
 *
 * @author chenck
 * @date 2024/9/29 19:24
 */
public class AutoDetectHotKeyCache {

    private static final Logger logger = LoggerFactory.getLogger(AutoDetectHotKeyCache.class);

    /**
     * 默认 10分钟
     */
    private static final Long expireAfterWrite = 10L;
    /**
     * 默认 10000
     */
    private static final Integer maxSize = 10000;

    // key = cacheName_key
    // value = true 表示本机识别到的热key，false 表示非本机识别到的热key
    private static final Cache<String, Boolean> hotKeyCache = Caffeine.newBuilder()
            .executor(new MdcForkJoinPool("HotKeyCache"))
            .removalListener((key, value, cause) -> {
                if (logger.isDebugEnabled()) {
                    logger.info("AutoDetect HotKey is Recycled, cause={}, key={}, value={}", cause, key, value);
                }
            })
            .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)
            .maximumSize(maxSize)
            .build();

    /**
     * get 热key
     */
    public static Boolean get(String cacheName, Object key) {
        return hotKeyCache.getIfPresent(buildKey(cacheName, key));
    }


    /**
     * set 热key
     *
     * @param isLocalRecognizedHotKey 是否本机识别的热key，true表示是本机识别，false表示非本机识别
     */
    public static void put(String cacheName, Object key, Boolean isLocalRecognizedHotKey) {
        hotKeyCache.put(buildKey(cacheName, key), isLocalRecognizedHotKey);
    }

    /**
     * evit 热key
     */
    public static void evit(String cacheName, Object key) {
        hotKeyCache.invalidate(buildKey(cacheName, key));
    }

    /**
     * 是否热key
     * 注：通过Cache.getIfPresent(key)来判断key是否存在，当value=null 则表示key不存在，即不是hotkey
     */
    public static Boolean isHotKey(String cacheName, Object key) {
        Boolean value = hotKeyCache.getIfPresent(buildKey(cacheName, key));
        // value=null 表示key不存在
        if (null == value) {
            return false;
        }
        return true;
    }

    /**
     * 构建key
     */
    public static String buildKey(String cacheName, Object key) {
        return cacheName + CacheConsts.SPLIT_UNDERLINE + key.toString();
    }

}
