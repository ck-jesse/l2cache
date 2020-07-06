//package com.coy.l2cache.listener;
//
//import com.coy.l2cache.consts.CacheConsts;
//import com.coy.l2cache.context.ExtendCacheManager;
//import com.coy.l2cache.sync.CacheMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.connection.MessageListener;
//import org.springframework.data.redis.core.RedisTemplate;
//
///**
// * 缓存消息监听
// *
// * @author chenck
// * @date 2020/4/29 10:56
// */
//public class CacheMessageListener implements MessageListener {
//    private final Logger logger = LoggerFactory.getLogger(CacheMessageListener.class);
//
//    private RedisTemplate<Object, Object> redisTemplate;
//
//    private ExtendCacheManager extendCacheManager;
//
//    public CacheMessageListener(RedisTemplate<Object, Object> redisTemplate, ExtendCacheManager extendCacheManager) {
//        super();
//        this.redisTemplate = redisTemplate;
//        this.extendCacheManager = extendCacheManager;
//    }
//
//    @Override
//    public void onMessage(Message message, byte[] pattern) {
//        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
//        if (extendCacheManager.currentCacheInstance(cacheMessage.getInstanceId())) {
//            logger.debug("[RedisCacheTopicMessage] not deal cache instanceId is same, instanceId={}, cacheName={}, key={}, optType={}",
//                    cacheMessage.getInstanceId(), cacheMessage.getCacheName(), cacheMessage.getKey(), cacheMessage.getOptType());
//            return;
//        }
//        logger.info("[RedisCacheTopicMessage] deal cache, instanceId={}, cacheName={}, key={}, optType={}",
//                cacheMessage.getInstanceId(), cacheMessage.getCacheName(), cacheMessage.getKey(), cacheMessage.getOptType());
//        if (CacheConsts.CACHE_REFRESH.equals(cacheMessage.getOptType())) {
//            extendCacheManager.refresh(cacheMessage.getCacheName(), cacheMessage.getKey());
//        } else {
//            extendCacheManager.clearLocalCache(cacheMessage.getCacheName(), cacheMessage.getKey());
//        }
//    }
//}
