package com.coy.l2cache.sync;

import com.coy.l2cache.Cache;
import com.coy.l2cache.content.CacheSupport;
import com.coy.l2cache.cache.Level1Cache;
import com.coy.l2cache.consts.CacheConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            if (this.cacheInstanceId.equalsIgnoreCase(message.getInstanceId())) {
                logger.debug("[CacheMessageListener][SyncCache] not deal cache instanceId is same, message={}", message.toString());
                return;
            }
            logger.info("[CacheMessageListener][SyncCache] message={}", message.toString());

            Level1Cache level1Cache = getLevel1Cache(message);
            if (null == level1Cache) {
                return;
            }
            if (CacheConsts.CACHE_REFRESH.equals(message.getOptType())) {
                level1Cache.refresh(message);
            } else {
                level1Cache.clearLocalCache(message);
            }
        } catch (Exception e) {
            logger.error("");
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
