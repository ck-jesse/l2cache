package com.coy.l2cache.load;

import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.sync.CacheMessage;
import com.coy.l2cache.CacheSyncPolicy;
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
    private final Callable<?> valueLoader;

    public LoadFunction(String instanceId, String cacheType, String cacheName,
                        Level2Cache level2Cache, CacheSyncPolicy cacheSyncPolicy, Callable<?> valueLoader) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.level2Cache = level2Cache;
        this.cacheSyncPolicy = cacheSyncPolicy;
        this.valueLoader = valueLoader;
    }

    @Override
    public Object apply(Object key) {
        try {
            Object value = null;
            if (null == level2Cache) {
                if (null == valueLoader) {
                    logger.debug("[LoadFunction] level2Cache and valueLoader is null direct return null, key={}", key);
                    return null;
                }
                value = valueLoader.call();
                logger.debug("[LoadFunction] load data from method, level2Cache is null, cacheName={}, key={}, value={}", cacheName,
                        key, value);
            } else {
                logger.debug("[LoadFunction] load cache, cacheName={}, key={}", cacheName, key);
                // 走到此处，表明已经从L1中没有获取到数据，所以先从L2中获取数据
                value = level2Cache.get(key);

                if (value != null) {
                    logger.debug("[LoadFunction] get cache from {}, cacheName={}, key={}, value={}", level2Cache.getCacheType(), cacheName, key,
                            value);
                    // 从L2中获取到数据后不需要显示设置到L1，利用L1本身的机制进行设置
                    return value;
                }

                if (null == valueLoader) {
                    logger.debug("[LoadFunction] valueLoader is null direct return null, key={}", key);
                    return null;
                }

                // 执行业务方法获取数据
                value = valueLoader.call();
                logger.debug("[LoadFunction] load data from target method, cacheName={}, key={}, value={}", cacheName, key, value);

                level2Cache.put(key, value);
            }
            if (null != cacheSyncPolicy) {
                cacheSyncPolicy.publish(new CacheMessage(this.instanceId, this.cacheType, this.cacheName, key, CacheConsts.CACHE_REFRESH));
            }
            return value;
        } catch (Exception ex) {
            throw new org.springframework.cache.Cache.ValueRetrievalException(key, this.valueLoader, ex);
        }
    }
}