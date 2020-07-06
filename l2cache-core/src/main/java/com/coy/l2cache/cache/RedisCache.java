package com.coy.l2cache.cache;

import com.coy.l2cache.cache.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cache
 *
 * @author chenck
 * @date 2020/6/29 16:37
 */
public class RedisCache extends AbstractAdaptingCache implements Level2Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    /**
     * L2 Redis
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * redis config
     */
    private final CacheConfig.Redis redis;

    public RedisCache(String cacheName, CacheConfig cacheConfig, RedisTemplate<Object, Object> redisTemplate) {
        super(cacheName, cacheConfig);
        this.redis = cacheConfig.getRedis();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long getExpireTime() {
        return redis.getExpireTime();
    }

    @Override
    public Object buildKey(Object key) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getCacheName()).append(":");
        if (redis.isUseKeyPrefix() && !StringUtils.isEmpty(redis.getKeyPrefix())) {
            sb.append(redis.getKeyPrefix()).append(":");
        }
        sb.append(key.toString());
        return sb.toString();
    }

    @Override
    public String getLevel() {
        return "2";
    }

    @Override
    public RedisTemplate<Object, Object> getActualCache() {
        return this.redisTemplate;
    }

    @Override
    public Object get(Object key) {
        return redisTemplate.opsForValue().get(buildKey(key));
    }

    @Override
    public synchronized <T> T get(Object key, Callable<T> valueLoader) {
        Object value = this.get(key);
        if (value != null) {
            return (T) value;
        }
        value = valueFromLoader(key, valueLoader);
        this.put(key, value);
        return (T) value;
    }

    @Override
    public void put(Object key, Object value) {
        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            throw new IllegalArgumentException(String.format("Cache '%s' does not allow 'null' values. ", getCacheName()));
        }
        redisTemplate.opsForValue().set(buildKey(key), cacheValue, this.getExpireTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        Object cacheValue = preProcessCacheValue(value);

        if (!isAllowNullValues() && cacheValue == null) {
            return get(key);// 不允许为null，且cacheValue为null，则直接获取缓存并返回
        }

        // 如果不存在，则设置，设置失败false表示已经存在
        boolean flag = this.redisTemplate.opsForValue().setIfAbsent(buildKey(key), cacheValue, this.getExpireTime(), TimeUnit.MILLISECONDS);
        if (!flag) {
            return this.get(key);// key存在，则取原值并返回
        }
        return null;
    }

    @Override
    public void evict(Object key) {
        logger.debug("RedisCache evict cache, cacheName={}, key={}", this.getCacheName(), key);
        this.redisTemplate.delete(buildKey(key));
    }

    @Override
    public void clear() {
        logger.debug("RedisCache clear all cache, cacheName={}", this.getCacheName());
        Set<Object> keys = redisTemplate.keys(this.getCacheName().concat(":"));
        for (Object key : keys) {
            redisTemplate.delete(key);
        }
    }

    /**
     * 预处理value
     */
    protected Object preProcessCacheValue(Object value) {
        if (value != null) {
            return value;
        }
        return isAllowNullValues() ? NullValue.INSTANCE : null;
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
