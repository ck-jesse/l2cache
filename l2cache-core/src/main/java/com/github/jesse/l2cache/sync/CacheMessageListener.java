package com.github.jesse.l2cache.sync;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.hotkey.AutoDetectHotKeyCache;
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
                logger.debug("[SyncCache] don't need to process your own messages, currInstanceId={}, message={}", this.cacheInstanceId, message.toString());
                return;
            }
            logger.info("[SyncCache] receive message, currInstanceId={}, instanceId={}, cacheName={}, cacheType={}, optType={}, key={}, desc={}",
                    this.cacheInstanceId, message.getInstanceId(), message.getCacheName(), message.getCacheType(), message.getOptType(), message.getKey(), message.getDesc());

            // 缓存其他节点识别到的hotkey
            if (CacheConsts.CACHE_HOTKEY.equals(message.getOptType())) {
                AutoDetectHotKeyCache.put(message.getCacheName(), message.getKey(), Boolean.FALSE);
                return;
            }

            Level1Cache level1Cache = CacheSupport.getLevel1Cache(message.getCacheType(), message.getCacheName());
            if (null == level1Cache) {
                return;
            }

            // 其他节点通知清除本地缓存和hotkey(暂未启用，先简化处理)
            if (CacheConsts.CACHE_HOTKEY_EVIT.equals(message.getOptType())) {
                level1Cache.clearLocalCache(message.getKey());
                return;
            }

            // refresh 刷新缓存
            if (CacheConsts.CACHE_REFRESH.equals(message.getOptType())) {
                level1Cache.refresh(message.getKey());
                return;
            }

            // clear local cache
            level1Cache.clearLocalCache(message.getKey());

        } catch (Exception e) {
            logger.error("[SyncCache] deal message error, currInstanceId=" + this.cacheInstanceId, e);
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }

}
