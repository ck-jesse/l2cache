package com.github.jesse.l2cache.cache;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.NullValue;
import com.github.jesse.l2cache.exception.RedisTrylockFailException;
import com.github.jesse.l2cache.load.ValueLoaderWarpper;
import com.github.jesse.l2cache.load.ValueLoaderWarpperTemp;
import com.github.jesse.l2cache.util.LogUtil;
import com.github.jesse.l2cache.util.RandomUtil;
import com.github.jesse.l2cache.util.SpringCacheExceptionUtil;
import com.google.common.collect.Lists;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

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

    private RMap<Object, Object> map;

    /**
     * 记录是否启用过副本，只要启用过，则记录为true
     * 场景1：如果先开启副本开关，再停用副本开关，然后再开启副本开关，那么可能会存在副本数据不一致的情况，通过该属性来控制，只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致。
     * 场景2：如果先开启副本开关，然后停止服务，服务停止期间，停用副本开关，然后启动服务后再启用副本开关，那么这种场景则会没办法保证一致性，这种场景只能将缓存过期时间减小，再过期后再进行开启。
     */
    private AtomicBoolean openedDuplicate = new AtomicBoolean();

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
        String cacheKey = (String) buildKeyBase(key);
        if (!this.checkDuplicateKey(cacheKey)) {
            return cacheKey;
        }
        // 根据 随机数 构建缓存key，用于获取缓存
        int duplicateSize = this.getDuplicateSize(key.toString());
        if (duplicateSize <= 0) {
            return cacheKey;
        }
        int duplicateIndex = RandomUtil.getRandomInt(0, duplicateSize);
        return this.buildKeyByDuplicate(key.toString(), duplicateIndex);
    }

    /**
     * 根据 复制品下标 构建缓存key
     * 注：用于操作复制品缓存数据
     *
     * @param duplicateIndex 复制品下标
     */
    private Object buildKeyByDuplicate(Object key, int duplicateIndex) {
        return this.buildKeyBase(key.toString() + CacheConsts.SPLIT + duplicateIndex);
    }

    /**
     * 构建基础缓存key
     * 注：用于操作基本的缓存key
     */
    private Object buildKeyBase(Object key) {
        if (key == null || "".equals(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        StringBuilder sb = new StringBuilder(this.getCacheName()).append(CacheConsts.SPLIT);
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
        String cacheKey = (String) buildKeyBase(key);
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
            //bucket.set(value, expireTime, TimeUnit.MILLISECONDS);
            Object oldValue = bucket.getAndSet(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("put cache, cacheName={}, expireTime={} ms, key={}, value={}, oldValue={}", this.getCacheName(), expireTime, cacheKey, value, oldValue);
        } else {
            //bucket.set(value);
            Object oldValue = bucket.getAndSet(value);
            logger.info("put cache, cacheName={}, key={}, value={}, oldValue={}", this.getCacheName(), cacheKey, value, oldValue);
        }

        // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
        if (this.checkDuplicateKey(cacheKey)) {
            this.duplicatePut(key, value);
        } else if (openedDuplicate.get()) {
            logger.warn("只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 put cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
            this.duplicateEvict(key);
        }
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        if (!isAllowNullValues() && value == null) {
            // 不允许为null，且cacheValue为null，则直接获取旧的缓存项并返回
            return this.get(key);
        }
        String cacheKey = (String) buildKeyBase(key);
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
        // key复制品处理
        if (rslt) {
            // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
            if (this.checkDuplicateKey(cacheKey)) {
                this.duplicateTrySet(key, value);
            } else if (openedDuplicate.get()) {
                logger.warn("只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 trySet cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
                this.duplicateEvict(key);
            }
        }
        return fromStoreValue(oldValue);
    }

    @Override
    public void evict(Object key) {
        String cacheKey = (String) buildKeyBase(key);
        boolean result = getBucket(cacheKey).delete();
        logger.info("evict cache, cacheName={}, key={}, openedDuplicate={}, result={}", this.getCacheName(), cacheKey, openedDuplicate.get(), result);

        // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
        if (this.checkDuplicateKey(cacheKey)) {
            this.duplicateEvict(key);
        } else if (openedDuplicate.get()) {
            logger.warn("只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 evict cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
            this.duplicateEvict(key);
        }
    }

    @Override
    public void clear() {
        logger.warn("not support clear all cache, cacheName={}", this.getCacheName());
    }

    @Override
    public boolean isExists(Object key) {
        String cacheKey = (String) buildKeyBase(key);
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
        if (CollectionUtils.isEmpty(keyMap)) {
            logger.info("batchGet cache keyMap is null, cacheName={}, keyMap={}", this.getCacheName(), keyMap);
            return hitMap;
        }
        // 集合切分
        List<List<K>> keyListCollect = Lists.partition(new ArrayList<>(keyMap.keySet()), redis.getBatchPageSize());

        // for循环分执行batch,减少瞬间redisson的netty堆外内存溢出
        keyListCollect.forEach(keyList -> {
            RBatch batch = redissonClient.createBatch();
            keyList.forEach(key -> {
                String cacheKey = (String) buildKeyBase(keyMap.get(key));
                RFuture<Object> async = batch.getBucket(cacheKey).getAsync();
                // 注：onComplete() 是在 batch.execute() 执行之后执行的，所以其中的操作不会增加batch操作的耗时。
                async.onComplete(new BiConsumerWrapper((value, exception) -> {
                    if (exception != null) {
                        logger.warn("batchGet cache error, cacheName={}, cacheKey={}, value={}, exception={}", this.getCacheName(), cacheKey, value, exception.getMessage());
                        return;
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("batchGet cache, cacheName={}, cacheKey={}, value={}", this.getCacheName(), cacheKey, value);
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
                        LogUtil.log(logger, cacheConfig.getLogLevel(), "[RedissonRBucketCache] batchGet cache, cacheName={}, cacheKey={}, value={}, returnNullValueKey={}", this.getCacheName(), cacheKey, value, returnNullValueKey);
                        return;
                    }
                }));
//                async.onComplete((value, exception) -> {
//                    if (exception != null) {
//                        logger.warn("batchGet cache error, cacheName={}, cacheKey={}, value={}, exception={}", this.getCacheName(), cacheKey, value, exception.getMessage());
//                        return;
//                    }
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("batchGet cache, cacheName={}, cacheKey={}, value={}", this.getCacheName(), cacheKey, value);
//                    }
//
//                    // value=null表示key不存在，则不将key包含在返回数据中
//                    if (value == null) {
//                        return;
//                    }
//                    V warpValue = (V) fromStoreValue(value);
//                    if (warpValue != null) {
//                        hitMap.put(key, warpValue);
//                        return;
//                    }
//                    // value=NullValue，且returnNullValueKey=true，则将key包含在返回数据中
//                    // 目的：batchGetOrLoad中调用batchGet时，可以过滤掉值为NullValue的key，防止缓存穿透到下一层
//                    if (returnNullValueKey) {
//                        hitMap.put(key, null);
//                        LogUtil.log(logger, cacheConfig.getLogLevel(), "[RedissonRBucketCache] batchGet cache, cacheName={}, cacheKey={}, value={}, returnNullValueKey={}", this.getCacheName(), cacheKey, value, returnNullValueKey);
//                        return;
//                    }
//                });
            });
            BatchResult result = batch.execute();
            LogUtil.logDetailPrint(logger, redis.getPrintDetailLogSwitch(), "[RedissonRBucketCache] batchGet cache, cacheName={}, totalKeyMapSize={}, currKeyListSize={}, hitMapSize={}", this.getCacheName(), keyMap.size(), keyList.size(), hitMap.size());
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
                String cacheKey = (String) buildKeyBase(key);
                Object value = toStoreValue(dataMap.get(key));
                // 过期时间处理
                long expireTime = this.expireTimeDeal(value);
                if (expireTime > 0) {
                    batch.getBucket(cacheKey).setAsync(value, expireTime, TimeUnit.MILLISECONDS);
                    logger.info("batchPut cache, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, cacheKey, value);
                } else {
                    batch.getBucket(cacheKey).setAsync(value);
                    logger.info("batchPut cache, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
                }
                // TODO 副本的逻辑需要去掉
                // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
                if (this.checkDuplicateKey(cacheKey)) {
                    int duplicateSize = getDuplicateSize(cacheKey);
                    this.duplicatePutBuild(key, value, batch, duplicateSize);
                    // 用于记录开启过副本
                    if (openedDuplicate.compareAndSet(false, true)) {
                        logger.info("batchPut openedDuplicate set true, cacheName={}, key={}, duplicateSize={}", this.getCacheName(), cacheKey, duplicateSize);
                    }
                } else if (openedDuplicate.get()) {
                    logger.warn("只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 batchPut cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
                    this.duplicateEvictBuild(key, batch, getDuplicateSize(cacheKey));
                }
            });
            BatchResult result = batch.execute();
            logger.info("batchPut cache, cacheName={}, totalKeyMapSize={}, currKeyListSize={}, syncedSlaves={}", this.getCacheName(), dataMap.size(), keyList.size(), result.getSyncedSlaves());
        });
        logger.info("batchPut cache end, cacheName={}, totalKeyMapSize={}", this.getCacheName(), dataMap.size());
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

    /**
     * 副本Put
     * 主要解决单个redis分片上热点key的问题，相当于原来存一份数据，现在存多份相同的数据，将热key的压力分散到多个分片。
     * 以redis内存空间来降低单分片压力。
     */
    private void duplicatePut(Object key, Object value) {
        String cacheKey = (String) buildKeyBase(key);
        int duplicateSize = getDuplicateSize(cacheKey);
        boolean check = this.checkSaveDuplicate(cacheKey, value, duplicateSize);
        if (!check) {
            return;
        }

        RBatch batch = redissonClient.createBatch();

        this.duplicatePutBuild(key, value, batch, duplicateSize);

        // 用于记录开启过副本
        if (openedDuplicate.compareAndSet(false, true)) {
            logger.info("duplicatePut openedDuplicate set true, cacheName={}, key={}, duplicateSize={}", this.getCacheName(), cacheKey, duplicateSize);
        }
        BatchResult result = batch.execute();
        logger.info("duplicatePut put succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
    }

    /**
     * 构建 set 批量命令
     */
    private void duplicatePutBuild(Object key, Object value, RBatch batch, int duplicateSize) {
        String cacheKey = (String) buildKeyBase(key);
        boolean check = this.checkSaveDuplicate(cacheKey, value, duplicateSize);
        if (!check) {
            return;
        }

        value = toStoreValue(value);
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);

        String tempKey = "";
        for (int i = 0; i < duplicateSize; i++) {
            tempKey = (String) buildKeyByDuplicate(key.toString(), i);
            if (expireTime > 0) {
                batch.getBucket(tempKey).setAsync(value, expireTime, TimeUnit.MILLISECONDS);
                logger.info("duplicatePut put, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, tempKey, value);
            } else {
                batch.getBucket(tempKey).setAsync(value);
                logger.info("duplicatePut put, cacheName={}, key={}, value={}", this.getCacheName(), tempKey, value);
            }
        }
    }

    /**
     * 副本TrySet
     */
    private void duplicateTrySet(Object key, Object value) {
        String cacheKey = (String) buildKeyBase(key);
        int duplicateSize = getDuplicateSize(cacheKey);
        boolean check = this.checkSaveDuplicate(cacheKey, value, duplicateSize);
        if (!check) {
            return;
        }

        RBatch batch = redissonClient.createBatch();

        this.duplicateTrySetBuild(key, value, batch, duplicateSize);

        // 用于记录开启过副本
        if (openedDuplicate.compareAndSet(false, true)) {
            logger.info("duplicateTrySet openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        BatchResult result = batch.execute();
        logger.info("duplicateTrySet trySet succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
    }

    /**
     * 构建 trySet 批量命令
     */
    private void duplicateTrySetBuild(Object key, Object value, RBatch batch, int duplicateSize) {
        String cacheKey = (String) buildKeyBase(key);
        boolean check = this.checkSaveDuplicate(cacheKey, value, duplicateSize);
        if (!check) {
            return;
        }

        value = toStoreValue(value);
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);

        String tempKey = "";
        for (int i = 0; i < duplicateSize; i++) {
            tempKey = (String) buildKeyByDuplicate(key.toString(), i);
            if (expireTime > 0) {
                batch.getBucket(tempKey).trySetAsync(value, expireTime, TimeUnit.MILLISECONDS);
                logger.info("duplicateTrySet trySet, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, tempKey, value);
            } else {
                batch.getBucket(tempKey).trySetAsync(value);
                logger.info("duplicateTrySet trySet, cacheName={}, key={}, value={}", this.getCacheName(), tempKey, value);
            }
        }
    }

    /**
     * 淘汰副本
     */
    private void duplicateEvict(Object key) {
        String cacheKey = (String) buildKeyBase(key);
        int duplicateSize = getDuplicateSize(cacheKey);
        boolean check = this.checkSaveDuplicate(cacheKey, null, duplicateSize);
        if (!check) {
            return;
        }
        RBatch batch = redissonClient.createBatch();

        this.duplicateEvictBuild(key, batch, duplicateSize);

        // 用于记录开启过副本
        if (openedDuplicate.compareAndSet(false, true)) {
            logger.info("duplicateEvict openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        BatchResult result = batch.execute();
        logger.info("duplicateEvict evict succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
    }

    /**
     * 构建 delete 批量命令
     */
    private void duplicateEvictBuild(Object key, RBatch batch, int duplicateSize) {
        String cacheKey = (String) buildKeyBase(key);
        boolean check = this.checkSaveDuplicate(cacheKey, null, duplicateSize);
        if (!check) {
            return;
        }

        String tempKey = "";
        for (int i = 0; i < duplicateSize; i++) {
            tempKey = (String) buildKeyByDuplicate(key.toString(), i);
            batch.getBucket(tempKey).deleteAsync();
            logger.info("duplicateEvict evict, cacheName={}, key={}", this.getCacheName(), tempKey);
        }
    }

    /**
     * 检查是否是副本key
     *
     * @param cacheKey 已经构建好的key
     */
    private boolean checkDuplicateKey(String cacheKey) {
        if (!redis.isDuplicate()) {
            if (logger.isDebugEnabled()) {
                logger.debug("checkDuplicateKey, isDuplicate is false, cacheName={}, isDuplicate={}, key={}", this.getCacheName(), redis.isDuplicate(), cacheKey);
            }
            return false;
        }
        if (redis.isDuplicateALlKey()) {
            if (logger.isDebugEnabled()) {
                logger.debug("checkDuplicateKey key, isDuplicateALlKey is true, cacheName={}, isDuplicateALlKey={}, key={}", this.getCacheName(), redis.isDuplicateALlKey(), cacheKey);
            }
            return true;
        }
        if (redis.getDuplicateKeyMap().containsKey(cacheKey)) {
            Integer duplicateSize = redis.getDuplicateKeyMap().get(cacheKey);
            if (null == duplicateSize || duplicateSize <= 0) {
                logger.warn("checkDuplicateKey key, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
                return false;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("checkDuplicateKey key, matched key, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
            }
            return true;
        }
        if (redis.getDuplicateCacheNameMap().containsKey(this.getCacheName())) {
            Integer duplicateSize = redis.getDuplicateCacheNameMap().get(this.getCacheName());
            if (null == duplicateSize || duplicateSize <= 0) {
                logger.warn("checkDuplicateKey cacheName, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
                return false;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("checkDuplicateKey cacheName, matched cacheName, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
            }
            return true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("checkDuplicateKey, not matched, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        return false;
    }

    /**
     * 检查是否需要保存副本
     */
    private boolean checkSaveDuplicate(String cacheKey, Object value, int duplicateSize) {
        if (value instanceof NullValue) {
            logger.info("duplicatePut not put, value is NullValue, cacheName={}, duplicateSize={}, key={}, value={}", this.getCacheName(), duplicateSize, cacheKey, value);
            return false;
        }
        if (duplicateSize <= 0) {
            logger.warn("duplicatePut not put, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}, value={}", this.getCacheName(), duplicateSize, cacheKey, value);
            return false;
        }
        return true;
    }

    /**
     * 获取cacheKey的副本数量
     * 副本数量的优先级：单个key维度 > cacheName维度 > 默认副本数量
     */
    private int getDuplicateSize(String cacheKey) {
        if (redis.getDuplicateKeyMap().containsKey(cacheKey)) {
            return redis.getDuplicateKeyMap().get(cacheKey);
        }
        if (redis.getDuplicateCacheNameMap().containsKey(this.getCacheName())) {
            return redis.getDuplicateCacheNameMap().get(this.getCacheName());
        }
        return redis.getDefaultDuplicateSize();
    }


}
