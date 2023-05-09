package com.github.jesse.l2cache.schedule;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

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
        // 每次执行设置trace_id，便于排查问题
        MDC.put(CacheConsts.SID, CacheConsts.PREFIX_CLEAR_NULL_VALUE + CacheConsts.SPLIT + RandomUtil.getUUID());
        try {
            if (clearBeforeSize <= 0) {
                return;
            }
            // cleanUp 会触发 expireAfterWrite 的过期淘汰和基于大小的淘汰
            nullValueCache.cleanUp();
            long clearAfterSize = nullValueCache.estimatedSize();
            if (clearBeforeSize > 0 && clearBeforeSize != clearAfterSize) {
                log.info("invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, clearAfterSize);
            } else {
                log.debug("invalidate NullValue, cacheName={}, clearBeforeSize={}, clearAfterSize={}", this.cacheName, clearBeforeSize, clearAfterSize);
            }
        } catch (Exception e) {
            log.error("invalidate NullValue error, cacheName=" + this.cacheName + ", clearBeforeSize=" + clearBeforeSize + ", clearAfterSize=" + nullValueCache.estimatedSize(), e);
        } finally {
            MDC.remove(CacheConsts.SID);
        }
    }
}
