package com.coy.l2cache.cache;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.exception.RedisTrylockFailException;
import com.coy.l2cache.util.SpringCacheExceptionUtil;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redisson Cache
 * <p>
 * 注意：
 * 基于Redisson的RMapCache的缓存淘汰
 * 目前的Redis自身并不支持散列（Hash）当中的元素淘汰，因此所有过期元素都是通过org.redisson.EvictionScheduler实例来实现定期清理的。
 * 为了保证资源的有效利用，每次运行最多清理300个过期元素。
 * 任务的启动时间将根据上次实际清理数量自动调整，间隔时间趋于1秒到1小时之间。
 * 比如该次清理时删除了300条元素，那么下次执行清理的时间将在1秒以后（最小间隔时间）。
 * 一旦该次清理数量少于上次清理数量，时间间隔将增加1.5倍。
 * <p>
 * 如果应用被关掉，则redis中的数据一直存在，不会被redis淘汰汰。
 *
 * @author chenck
 * @date 2020/7/3 13:59
 */
public class RedissonCache extends AbstractAdaptingCache implements Level2Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedissonCache.class);

    /**
     * redis config
     */
    private final CacheConfig.Redis redis;

    /**
     * L2 Redisson MapCache 含元素淘汰功能
     * 注：保留元素的插入顺序
     */
    private RMapCache<Object, Object> mapCache;

    /**
     * L2 Redisson Map 无元素淘汰功能
     * 注：保留元素的插入顺序
     */
    private final RMap<Object, Object> map;

    public RedissonCache(String cacheName, CacheConfig cacheConfig, RMap<Object, Object> map) {
        super(cacheName, cacheConfig);
        this.redis = cacheConfig.getRedis();
        this.map = map;
        if (map instanceof RMapCache) {
            this.mapCache = (RMapCache<Object, Object>) map;
        }
    }

    @Override
    public long getExpireTime() {
        return redis.getExpireTime();
    }

    @Override
    public Object buildKey(Object key) {
        return key;
    }

    @Override
    public String getCacheType() {
        return CacheType.REDIS.name().toLowerCase();
    }

    @Override
    public RMap<?, ?> getActualCache() {
        return this.map;
    }

    @Override
    public Object get(Object key) {
        Object value = map.get(buildKey(key));
        logger.debug("[RedisCache] get cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
        return fromStoreValue(value);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = this.get(key);
        if (null == value) {
            return null;
        }
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("[RedisCache] Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = map.get(buildKey(key));
        if (value != null) {
            logger.debug("[RedisCache] get(key, callable) from redis, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return (T) fromStoreValue(value);
        }
        if (null == valueLoader) {
            logger.debug("[RedisCache] get(key, callable) callable is null, return null, cacheName={}, key={}", this.getCacheName(), key);
            return null;
        }
        RLock lock = null;
        if (redis.isLock()) {
            // 增加分布式锁，集群环境下同一时刻只会有一个加载数据的线程，解决ABA的问题，保证一级缓存二级缓存数据的一致性
            lock = map.getLock(key);
            if (redis.isTryLock()) {
                if (!lock.tryLock()) {
                    // 高并发场景下，拦截一部分请求将其快速失败，保证性能
                    logger.warn("[RedisCache] 重复请求, get(key, callable) tryLock fastfail, return null, cacheName={}, key={}", this.getCacheName(), key);
                    throw new RedisTrylockFailException("重复请求 tryLock fastfail, key=" + key);
                }
            } else {
                lock.lock();
            }
        }
        try {
            if (redis.isLock()) {
                value = map.get(key);
            }
            if (value == null) {
                logger.debug("[RedisCache] rlock, load data from target method, cacheName={}, key={}, isLock={}", this.getCacheName(), key, redis.isLock());
                value = valueLoader.call();
                logger.debug("[RedisCache] rlock, cacheName={}, key={}, value={}, isLock={}", this.getCacheName(), key, value, redis.isLock());
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
        if (!isAllowNullValues() && value == null) {
            map.remove(buildKey(key));
            return;
        }

        value = toStoreValue(value);
        if (mapCache == null) {
            map.fastPut(buildKey(key), value);
            logger.info("[RedisCache] put cache, cacheName={}, key={}, value={}", this.getCacheName(), key, value);
            return;
        }
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);

        if (redis.getMaxIdleTime() > 0) {
            mapCache.fastPut(buildKey(key), value, expireTime, TimeUnit.MILLISECONDS, redis.getMaxIdleTime(), TimeUnit.MILLISECONDS);
            logger.info("[RedisCache] put cache, cacheName={}, key={}, value={}, expireTime={} ms, maxIdleTime={}", this.getCacheName(), key, value, expireTime, redis.getMaxIdleTime());
        } else {
            mapCache.fastPut(buildKey(key), value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("[RedisCache] put cache, cacheName={}, key={}, value={}, expireTime={} ms", this.getCacheName(), key, value, expireTime);
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (!isAllowNullValues() && value == null) {
            // 不允许为null，且cacheValue为null，则直接获取旧的缓存项并返回
            return this.get(key);
        }
        Object prevValue = null;
        if (mapCache == null) {
            prevValue = map.putIfAbsent(buildKey(key), toStoreValue(value));
            return fromStoreValue(prevValue);
        }
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);

        if (redis.getMaxIdleTime() > 0) {
            prevValue = mapCache.putIfAbsent(buildKey(key), toStoreValue(value), expireTime, TimeUnit.MILLISECONDS, redis.getMaxIdleTime(), TimeUnit.MILLISECONDS);
            logger.info("[RedisCache] putIfAbsent cache, cacheName={}, key={}, value={}, expireTime={} ms, maxIdleTime={}", this.getCacheName(), key, value, expireTime, redis.getMaxIdleTime());
        } else {
            prevValue = mapCache.putIfAbsent(buildKey(key), toStoreValue(value), expireTime, TimeUnit.MILLISECONDS);
            logger.info("[RedisCache] putIfAbsent cache, cacheName={}, key={}, value={}, expireTime={} ms", this.getCacheName(), key, value, expireTime);
        }
        return fromStoreValue(prevValue);
    }

    @Override
    public void evict(Object key) {
        logger.debug("[RedisCache] evict cache, cacheName={}, key={}", this.getCacheName(), key);
        map.fastRemove(buildKey(key));
    }

    @Override
    public void clear() {
        logger.debug("[RedisCache] clear all cache, cacheName={}", this.getCacheName());
        map.clear();
    }

    @Override
    public boolean isExists(Object key) {
        boolean rslt = map.containsKey(key);
        logger.debug("[RedisCache] key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), key, rslt);
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
            expireTime = 0;// 0表示无限存储
        }
        return expireTime;
    }

}
