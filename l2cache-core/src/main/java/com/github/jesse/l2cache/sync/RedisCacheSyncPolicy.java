package com.github.jesse.l2cache.sync;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.content.RedissonSupport;
import com.github.jesse.l2cache.util.pool.RunnableMdcWarpper;
import com.github.jesse.l2cache.util.pool.ThreadPoolSupport;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    /**
     * 定义静态线程池，避免高并发情况下，造成锁的竞争
     */
    private static final ThreadPoolExecutor poolExecutor = ThreadPoolSupport.getPool("publish_redis_msg");

    @Override
    public void connnect() {
        if (!start.compareAndSet(false, true)) {
            logger.info("already started");
            return;
        }
        RedissonClient redissonClient = getRedissonClient(this.getCacheConfig());
        this.topic = redissonClient.getTopic(this.getCacheConfig().getCacheSyncPolicy().getTopic());

        // 订阅主题
        this.topic.addListener(CacheMessage.class, (channel, msg) -> {
            // 执行消息监听器
            RedisCacheSyncPolicy.this.getCacheMessageListener().onMessage(msg);
        });
    }

    @Override
    public void publish(CacheMessage message) {
        poolExecutor.execute(new RunnableMdcWarpper(() -> {
            try {
                Long publishMsgPeriodMilliSeconds = this.getCacheConfig().getCaffeine().getPublishMsgPeriodMilliSeconds();
                RedissonClient redissonClient = getRedissonClient(this.getCacheConfig());
                RLock lock = redissonClient.getLock(buildLockKey(message));
                // 限制同一个key指定时间内只能发送一次消息，防止同一个key短时间内发送太多消息，给redis增加压力
                if (!lock.tryLock(0, publishMsgPeriodMilliSeconds, TimeUnit.MILLISECONDS)) {
                    logger.warn("trylock fail, no need to publish message, publishMsgPeriod={}ms, message={}", publishMsgPeriodMilliSeconds, message.toString());
                    return;
                }

                // 重入锁的数量(同一个线程可以重入)
                int lockHoldCount = lock.getHoldCount();
                if (lockHoldCount > 1) {
                    logger.warn("trylock succ, no need to publish message, publishMsgPeriod={}ms, lockHoldCount={}, message={}", publishMsgPeriodMilliSeconds, lockHoldCount, message.toString());
                    return;
                }

                long receivedMsgClientNum = this.topic.publish(message);
                logger.info("publish succ, cacheName={}, key={}, receivedMsgClientNum={}, message={}", message.getCacheName(), message.getKey(), receivedMsgClientNum, message.toString());
            } catch (Exception e) {
                logger.error("publish error, cacheName=" + message.getCacheName() + ", key=" + message.getKey(), e);
            }

        }, message));
    }

    @Override
    public void disconnect() {

    }

    protected RedissonClient getRedissonClient(CacheConfig cacheConfig) {
        Object actualClient = this.getActualClient();
        if (null != actualClient && actualClient instanceof RedissonClient) {
            if (logger.isDebugEnabled()) {
                logger.debug("use setting RedissonClient instance");
            }
            return (RedissonClient) actualClient;
        }

        logger.info("get or create RedissonClient instance by cache config");
        return RedissonSupport.getRedisson(cacheConfig);
    }

    private String buildLockKey(CacheMessage message) {
        // 从 "缓存名称:key" 改为 "缓存名称:key:操作类型"，让lockKey粒度更加精细化，避免不同操作类型(refresh/clear)互相影响
        return new StringBuilder("lock")
                .append(CacheConsts.SPLIT)
                .append(message.getCacheName())
                .append(CacheConsts.SPLIT)
                .append(message.getKey())
                .append(CacheConsts.SPLIT)
                .append(message.getOptType())
                .toString();
    }
}
