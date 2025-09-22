package com.github.jesse.l2cache.sync;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
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
 * <p>
 * 1、redis 缓冲区作用
 * 客户端和服务端之间通信：用来暂存客户端发送的命令数据，或是服务端返回给客户端的数据结果。
 * 主从节点数据同步：用来暂存主节点接收的写命令和数据。
 * <p>
 * 2、redis 缓冲区配置
 * 使用该同步策略时，需要注意redis的缓冲区参数配置，避免缓冲区溢出，导致服务端关闭和客户端的连接
 * ## 配置示例
 * ## 消息订阅频道的客户端
 * client-output-buffer-limit pubsub 32mb 8mb 60
 * ## 配置解释
 * ## 第一个参数：代表分配给客户端的缓存大小，为0代表没有限制，32mb 表示缓冲区大小上限为 32mb，超过就断开客户端连接
 * ## 第二个参数：表示持续写入的最大内存，为0代表没有限制
 * ## 第三个参数：表示持续写入的最长时间，为0代表没有限制
 * <p>
 * 3、如何解决输出缓冲区溢出
 * 3.1 避免使用bigkey
 * 3.2 合理设置输出缓冲区上限、持续写入时间上限以及持续写入内存容量上限
 * <p>
 * 4、实战
 * 第一，从实际使用情况，以及压测的情况来看，暂时未发现关闭连接的情况。
 * 第二，由于l2cache中发送的消息体大小是可控的（仅包含key值，不包含value），所以不会出现输出bigkey的情况。
 * 因此，初步判断默认的输出缓存区是够用，但关注它总归是好的。
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
        RedissonClient redissonClient = getRedissonClient(this.getL2CacheConfig());
        this.topic = redissonClient.getTopic(this.getL2CacheConfig().getCacheSyncPolicy().getTopic());

        // 订阅主题
        this.topic.addListener(CacheMessage.class, (channel, msg) -> {
            // 执行消息监听器
            RedisCacheSyncPolicy.this.getCacheMessageListener().onMessage(msg);
        });
    }

    @Override
    public void publish(CacheMessage message) {
        poolExecutor.execute(new RunnableMdcWarpper(() -> {
            String lockKey = "";
            try {
                lockKey = buildLockKey(message);
                L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), message.getCacheName());

                Long publishMsgPeriodMilliSeconds = cacheConfig.getCaffeine().getPublishMsgPeriodMilliSeconds();
                RedissonClient redissonClient = getRedissonClient(this.getL2CacheConfig());
                RLock lock = redissonClient.getLock(lockKey);

                // 限制同一个key指定时间内只能发送一次消息，防止同一个key短时间内发送太多消息，给redis增加压力
                if (!lock.tryLock(0, publishMsgPeriodMilliSeconds, TimeUnit.MILLISECONDS)) {
                    logger.warn("无需发送消息(基于缓存值的MD5哈希,防止重复发送消息), publishMsgPeriod={}ms, lockKey={}, message={}", publishMsgPeriodMilliSeconds, lockKey, message.toString());
                    return;
                }

                // 重入锁的数量(同一个线程可以重入)
                int lockHoldCount = lock.getHoldCount();
                if (lockHoldCount > 1) {
                    logger.warn("无需发送消息(锁重入), publishMsgPeriod={}ms, lockHoldCount={}, lockKey={}, message={}", publishMsgPeriodMilliSeconds, lockHoldCount, lockKey, message.toString());
                    return;
                }

                long receivedMsgClientNum = this.topic.publish(message);
                logger.info("消息发送成功, cacheName={}, key={}, receivedMsgClientNum={}, lockKey={}, message={}", message.getCacheName(), message.getKey(), receivedMsgClientNum, lockKey, message.toString());
            } catch (Exception e) {
                logger.error("消息发送异常, cacheName=" + message.getCacheName() + ", key=" + message.getKey() + ", lockKey=" + lockKey, e);
            }

        }, message));
    }

    @Override
    public void disconnect() {

    }

    protected RedissonClient getRedissonClient(L2CacheConfig l2CacheConfig) {
        RedissonClient actualClient = this.getActualClient();
        if (null != actualClient) {
            if (logger.isDebugEnabled()) {
                logger.debug("[获取RedissonClient实例] 使用服务中已经存在的 RedissonClient instance");
            }
            return actualClient;
        }
        if (logger.isDebugEnabled()) {
            logger.info("[获取RedissonClient实例] get or create RedissonClient instance by cache config");
        }
        return RedissonSupport.getRedisson(l2CacheConfig);
    }

    private String buildLockKey(CacheMessage message) {
        // 从 "缓存名称:key" 改为 "缓存名称:key:操作类型"，让lockKey粒度更加精细化，避免不同操作类型(refresh/clear)互相影响
        StringBuilder lockKey = new StringBuilder("lock")
                .append(CacheConsts.SPLIT)
                .append(message.getCacheName())
                .append(CacheConsts.SPLIT)
                .append(message.getKey())
                .append(CacheConsts.SPLIT)
                .append(message.getOptType());

        // 如果有cacheValueHash，将其作为锁key的一部分，实现基于内容的防止重复发送消息
        // MD5哈希防重: 基于缓存值的MD5计算，避免相同内容的重复消息
        if (message.getCacheValueHash() != null) {
            lockKey.append(CacheConsts.SPLIT).append(message.getCacheValueHash());
        }
        return lockKey.toString();
    }
}
