package com.coy.l2cache.load;

import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.sync.CacheMessage;
import com.coy.l2cache.CacheSyncPolicy;
import com.coy.l2cache.util.SpringCacheExceptionUtil;
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
    private final Callable<?> valueLoader;// 加载数据的目标方法

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
            // 走到此处，表明从L1中没有获取到缓存，需要先从L2中获取缓存，若L2无缓存，则再执行目标方法加载数据到缓存
            if (null == level2Cache) {
                if (null == valueLoader) {
                    logger.debug("[LoadFunction] level2Cache and valueLoader is null, return null, key={}", key);
                    return null;
                }
                Object value = valueLoader.call();
                logger.debug("[LoadFunction] load data from target method, level2Cache is null, cacheName={}, key={}, value={}", cacheName,
                        key, value);
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage(this.instanceId, this.cacheType, this.cacheName, key, CacheConsts.CACHE_REFRESH));
                }
                return value;
            }
            if (null == cacheSyncPolicy) {
                return level2Cache.get(key, valueLoader);
            }

            // 对 valueLoader 进行包装，以便目标方法执行完后发送缓存同步消息，此方式不会对level2Cache造成污染
            return level2Cache.get(key, () -> {
                if (null == valueLoader) {
                    logger.debug("[LoadFunction] valueLoader is null, return null, key={}", key);
                    return null;
                }
                Object tempValue = valueLoader.call();
                logger.debug("[LoadFunction] valueLoader.call, key={}, value={}", key, tempValue);
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage(this.instanceId, this.cacheType, this.cacheName, key, CacheConsts.CACHE_REFRESH));
                }
                return tempValue;
            });
        } catch (Exception ex) {
            // 将异常包装spring cache异常
            throw SpringCacheExceptionUtil.warpper(key, this.valueLoader, ex);
        }
    }
}