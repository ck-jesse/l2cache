package com.github.jesse.l2cache.cache;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.NullValue;
import com.github.jesse.l2cache.exception.RedisTrylockFailException;
import com.github.jesse.l2cache.load.ValueLoaderWarpper;
import com.github.jesse.l2cache.load.ValueLoaderWarpperTemp;
import com.github.jesse.l2cache.util.BiConsumerWrapper;
import com.github.jesse.l2cache.util.LogUtil;
import com.github.jesse.l2cache.util.RandomUtil;
import com.github.jesse.l2cache.util.SpringCacheExceptionUtil;
import com.google.common.collect.Lists;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * redis config
     */
    private final CacheConfig.Redis redis;

    /**
     * RBucket String结构
     */
    private RedissonClient redissonClient;

    /**
     * RMap 用于获取分布式锁
     */
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
        return redis.getExpireTimeCacheNameMap().getOrDefault(this.getCacheName(), redis.getExpireTime());
    }

    @Override
    public Object buildKey(Object key) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        StringBuilder cacheKey = new StringBuilder(this.getCacheName()).append(CacheConsts.SPLIT).append(key.toString());
        return cacheKey.toString();
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
     *
     * @param cacheKey 已经拼接好的缓存key
     */
    private RBucket<Object> getBucket(String cacheKey) {
        RBucket<Object> bucket = redissonClient.getBucket(cacheKey);
        return bucket;
    }

    @Override
    public Object get(Object key) {
        String cacheKey = (String) buildKey(key);
        Object value = getBucket(cacheKey).get();
        LogUtil.logDetailPrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] get cache, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
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
        String cacheKey = (String) buildKey(key);
        RBucket<Object> bucket = getBucket(cacheKey);
        Object value = bucket.get();
        if (value != null) {
            LogUtil.logDetailPrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] get(key, callable) from redis, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
            return (T) fromStoreValue(value);
        }
        if (null == valueLoader) {
            logger.info("get(key, callable) callable is null, value is null, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
            return null;
        }
        // 防止从redis中获取的value为null，同时valueLoader也为null的情况下，往redis中存了一个NullValue对象
        if (valueLoader instanceof ValueLoaderWarpperTemp) {
            ValueLoaderWarpperTemp valueLoaderWarpperTemp = ((ValueLoaderWarpperTemp) valueLoader);
            if (null == valueLoaderWarpperTemp.getValueLoader()) {
                logger.info("get(key, callable) ValueLoaderWarpperTemp.valueLoader is null, value is null, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
                return null;
            }
            if (valueLoaderWarpperTemp.getValueLoader() instanceof ValueLoaderWarpper
                    && null == ((ValueLoaderWarpper) valueLoaderWarpperTemp.getValueLoader()).getValueLoader()) {
                LogUtil.log(logger, cacheConfig.getLogLevel(), "[RedissonRBucketCache] get(key, callable) ValueLoaderWarpper.valueLoader is null, value is null, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
                return null;
            }
        }
        RLock lock = null;
        if (redis.isLock() && null != map) {
            // 增加分布式锁，集群环境下同一时刻只会有一个加载数据的线程，解决ABA的问题，保证一级缓存二级缓存数据的一致性
            lock = map.getLock(key);
            if (redis.isTryLock()) {
                if (!lock.tryLock()) {
                    // 高并发场景下，拦截一部分请求将其快速失败，保证性能
                    logger.warn("重复请求, get(key, callable) tryLock fastfail, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
                    throw new RedisTrylockFailException("重复请求 tryLock fastfail, key=" + cacheKey);
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
                if (logger.isDebugEnabled()) {
                    logger.debug("rlock, load data from target method, cacheName={}, key={}, isLock={}", this.getCacheName(), cacheKey, redis.isLock());
                }
                value = valueLoader.call();
                if (logger.isDebugEnabled()) {
                    logger.debug("rlock, cacheName={}, key={}, value={}, isLock={}", this.getCacheName(), cacheKey, value, redis.isLock());
                }

                // redis和db都为null，那么不发送消息，减少不必要的通信
                if (value == null && valueLoader instanceof ValueLoaderWarpperTemp) {
                    ((ValueLoaderWarpperTemp) valueLoader).setPublishMsg(false);
                    logger.warn("redis and db load value both is null, not need to publish message, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
                }
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
        String cacheKey = (String) buildKey(key);
        RBucket<Object> bucket = getBucket(cacheKey);
        if (!isAllowNullValues() && value == null) {
            boolean flag = bucket.delete();
            logger.warn("delete cache, cacheName={}, key={}, value={}, delete={}", this.getCacheName(), cacheKey, value, flag);
            return;
        }

        value = toStoreValue(value);
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);
        if (expireTime > 0) {
            Object oldValue = bucket.getAndSet(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("put cache, cacheName={}, expireTime={} ms, key={}, value={}, oldValue={}", this.getCacheName(), expireTime, cacheKey, value, oldValue);
        } else {
            Object oldValue = bucket.getAndSet(value);
            logger.info("put cache, cacheName={}, key={}, value={}, oldValue={}", this.getCacheName(), cacheKey, value, oldValue);
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (!isAllowNullValues() && value == null) {
            // 不允许为null，且cacheValue为null，则直接获取旧的缓存项并返回
            return this.get(key);
        }
        String cacheKey = (String) buildKey(key);
        RBucket<Object> bucket = getBucket(cacheKey);
        Object oldValue = bucket.get();
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);
        boolean rslt = false;
        if (expireTime > 0) {
            rslt = bucket.trySet(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("putIfAbsent cache, cacheName={}, expireTime={} ms, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), expireTime, rslt, cacheKey, value, oldValue);
        } else {
            rslt = bucket.trySet(value);
            logger.info("putIfAbsent cache, cacheName={}, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), rslt, cacheKey, value, oldValue);
        }
        return fromStoreValue(oldValue);
    }

    @Override
    public void evict(Object key) {
        String cacheKey = (String) buildKey(key);
        boolean result = getBucket(cacheKey).delete();
        logger.info("evict cache, cacheName={}, key={}, result={}", this.getCacheName(), cacheKey, result);
    }

    @Override
    public void clear() {
        logger.warn("not support clear all cache, cacheName={}", this.getCacheName());
    }

    @Override
    public boolean isExists(Object key) {
        String cacheKey = (String) buildKey(key);
        boolean rslt = getBucket(cacheKey).isExists();
        if (logger.isDebugEnabled()) {
            logger.debug("key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), cacheKey, rslt);
        }
        return rslt;
    }


    /**
     * 批量get
     * 实现抽像方法
     *
     * @param keyMap             将List<K>转换后的 cacheKey Map
     * @param returnNullValueKey true 把key的value为NullValue时转换为null后返回
     * @see Cache#batchGetOrLoad(java.util.List, java.util.function.Function, java.util.function.Function)
     */
    @Override
    public <K, V> Map<K, V> batchGet(Map<K, Object> keyMap, boolean returnNullValueKey) {
        // 命中列表
        Map<K, V> hitMap = new HashMap<>();

        // 查询参数为空
        if (CollectionUtil.isEmpty(keyMap)) {
            logger.info("batchGet cache keyMap is null, cacheName={}, keyMap={}", this.getCacheName(), keyMap);
            return hitMap;
        }
        // 集合切分
        List<List<K>> keyListCollect = Lists.partition(new ArrayList<>(keyMap.keySet()), redis.getBatchPageSize());

        // for循环分执行batch,减少瞬间redisson的netty堆外内存溢出
        keyListCollect.forEach(keyList -> {
            RBatch batch = redissonClient.createBatch();
            keyList.forEach(key -> {
                String cacheKey = (String) buildKey(keyMap.get(key));
                RFuture<Object> async = batch.getBucket(cacheKey).getAsync();
                // 注：onComplete() 是在 batch.execute() 执行之后执行的，所以其中的操作不会增加batch操作的耗时。
                async.onComplete(new BiConsumerWrapper((value, exception) -> {
                    if (exception != null) {
                        logger.warn("batchGet cache error, cacheKey={}, value={}, exception={}", cacheKey, value, exception.getMessage());
                        return;
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("batchGet cache, cacheKey={}, value={}", cacheKey, value);
                    }

                    // value=null表示key不存在，则不将key包含在返回数据中
                    if (value == null) {
                        return;
                    }
                    V warpValue = (V) fromStoreValue(value);
                    if (warpValue != null) {
                        hitMap.put(key, warpValue);
                        return;
                    }
                    // value=NullValue，且returnNullValueKey=true，则将key包含在返回数据中
                    // 目的：batchGetOrLoad中调用batchGet时，可以过滤掉值为NullValue的key，防止缓存穿透到下一层
                    if (returnNullValueKey) {
                        hitMap.put(key, null);
                        LogUtil.log(logger, cacheConfig.getLogLevel(), "[RedissonRBucketCache] batchGet cache, cacheKey={}, value={}, returnNullValueKey={}", cacheKey, value, returnNullValueKey);
                        return;
                    }
                }));
            });
            BatchResult result = batch.execute();
            LogUtil.logDetailPrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] batchGet cache, cacheName={}, currKeyListSize={}, hitMapSize={}", this.getCacheName(), keyList.size(), hitMap.size());
        });
        LogUtil.logDetailPrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] batchGet cache end, cacheName={}, cacheKeyMapSize={}, hitMapSize={}, hitMap={}", this.getCacheName(), keyMap.size(), hitMap.size(), hitMap);
        LogUtil.logSimplePrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] batchGet cache end, cacheName={}, cacheKeyMapSize={}, hitMapSize={}", this.getCacheName(), keyMap.size(), hitMap.size());
        return hitMap;
    }


    @Override
    public <V> void batchPut(Map<Object, V> dataMap) {
        if (null == dataMap || dataMap.size() == 0) {
            return;
        }
        logger.info("batchPut cache start, cacheName={}, totalKeyMapSize={}", this.getCacheName(), dataMap.size());
        // 集合切分
        List<List<Object>> keyListCollect = Lists.partition(new ArrayList<>(dataMap.keySet()), redis.getBatchPageSize());

        // for循环分执行batch,减少瞬间redisson的netty堆外内存溢出
        keyListCollect.forEach(keyList -> {
            RBatch batch = redissonClient.createBatch();
            // 注：keyList.forEach() 是在 batch.execute() 执行之前执行的，所以其中的操作会增加batch操作的耗时。
            // 也就是说edisConnection连接被长时间占用，在高并发情况下，会出现nettyThreads不够用的情况，导致出现如下异常：
            // RedisTimeoutException : Command still hasn't been written into connection! Increase nettyThreads and/or retryInterval settings
            // 方案一：增加nettyThreads（官方的方法，但很难精确到具体的值，因为压测的量不同的情况下，可能情况都不同，所以建议第二种方案）
            // 方案二：直接单个缓存写入，不使用RBatch进行批量写入操作（但增加了交互次数）。
            keyList.forEach(key -> {
                String cacheKey = (String) buildKey(key);
                Object value = toStoreValue(dataMap.get(key));
                // 过期时间处理
                long expireTime = this.expireTimeDeal(value);
                if (expireTime > 0) {
                    batch.getBucket(cacheKey).setAsync(value, expireTime, TimeUnit.MILLISECONDS);
                    logger.info("batchPut cache, expireTime={} ms, key={}, value={}", expireTime, cacheKey, value);
                } else {
                    batch.getBucket(cacheKey).setAsync(value);
                    logger.info("batchPut cache, key={}, value={}", cacheKey, value);
                }
            });
            BatchResult result = batch.execute();
            // logger.info("batchPut cache, cacheName={}, currKeyListSize={}, syncedSlaves={}", this.getCacheName(), keyList.size(), result.getSyncedSlaves());
        });
        logger.info("batchPut cache end, cacheName={}, totalKeyMapSize={}", this.getCacheName(), dataMap.size());
    }


    @Override
    public <K> void batchEvict(Map<K, Object> keyMap) {
        if (null == keyMap || keyMap.size() == 0) {
            return;
        }
        logger.info("batchEvict cache start, cacheName={}, totalKeyMapSize={}", this.getCacheName(), keyMap.size());
        // 集合切分
        List<List<Map.Entry<K, Object>>> keyListCollect = Lists.partition(new ArrayList<>(keyMap.entrySet()), redis.getBatchPageSize());

        // for循环分执行batch,减少瞬间redisson的netty堆外内存溢出
        keyListCollect.forEach(keyList -> {
            RBatch batch = redissonClient.createBatch();
            keyList.forEach(entry -> {
                String cacheKey = (String) buildKey(entry.getValue());
                RFuture<Object> async = batch.getBucket(cacheKey).getAndDeleteAsync();
                // 注：onComplete() 是在 batch.execute() 执行之后执行的，所以其中的操作不会增加batch操作的耗时。
                async.onComplete(new BiConsumerWrapper((value, exception) -> {
                    if (exception != null) {
                        logger.warn("batchEvict cache error, cacheKey={}, value={}, exception={}", cacheKey, value, exception.getMessage());
                        return;
                    }
                    logger.info("batchEvict cache, cacheKey={}, value={}", cacheKey, value);
                }));
            });
            BatchResult result = batch.execute();
            // logger.info("batchEvict cache, cacheName={}, currKeyListSize={}, syncedSlaves={}", this.getCacheName(), keyList.size(), result.getSyncedSlaves());
        });
        logger.info("batchEvict cache end, cacheName={}, totalKeyMapSize={}", this.getCacheName(), keyMap.size());
    }

    // ----------下面为私有方法

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
