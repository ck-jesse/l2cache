package com.coy.l2cache.cache;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.exception.RedisTrylockFailException;
import com.coy.l2cache.util.RandomUtil;
import com.coy.l2cache.util.SpringCacheExceptionUtil;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return redis.getExpireTime();
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
        return this.buildKeyBase(key.toString() + SPLIT + duplicateIndex);
    }

    /**
     * 构建基础缓存key
     * 注：用于操作基本的缓存key
     */
    private Object buildKeyBase(Object key) {
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
        logger.debug("[RedissonRBucketCache] get cache, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
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
            logger.debug("[RedissonRBucketCache] get(key, callable) from redis, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
            return (T) fromStoreValue(value);
        }
        if (null == valueLoader) {
            logger.info("[RedissonRBucketCache] get(key, callable) callable is null, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
            return null;
        }
        RLock lock = null;
        if (redis.isLock() && null != map) {
            // 增加分布式锁，集群环境下同一时刻只会有一个加载数据的线程，解决ABA的问题，保证一级缓存二级缓存数据的一致性
            lock = map.getLock(key);
            if (redis.isTryLock()) {
                if (!lock.tryLock()) {
                    // 高并发场景下，拦截一部分请求将其快速失败，保证性能
                    logger.warn("[RedissonRBucketCache] 重复请求, get(key, callable) tryLock fastfail, return null, cacheName={}, key={}", this.getCacheName(), cacheKey);
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
                logger.debug("[RedissonRBucketCache] rlock, load data from target method, cacheName={}, key={}, isLock={}", this.getCacheName(), cacheKey, redis.isLock());
                value = valueLoader.call();
                logger.debug("[RedissonRBucketCache] rlock, cacheName={}, key={}, value={}, isLock={}", this.getCacheName(), cacheKey, value, redis.isLock());
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
            bucket.delete();
            return;
        }

        value = toStoreValue(value);
        // 过期时间处理
        long expireTime = this.expireTimeDeal(value);
        if (expireTime > 0) {
            bucket.set(value, expireTime, TimeUnit.MILLISECONDS);
            logger.info("[RedissonRBucketCache] put cache, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, cacheKey, value);
        } else {
            bucket.set(value);
            logger.info("[RedissonRBucketCache] put cache, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
        }

        // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
        if (this.checkDuplicateKey(cacheKey)) {
            this.duplicatePut(key, value);
        } else if (openedDuplicate.get()) {
            logger.warn("[RedissonRBucketCache] 只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 put cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
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
            logger.info("[RedissonRBucketCache] putIfAbsent cache, cacheName={}, expireTime={} ms, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), expireTime, rslt, cacheKey, value, oldValue);
        } else {
            rslt = bucket.trySet(value);
            logger.info("[RedissonRBucketCache] putIfAbsent cache, cacheName={}, rslt={}, key={}, value={}, oldValue={}", this.getCacheName(), rslt, cacheKey, value, oldValue);
        }
        // key复制品处理
        if (rslt) {
            // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
            if (this.checkDuplicateKey(cacheKey)) {
                this.duplicateTrySet(key, value);
            } else if (openedDuplicate.get()) {
                logger.warn("[RedissonRBucketCache] 只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 trySet cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
                this.duplicateEvict(key);
            }
        }
        return fromStoreValue(oldValue);
    }

    @Override
    public void evict(Object key) {
        String cacheKey = (String) buildKeyBase(key);
        boolean result = getBucket(cacheKey).delete();
        logger.info("[RedissonRBucketCache] evict cache, cacheName={}, key={}, openedDuplicate={}, result={}", this.getCacheName(), cacheKey, openedDuplicate.get(), result);

        // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
        if (this.checkDuplicateKey(cacheKey)) {
            this.duplicateEvict(key);
        } else if (openedDuplicate.get()) {
            logger.warn("[RedissonRBucketCache] 只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 evict cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
            this.duplicateEvict(key);
        }
    }

    @Override
    public void clear() {
        logger.warn("[RedissonRBucketCache] not support clear all cache, cacheName={}", this.getCacheName());
    }

    @Override
    public boolean isExists(Object key) {
        String cacheKey = (String) buildKeyBase(key);
        boolean rslt = getBucket(cacheKey).isExists();
        logger.debug("[RedissonRBucketCache] key is exists, cacheName={}, key={}, rslt={}", this.getCacheName(), cacheKey, rslt);
        return rslt;
    }

    @Override
    public List<Object> batchGet(List<Object> keyList) {
        if (null == keyList || keyList.size() == 0) {
            return new ArrayList<>();
        }
        RBatch batch = redissonClient.createBatch();
        // 生成完整的key
        List<String> keyListStr = new ArrayList<>();
        keyList.forEach(key -> {
            keyListStr.add((String) buildKey(key));
        });
        // 添加获取缓存值的命令
        keyListStr.forEach(key -> {
            batch.getBucket(key).getAsync();
        });
        BatchResult result = batch.execute();
        List<Object> response = result.getResponses();
        logger.debug("[RedissonRBucketCache] batchGet cache, cacheName={}, keyList={}, valueList={}", this.getCacheName(), keyListStr, response);
        if (null == response) {
            return new ArrayList<>();
        }
        List<Object> list = new ArrayList<>();
        response.forEach(value -> {
            if (null != fromStoreValue(value)) {
                list.add(value);
            }
        });
        return list;
    }

    @Override
    public <T> List<T> batchGet(List<Object> keyList, Class<T> type) {
        List<Object> list = batchGet(keyList);
        if (null == list || list.size() == 0) {
            return (List<T>) list;
        }
        return (List<T>) list;
    }

    @Override
    public <T> void batchPut(Map<Object, T> dataMap) {
        if (null == dataMap || dataMap.size() == 0) {
            return;
        }
        RBatch batch = redissonClient.createBatch();
        dataMap.entrySet().forEach(entry -> {
            String cacheKey = (String) buildKeyBase(entry.getKey());
            Object value = toStoreValue(entry.getValue());
            // 过期时间处理
            long expireTime = this.expireTimeDeal(value);
            if (expireTime > 0) {
                batch.getBucket(cacheKey).setAsync(value, expireTime, TimeUnit.MILLISECONDS);
                logger.info("[RedissonRBucketCache] batchPut cache, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, cacheKey, value);
            } else {
                batch.getBucket(cacheKey).setAsync(value);
                logger.info("[RedissonRBucketCache] batchPut cache, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
            }
            // 必须先检查 checkDuplicateKey()，为false时，再检查openedDuplicate
            if (this.checkDuplicateKey(cacheKey)) {
                this.duplicatePutBuild(entry.getKey(), value, batch, getDuplicateSize(cacheKey));
                // 用于记录开启过副本
                if (openedDuplicate.compareAndSet(false, true)) {
                    logger.info("[RedissonRBucketCache] batchPut openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
                }
            } else if (openedDuplicate.get()) {
                logger.warn("[RedissonRBucketCache] 只要启用过副本则不管副本存不存在都淘汰一次副本，保证数据一致 batchPut cache, cacheName={}, key={}, openedDuplicate={}", this.getCacheName(), cacheKey, openedDuplicate.get());
                this.duplicateEvictBuild(entry.getKey(), batch, getDuplicateSize(cacheKey));
            }
        });
        BatchResult result = batch.execute();
        logger.debug("[RedissonRBucketCache] batchPut cache, cacheName={}, size={}, syncedSlaves={}", this.getCacheName(), dataMap.size(), result.getSyncedSlaves());
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
            logger.info("[RedissonRBucketCache] duplicatePut openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        BatchResult result = batch.execute();
        logger.info("[RedissonRBucketCache] duplicatePut put succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
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
                logger.info("[RedissonRBucketCache] duplicatePut put, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, tempKey, value);
            } else {
                batch.getBucket(tempKey).setAsync(value);
                logger.info("[RedissonRBucketCache] duplicatePut put, cacheName={}, key={}, value={}", this.getCacheName(), tempKey, value);
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
            logger.info("[RedissonRBucketCache] duplicateTrySet openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        BatchResult result = batch.execute();
        logger.info("[RedissonRBucketCache] duplicateTrySet trySet succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
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
                logger.info("[RedissonRBucketCache] duplicateTrySet trySet, cacheName={}, expireTime={} ms, key={}, value={}", this.getCacheName(), expireTime, tempKey, value);
            } else {
                batch.getBucket(tempKey).trySetAsync(value);
                logger.info("[RedissonRBucketCache] duplicateTrySet trySet, cacheName={}, key={}, value={}", this.getCacheName(), tempKey, value);
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
            logger.info("[RedissonRBucketCache] duplicateEvict openedDuplicate set true, cacheName={}, key={}", this.getCacheName(), cacheKey);
        }
        BatchResult result = batch.execute();
        logger.info("[RedissonRBucketCache] duplicateEvict evict succ, cacheName={}, key={}, size={}, syncedSlaves={}", this.getCacheName(), cacheKey, result.getResponses().size(), result.getSyncedSlaves());
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
            logger.info("[RedissonRBucketCache] duplicateEvict evict, cacheName={}, key={}", this.getCacheName(), tempKey);
        }
    }

    /**
     * 检查是否是副本key
     *
     * @param cacheKey 已经构建好的key
     */
    private boolean checkDuplicateKey(String cacheKey) {
        if (!redis.isDuplicate()) {
            logger.debug("[RedissonRBucketCache] checkDuplicateKey, isDuplicate is false, cacheName={}, isDuplicate={}, key={}", this.getCacheName(), redis.isDuplicate(), cacheKey);
            return false;
        }
        if (redis.isDuplicateALlKey()) {
            logger.debug("[RedissonRBucketCache] checkDuplicateKey key, isDuplicateALlKey is true, cacheName={}, isDuplicateALlKey={}, key={}", this.getCacheName(), redis.isDuplicateALlKey(), cacheKey);
            return true;
        }
        if (redis.getDuplicateKeyMap().containsKey(cacheKey)) {
            Integer duplicateSize = redis.getDuplicateKeyMap().get(cacheKey);
            if (null == duplicateSize || duplicateSize <= 0) {
                logger.warn("[RedissonRBucketCache] checkDuplicateKey key, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
                return false;
            }
            logger.debug("[RedissonRBucketCache] checkDuplicateKey key, matched key, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
            return true;
        }
        if (redis.getDuplicateCacheNameMap().containsKey(this.getCacheName())) {
            Integer duplicateSize = redis.getDuplicateCacheNameMap().get(this.getCacheName());
            if (null == duplicateSize || duplicateSize <= 0) {
                logger.warn("[RedissonRBucketCache] checkDuplicateKey cacheName, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
                return false;
            }
            logger.debug("[RedissonRBucketCache] checkDuplicateKey cacheName, matched cacheName, cacheName={}, duplicateSize={}, key={}", this.getCacheName(), duplicateSize, cacheKey);
            return true;
        }
        logger.debug("[RedissonRBucketCache] checkDuplicateKey, not matched, cacheName={}, key={}", this.getCacheName(), cacheKey);
        return false;
    }

    /**
     * 检查是否需要保存副本
     */
    private boolean checkSaveDuplicate(String cacheKey, Object value, int duplicateSize) {
        if (value instanceof NullValue) {
            logger.info("[RedissonRBucketCache] duplicatePut not put, value is NullValue, cacheName={}, duplicateSize={}, key={}, value={}", this.getCacheName(), duplicateSize, cacheKey, value);
            return false;
        }
        if (duplicateSize <= 0) {
            logger.warn("[RedissonRBucketCache] duplicatePut not put, duplicateSize less than 0, cacheName={}, duplicateSize={}, key={}, value={}", this.getCacheName(), duplicateSize, cacheKey, value);
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
