package com.coy.l2cache.context;

import com.coy.l2cache.CaffeineRedisCacheProperties;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.listener.CacheMessage;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/4/28 19:55
 */
public abstract class AbstractCaffeineRedisCache extends AbstractValueAdaptingCache implements ExtendCache {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCaffeineRedisCache.class);

    /**
     * 缓存实例id
     */
    private final String instanceId;

    /**
     * 缓存名字
     */
    private final String name;

    @Nullable
    private final CacheLoader<Object, Object> cacheLoader;

    /**
     * RedisTemplate
     */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * Caffeine 属性配置
     */
    private final CaffeineRedisCacheProperties.Caffeine caffeine;

    /**
     * Redis 属性配置
     */
    private final CaffeineRedisCacheProperties.Redis redis;

    /**
     * 过期时间(ms)
     */
    private long expireTime = 0L;

    /**
     * Create a {@link AbstractCaffeineRedisCache} instance with the specified name and the
     * given internal {@link Cache} to use.
     *
     * @param name                         the name of the cache
     * @param redisTemplate                whether to accept and convert {@code null}values for this cache
     * @param caffeineRedisCacheProperties the properties for this cache
     */
    public AbstractCaffeineRedisCache(String name, RedisTemplate<Object, Object> redisTemplate,
                                      CaffeineRedisCacheProperties caffeineRedisCacheProperties, long expireTime,
                                      CacheLoader<Object, Object> cacheLoader) {
        super(caffeineRedisCacheProperties.isAllowNullValues());
        Assert.notNull(caffeineRedisCacheProperties.getInstanceId(), "Instance Id must not be null");
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(redisTemplate, "RedisTemplate must not be null");
        this.instanceId = caffeineRedisCacheProperties.getInstanceId();
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.caffeine = caffeineRedisCacheProperties.getCaffeine();
        this.redis = caffeineRedisCacheProperties.getRedis();
        this.expireTime = expireTime;
        this.cacheLoader = cacheLoader;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * @Cacheable(sync=false) 进入此方法
     * 并发场景：未做同步控制，所以存在多个线程同时加载数据的情况，即可能存在缓存击穿的情况
     */
    @Override
    @Nullable
    public ValueWrapper get(Object key) {
        if (isLoadingCache()) {
            Object value = get0(key);
            logger.debug("LoadingCache.get cache, cacheName={}, key={}, value={}", this.getName(), key, value);
            return toValueWrapper(value);
        }

        ValueWrapper value = super.get(key);
        logger.debug("Cache.get cache, cacheName={}, key={}, value={}", this.getName(), key, value);
        return value;
    }

    /**
     * @Cacheable(sync=true) 进入此方法
     * 并发场景：仅一个线程加载数据，其他线程均阻塞
     * 注：借助Callable入参，可以实现不同缓存调用不同的加载数据逻辑的目的。
     */
    @Override
    @Nullable
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (isLoadingCache()) {
            if (null != this.cacheLoader) {
                if (this.cacheLoader instanceof CustomCacheLoader) {
                    // 将Callable设置到自定义CacheLoader中，以便在load()中执行具体的业务方法来加载数据
                    CustomCacheLoader customCacheLoader = ((CustomCacheLoader) this.cacheLoader);
                    customCacheLoader.setExtendCache(this);
                    customCacheLoader.addValueLoader(key, valueLoader);
                }

                // 如果是refreshAfterWrite策略，则只会阻塞加载数据的线程，其他线程返回旧值（如果是异步加载，则所有线程都返回旧值）
                Object value = get0(key);
                logger.debug("LoadingCache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getName(), key, value);
                return (T) fromStoreValue(value);
            }
        }

        // 同步加载数据，仅一个线程加载数据，其他线程均阻塞
        Object value = get0(key, valueLoader);
        logger.debug("Cache.get(key, callable) cache, cacheName={}, key={}, value={}", this.getName(), key, value);
        return (T) fromStoreValue(value);
    }

    @Override
    @Nullable
    protected Object lookup(Object key) {
        Object value = lookup0(key);
        if (value != null) {
            logger.debug("lookup get cache from caffeine, cacheName={}, key={}", this.getName(), key);
            return value;
        }

        value = getRedisValue(key);

        if (value != null) {
            logger.debug("lookup get cache from redis and put in caffeine, cacheName={}, key={}", this.getName(), key);
            put0(key, value);
        }
        return value;
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        logger.debug("put cache, cacheName={}, key={}, value={}", this.getName(), key, value);
        Object userValue = toStoreValue(value);

        setRedisValue(key, userValue);

        cacheChangePush(key, CacheConsts.CACHE_REFRESH);

        put0(key, value);
    }

    @Override
    @Nullable
    public ValueWrapper putIfAbsent(Object key, @Nullable final Object value) {
        logger.debug("putIfAbsent cache, cacheName={}, key={}, value={}", this.getName(), key, value);
        Object userValue = toStoreValue(value);
        // 如果不存在，则设置
        boolean flag = this.redisTemplate.opsForValue().setIfAbsent(getRedisKey(key), userValue, getExpireTime(), TimeUnit.MILLISECONDS);

        if (!flag) {
            // key存在，则取原值并返回
            return toValueWrapper(getRedisValue(key));
        }

        cacheChangePush(key, CacheConsts.CACHE_REFRESH);

        put0(key, value);

        return toValueWrapper(userValue);
    }

    @Override
    public void evict(Object key) {
        logger.debug("evict cache, cacheName={}, key={}", this.getName(), key);
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        this.redisTemplate.delete(getRedisKey(key));

        cacheChangePush(key, CacheConsts.CACHE_CLEAR);

        evict0(key);
    }

    @Override
    public void clear() {
        logger.debug("clear all cache, cacheName={}", this.getName());
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        Set<Object> keys = redisTemplate.keys(this.name.concat(":"));
        for (Object key : keys) {
            redisTemplate.delete(key);
        }

        cacheChangePush(null, CacheConsts.CACHE_CLEAR);

        clear0();
    }

    @Override
    public long getExpireTime() {
        return expireTime;
    }

    @Override
    public Object getRedisKey(Object key) {
        return redis.getRedisKey(name, key);
    }

    @Override
    public Object getRedisValue(Object key) {
        return redisTemplate.opsForValue().get(getRedisKey(key));
    }

    @Override
    public void setRedisValue(Object key, Object value) {
        redisTemplate.opsForValue().set(getRedisKey(key), value, getExpireTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public Object toStoreValueWrap(@Nullable Object userValue) {
        return toStoreValue(userValue);
    }

    @Override
    public void cacheChangePush(Object key, String optType) {
        redisTemplate.convertAndSend(redis.getTopic(), new CacheMessage(this.instanceId, this.name, key, optType));
    }

    // the abstract method of operate native cache

    /**
     * 基于LoadingCache的get(key)
     */
    public abstract Object get0(Object key);

    public abstract Object get0(Object key, Callable<?> valueLoader);

    public abstract Object lookup0(Object key);

    public abstract void put0(Object key, Object value);

    public abstract void evict0(Object key);

    public abstract void clear0();
}
