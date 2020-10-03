package com.coy.l2cache.cache;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.exception.RedisTrylockFailException;
import com.coy.l2cache.util.SpringCacheExceptionUtil;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redisson RBucket Cache
 * <p>
 * 由于基于Redisson的RMapCache的缓存淘汰机制在大量key过期时，存在一个bug，导致获取已过期但还未被删除的key的值时，返回为null，所以改造为使用RBucket来实现。
 * 数据结构从hash改造为String，一方面解决RMapCache的缓存淘汰问题，另一方面，解决热点key的问题。
 *
 * @author chenck
 * @date 2020/10/2 22:00
 */
public class RedissonRBucketCache extends AbstractAdaptingCache implements Level2Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedissonRBucketCache.class);

    private static final String SPLIT = ":";

    /**
     * redis config
     */
    private final CacheConfig.Redis redis;

    /**
     * RBucket String结构
     */
    private RedissonClient redissonClient;

    private RMap<Object, Object> map;

    public RedissonRBucketCache(String cacheName, CacheConfig cacheConfig, RedissonClient redissonClient) {
        super(cacheName, cacheConfig);
        this.redis = cacheConfig.getRedis();
        this.redissonClient = redissonClient;
        if (redis.isLock()) {
            map = redissonClient.getMap(cacheName);
        }
    }

    @Override
    public long getExpireTime() {
        return redis.getExpireTime();
    }

    @Override
    public Object buildKey(Object key) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        StringBuilder sb = new StringBuilder(this.getCacheName()).append(SPLIT);
        sb.append(key.toString());
        return sb.toString();
    }

    @Override
    public String getCacheType() {
        return CacheType.REDIS.name().toLowerCase();
    }

    @Override
    public RedissonClient getActualCache() {
        return this.redissonClient;
    }

    /**
     * 获取 RBucket 对象
     */
    private RBucket<Object> getBucket(Object key) {
        RBucket<Object> bucket = redissonClient.getBucket((String) buildKey(key));
        return bucket;
    }

    @Override
    public Object get(Object key) {
        Object value = getBucket(key).get();
        logger.debug("[RedissonRBucketCache] get cache, cacheName={}, key={}, value={}", this.getCacheName(), buildKey(key), value);
        return fromStoreValue(value);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = this.get(key);
        if (null == value) {
            return null;
        }
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("[RedissonRBucketCache] Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        RBucket<Object> bucket = getBucket(key);
        Object value = bucket.get();
        if (value != null) {
            logger.info("[RedissonRBucketCache] get(key, callable) from redis, cacheName={}, key={}, value={}", this.getCacheName(), buildKey(key), value);
            return (T) fromStoreValue(value);
        }
        if (null == valueLoader) {
            logger.warn("[RedissonRBucketCache] get(key, callable) callable is null, return null, cacheName={}, key={}", this.getCacheName(), buildKey(key));
            return null;
        }
        RLock lock = null;
        if (redis.isLock() && null != map) {
            // 增加分布式锁，集群环境下同一时刻只会有一个加载数据的线程，解决ABA的问题，保证一级缓存二级缓存数据的一致性
            lock = map.getLock(key);
            if (redis.isTryLock()) {
                if (!lock.tryLock()) {
                    // 高并发场景下，拦截一部分请求将其快速失败，保证性能
                    logger.warn("[RedissonRBucketCache] 重复请求, get(key, callable) tryLock fastfail, return null, cacheName={}, key={}", this.getCacheName(), buildKey(key));
                    throw new RedisTrylockFailException("重复请求 tryLock fastfail, key=" + buildKey(key));
                }
            } else {
                lock.lock();
            }
        }
        try {
            if (redis.isLock()) {
                value = bucket.get();
            }
            if (value == null) {
                logger.debug("[RedissonRBucketCache] rlock, load data from target method, cacheName={}, key={}, isLock={}", this.getCacheName(), buildKey(key), redis.isLock());
                value = valueLoader.call();
                logger.debug("[RedissonRBucketCache] rlock, cacheName={}, key={}, value={}, isLock={}", this.getCacheName(), buildKey(key), value, redis.isLock());
                this.put(key, value);
            }
        } catch (Exception ex) {
            // 将异常包装spring cache异常
            throw SpringCacheExceptionUtil.warpper(key, valueLoader, ex);
        } finally {
            if (null != lock) {
                lock.unlock();
            }
        }
        return (T) fromStoreValue(value);
    }

    @Override
    public void put(Object key, Object value) {
        RBucket<Object> bucket = getBucket(key);
        if (!isAllowNullValues() && value == null) {
            bucket.delete();
            return;
        }

        value = toStoreValue(value);
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);
        if (expireTime > 0) {
            bucket.set(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("[RedissonRBucketCache] put cache, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, buildKey(key), value);
        } else {
            bucket.set(value);
            logger.info("[RedissonRBucketCache] put cache, cacheName={}, key={}, value={}", this.getCacheName(), buildKey(key), value);
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (!isAllowNullValues() && value == null) {
            // 不允许为null，且cacheValue为null，则直接获取旧的缓存项并返回
            return this.get(key);
        }
        RBucket<Object> bucket = getBucket(key);
        Object oldValue = bucket.get();
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);
        if (expireTime > 0) {
            boolean rslt = bucket.trySet(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("[RedissonRBucketCache] putIfAbsent cache, cacheName={}, expireTime={} ms, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), expireTime, rslt, buildKey(key), value, oldValue);
        } else {
            boolean rslt = bucket.trySet(value);
            logger.info("[RedissonRBucketCache] putIfAbsent cache, cacheName={}, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), rslt, buildKey(key), value, oldValue);
        }
        return fromStoreValue(oldValue);
    }

    @Override
    public void evict(Object key) {
        logger.info("[RedissonRBucketCache] evict cache, cacheName={}, key={}", this.getCacheName(), buildKey(key));
        getBucket(key).delete();
    }

    @Override
    public void clear() {
        logger.warn("[RedissonRBucketCache] not support clear all cache, cacheName={}", this.getCacheName());
    }

    @Override
    public boolean isExists(Object key) {
        boolean rslt = getBucket(key).isExists();
        logger.debug("[RedissonRBucketCache] key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), buildKey(key), rslt);
        return rslt;
    }

    /**
     * 过期时间处理
     * 如果是null值，则单独设置其过期时间
     */
    private long expireTimeDeal(Object value) {
        long expireTime = this.getExpireTime();
        if (value instanceof NullValue) {
            expireTime = TimeUnit.SECONDS.toMillis(this.getNullValueExpireTimeSeconds());
        }
        if (expireTime < 0) {
            expireTime = 0;
        }
        return expireTime;
    }

}
