package com.github.l2cache.schedule;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenck
 * @date 2020/10/9 16:27
 */
@Slf4j
public class NullValueCacheClearTask implements Runnable {

    /**
     * 缓存名字
     */
    private final String cacheName;

    private final Cache<Object, Integer> nullValueCache;

    public NullValueCacheClearTask(String cacheName, Cache<Object, Integer> nullValueCache) {
        this.cacheName = cacheName;
        this.nullValueCache = nullValueCache;
    }

    @Override
    public void run() {
        long clearBeforeSize = nullValueCache.estimatedSize();
        try {
            if (clearBeforeSize <= 0) {
                return;
            }
            // cleanUp 会触发 expireAfterWrite 的过期淘汰和基于大小的淘汰
            nullValueCache.cleanUp();
            if (clearBeforeSize > 0) {
                log.info("[NullValueCacheClearTask] invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, nullValueCache.estimatedSize());
            } else {
                log.debug("[NullValueCacheClearTask] invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, nullValueCache.estimatedSize());
            }
        } catch (Exception e) {
            log.error("[NullValueCacheClearTask] invalidate NullValue error, cacheName=" + this.cacheName + ", clearBeforeSize=" + clearBeforeSize + ", clearAfterSize=" + nullValueCache.estimatedSize(), e);
        }
    }
}
