package com.coy.l2cache.load;

import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.exception.RedisTrylockFailException;
import com.coy.l2cache.sync.CacheMessage;
import com.coy.l2cache.CacheSyncPolicy;
import com.coy.l2cache.util.NullValueUtil;
import com.coy.l2cache.util.SpringCacheExceptionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 加载数据 Function
 * <p>
 * LoadFunction封装了L2的数据加载逻辑，主要是为了支持如下获取数据的场景：
 * Caffeine.get(key, Function)
 * Guava.get(key, Function)
 * ConcurrentHashMap.computeIfAbsent(key, Function)
 *
 * @author chenck
 * @date 2020/5/15 12:37
 */
public class LoadFunction implements Function<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(LoadFunction.class);
    private final String instanceId;
    private final String cacheType;
    private final String cacheName;
    private final Level2Cache level2Cache;
    private final CacheSyncPolicy cacheSyncPolicy;
    private final ValueLoaderWarpper valueLoader;// 加载数据的目标方法
    /**
     * 是否存储空值，设置为true时，可防止缓存穿透
     */
    private boolean allowNullValues;
    /**
     * 存放NullValue的key，用于控制NullValue对象的有效时间
     */
    private Cache<Object, Integer> nullValueCache;

    public LoadFunction(String instanceId, String cacheType, String cacheName,
                        Level2Cache level2Cache, CacheSyncPolicy cacheSyncPolicy, ValueLoaderWarpper valueLoader,
                        Boolean allowNullValues, Cache<Object, Integer> nullValueCache) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.level2Cache = level2Cache;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.valueLoader = valueLoader;
        this.allowNullValues = allowNullValues;
        this.nullValueCache = nullValueCache;
    }

    @Override
    public Object apply(Object key) {
        try {
            // 走到此处，表明从L1中没有获取到缓存，需要先从L2中获取缓存，若L2无缓存，则再执行目标方法加载数据到缓存
            if (null == level2Cache) {
                if (null == valueLoader) {
                    logger.debug("[LoadFunction] level2Cache and valueLoader is null, return null, cacheName={},key={}", cacheName, key);
                    return this.toStoreValue(key, null);
                }
                Object value = valueLoader.call();
                logger.debug("[LoadFunction] load data from target method, level2Cache is null, cacheName={}, key={}, value={}", cacheName, key, value);
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage(this.instanceId, this.cacheType, this.cacheName, key, CacheConsts.CACHE_REFRESH));
                }
                return this.toStoreValue(key, value);
            }

            if (null == cacheSyncPolicy) {
                return this.toStoreValue(key, level2Cache.get(key, valueLoader));
            }

            // 对 valueLoader 进行包装，以便目标方法执行完后，先put到redis，再发送缓存同步消息，此方式不会对level2Cache造成污染
            ValueLoaderWarpperTemp warpper = null;
            if (null != valueLoader) {
                warpper = new ValueLoaderWarpperTemp(cacheName, key, valueLoader);
            }
            // 先从redis获取缓存，若不存在，则执行ValueLoaderWarpper从db加载数据
            Object value = level2Cache.get(key, warpper);
            if (null != warpper && warpper.isCall()) {
                // 必须在redis.put()之后再发送消息，否则，消息消费方从redis中获取不到缓存，会继续加载值，若程序刚启动，而没有valueLoader,则redis会被设置为null值
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage(this.instanceId, this.cacheType, this.cacheName, key, CacheConsts.CACHE_REFRESH));
                }
            }
            // 集群环境下，valueLoader和value都为null时，直接返回null，避免缓存NullValue，导致出现实际上数据存在，而获取到null值的情况。
            // value等于null，表示从redis中获取的值为null（也就是key不存在），所以直接返回null，避免缓存NullValue，导致缓存和db不一致的情况。
            if (null == valueLoader && null == value) {
                logger.info("[LoadFunction] valueLoader is null, value is null, return null, cacheName={}, key={}", cacheName, key);
                return null;
            }
            return this.toStoreValue(key, value);
        } catch (RedisTrylockFailException e) {
            // 针对 redis 加载数据时的重复请求，直接返回null，避免缓存NullValue
            logger.warn("[LoadFunction] RedisTrylockFailException cacheName={}, key={}, msg={}", cacheName, key, e.getMessage());
            return null;
        } catch (Exception ex) {
            // 将异常包装spring cache异常
            throw SpringCacheExceptionUtil.warpper(key, this.valueLoader, ex);
        } finally {
            if (valueLoader.getWaitRefreshNum() > 0) {
                int beforeWaitRefreshNum = valueLoader.clearWaitRefreshNum();
                logger.info("[LoadFunction] clear waitRefreshNum, cacheName={}, key={}, beforeWaitRefreshNum={}", cacheName, key, beforeWaitRefreshNum);
            }
        }
    }

    /**
     * 转换为存储的值
     */
    private Object toStoreValue(Object key, Object value) {
        // allowNullValues=true，且value=null，则往缓存中put一个NullValue空对象，防止请求穿透到二级缓存或者DB上
        // 注意：CaffeineCache 的定时任务检查到缓存项的值为NullValue时，会清理掉该缓存项，避免一直缓存，一定程度上解决缓存穿透的问题。
        if (this.allowNullValues && (value == null || value instanceof NullValue)) {
            if (null != this.nullValueCache) {
                logger.info("[LoadFunction] NullValueCache put, cacheName={}, key={}, value=1", cacheName, key);
                this.nullValueCache.put(key, 1);
            }
        }
        return NullValueUtil.toStoreValue(value, this.allowNullValues, this.cacheName);
    }

}