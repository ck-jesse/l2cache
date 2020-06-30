package com.coy.l2cache.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Redis Cache
 *
 * @author chenck
 * @date 2020/6/29 16:37
 */
public class RedisCache implements L2Cache {

    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);
    /**
     * 缓存名字
     */
    private final String name;

    /**
     * L2 Redis
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisCache(String name, RedisTemplate<Object, Object> redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long getExpireTime() {
        return 0;
    }

    @Override
    public Object buildKey(Object key) {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getLevel() {
        return "2";
    }

    @Override
    public Object getActualCache() {
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
        redisTemplate.opsForValue().set(buildKey(key), value, getExpireTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        // 如果不存在，则设置，设置失败false表示已经存在
        boolean flag = this.redisTemplate.opsForValue().setIfAbsent(buildKey(key), value, getExpireTime(), TimeUnit.MILLISECONDS);

        if (!flag) {
            // key存在，则取原值并返回
            return this.get(key);
        }
        return null;
    }

    @Override
    public void evict(Object key) {
        logger.debug("evict cache, cacheName={}, key={}", this.getName(), key);
        this.redisTemplate.delete(buildKey(key));
    }

    @Override
    public void clear() {
        logger.debug("clear all cache, cacheName={}", this.getName());
        Set<Object> keys = redisTemplate.keys(this.name.concat(":"));
        for (Object key : keys) {
            redisTemplate.delete(key);
        }
    }

    private static <T> T valueFromLoader(Object key, Callable<T> valueLoader) {
        try {
            return valueLoader.call();
        } catch (Exception var3) {
            throw new Cache.ValueRetrievalException(key, valueLoader, var3);
        }
    }
}
