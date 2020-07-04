package com.coy.l2cache.cache;

import com.coy.l2cache.cache.config.CacheConfig;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

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
public class RedissonCache extends AbstractAdaptingCache implements L2Cache {

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

    public RedissonCache(String cacheName, CacheConfig.Redis redis, RMap<Object, Object> map) {
        this(cacheName, false, redis, map);
    }

    public RedissonCache(String cacheName, boolean allowNullValues, CacheConfig.Redis redis, RMap<Object, Object> map) {
        super(cacheName, allowNullValues);
        this.redis = redis;
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
//        StringBuilder sb = new StringBuilder();
//        sb.append(this.cacheName).append(":");
//        if (redis.isUseKeyPrefix() && !StringUtils.isEmpty(redis.getKeyPrefix())) {
//            sb.append(redis.getKeyPrefix()).append(":");
//        }
//        sb.append(key.toString());
//        return sb.toString();
    }

    @Override
    public String getLevel() {
        return "2";
    }

    @Override
    public RMap<?, ?> getActualCache() {
        return this.map;
    }

    @Override
    public Object get(Object key) {
        Object value = map.get(buildKey(key));
        return fromStoreValue(value);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = this.get(key);
        if (null == value) {
            return null;
        }
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = this.get(key);
        if (value != null) {
            return (T) value;
        }
        RLock lock = map.getLock(key);
        lock.lock();
        try {
            value = map.get(key);
            if (value == null) {
                value = valueFromLoader(key, valueLoader);
                this.put(key, value);
            }
        } finally {
            lock.unlock();
        }
        return (T) fromStoreValue(value);
    }

    @Override
    public void put(Object key, Object value) {
        /*if (!isAllowNullValues() && value == null) {
            map.remove(buildKey(key));
            return;
        }*/

        value = toStoreValue(value);
        if (mapCache != null) {
            mapCache.fastPut(buildKey(key), value, this.getExpireTime(), TimeUnit.MILLISECONDS, redis.getMaxIdleTime(), TimeUnit.MILLISECONDS);
        } else {
            map.fastPut(buildKey(key), value);
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (!isAllowNullValues() && value == null) {
            return this.get(key);// 不允许为null，且cacheValue为null，则直接获取缓存并返回
        }
        Object prevValue = null;
        if (mapCache != null) {
            prevValue = mapCache.putIfAbsent(buildKey(key), toStoreValue(value), this.getExpireTime(), TimeUnit.MILLISECONDS, redis.getMaxIdleTime(),
                    TimeUnit.MILLISECONDS);
        } else {
            prevValue = map.putIfAbsent(buildKey(key), toStoreValue(value));
        }
        return fromStoreValue(prevValue);
    }

    @Override
    public void evict(Object key) {
        logger.debug("RedisCache evict cache, cacheName={}, key={}", this.getCacheName(), key);
        map.fastRemove(buildKey(key));
    }

    @Override
    public void clear() {
        logger.debug("RedisCache clear all cache, cacheName={}", this.getCacheName());
        map.clear();
    }

    /**
     *
     */
    private static <T> T valueFromLoader(Object key, Callable<T> valueLoader) {
        try {
            return valueLoader.call();
        } catch (Exception var3) {
            throw new Cache.ValueRetrievalException(key, valueLoader, var3);
        }
    }
}
