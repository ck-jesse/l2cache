package com.coy.l2cache.schedule;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chenck
 * @date 2020/10/9 16:27
 */
@Slf4j
public class NullValueClearTask implements Runnable {

    /**
     * 缓存名字
     */
    private final String cacheName;

    private final Cache<Object, Integer> nullValueCache;

    public NullValueClearTask(String cacheName, Cache<Object, Integer> nullValueCache) {
        this.cacheName = cacheName;
        this.nullValueCache = nullValueCache;
    }

    @Override
    public void run() {
        long clearBeforeSize = nullValueCache.estimatedSize();
        try {
            // cleanUp 会触发 expireAfterWrite 的过期淘汰和基于大小的淘汰
            nullValueCache.cleanUp();
            if (clearBeforeSize > 0) {
                log.info("[NullValueClearTask] invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, nullValueCache.estimatedSize());
            } else {
                log.debug("[NullValueClearTask] invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, nullValueCache.estimatedSize());
            }
        } catch (Exception e) {
            log.error("[NullValueClearTask] invalidate NullValue error, cacheName=" + this.cacheName + ", clearBeforeSize=" + clearBeforeSize + ", clearAfterSize=" + nullValueCache.estimatedSize(), e);
        }
    }
}
