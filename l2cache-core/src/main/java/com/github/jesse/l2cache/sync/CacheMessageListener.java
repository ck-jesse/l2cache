package com.github.jesse.l2cache.sync;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.util.pool.MdcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 缓存消息监听器
 *
 * @author chenck
 * @date 2020/7/7 15:11
 */
public class CacheMessageListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheMessageListener.class);

    private String cacheInstanceId;

    public CacheMessageListener(String cacheInstanceId) {
        this.cacheInstanceId = cacheInstanceId;
    }

    @Override
    public void onMessage(CacheMessage message) {
        Map<String, String> oldContext = MdcUtil.beforeExecution(message.getMdcContextMap());
        try {
            if (this.cacheInstanceId.equalsIgnoreCase(message.getInstanceId())) {
                logger.debug("[CacheMessageListener][SyncCache] don't need to process your own messages, currInstanceId={}, message={}", this.cacheInstanceId, message.toString());
                return;
            }
            logger.info("[CacheMessageListener][SyncCache] receive message, currInstanceId={}, instanceId={}, cacheName={}, cacheType={}, optType={}, key={}",
                    this.cacheInstanceId, message.getInstanceId(), message.getCacheName(), message.getCacheType(), message.getOptType(), message.getKey());

            Level1Cache level1Cache = getLevel1Cache(message);
            if (null == level1Cache) {
                return;
            }
            if (CacheConsts.CACHE_REFRESH.equals(message.getOptType())) {
                level1Cache.refresh(message.getKey());
            } else {
                level1Cache.clearLocalCache(message.getKey());
            }
        } catch (Exception e) {
            logger.error("[CacheMessageListener][SyncCache] deal message error, currInstanceId=" + this.cacheInstanceId, e);
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }

    /**
     * 获取 Level1Cache
     */
    private Level1Cache getLevel1Cache(CacheMessage message) {
        Cache cache = CacheSupport.getCache(message.getCacheType(), message.getCacheName());
        if (null == cache) {
            logger.warn("[CacheMessageListener][SyncCache] cache is not exists or not instanced, cacheType=" + message.getCacheType() + ", " +
                    "cacheName=" + message.getCacheName());
            return null;
        }
        if (!(cache instanceof Level1Cache)) {
            logger.warn("[CacheMessageListener][SyncCache] cache must be implements Level1Cache, cacheType=" + message.getCacheType() + " " + cache.getClass().getName());
            return null;
        }
        return (Level1Cache) cache;
    }
}
