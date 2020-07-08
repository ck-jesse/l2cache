package com.coy.l2cache.sync;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.content.RedissonSupport;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 redis pubsub 的同步策略
 *
 * @author chenck
 * @date 2020/7/7 14:02
 */
public class RedisCacheSyncPolicy extends AbstractCacheSyncPolicy {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheSyncPolicy.class);

    AtomicBoolean start = new AtomicBoolean(false);
    private RTopic topic;

    @Override
    public void connnect() {
        if (!start.compareAndSet(false, true)) {
            logger.info("[RedisCacheSyncPolicy] already started");
            return;
        }
        RedissonClient redissonClient = getRedissonClient(this.getCacheConfig());
        this.topic = redissonClient.getTopic(this.getCacheConfig().getCacheSyncPolicy().getTopic());

        // 订阅主题
        this.topic.addListener(CacheMessage.class, (channel, msg) -> {
            logger.debug("[RedisCacheSyncPolicy] received a message, instanceId={}, cacheName={}, cacheType={}, optType={}, key={}",
                    msg.getInstanceId(), msg.getCacheName(), msg.getCacheType(), msg.getOptType(), msg.getKey());
            RedisCacheSyncPolicy.this.getCacheMessageListener().onMessage(msg);
        });
    }

    @Override
    public void publish(CacheMessage message) {
        try {
            logger.debug("[RedisCacheSyncPolicy] publish cache sync message, message={}", message.toString());
            long receivedMsgClientNum = this.topic.publish(message);
            logger.debug("[RedisCacheSyncPolicy] receivedMsgClientNum={}", receivedMsgClientNum);
        } catch (Exception e) {
            logger.error("[RedisCacheSyncPolicy] publish cache sync message error", e);
        }
    }

    @Override
    public void disconnect() {

    }

    protected RedissonClient getRedissonClient(CacheConfig cacheConfig) {
        Object actualClient = this.getActualClient();
        if (null != actualClient && actualClient instanceof RedissonClient) {
            logger.info("use setting RedissonClient instance");
            return (RedissonClient) actualClient;
        }

        logger.info("get or create RedissonClient instance by cache config");
        return RedissonSupport.getRedisson(cacheConfig);
    }

}
