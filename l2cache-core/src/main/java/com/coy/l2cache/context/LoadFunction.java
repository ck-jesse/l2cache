package com.coy.l2cache.context;

import com.coy.l2cache.consts.CacheConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 加载数据 Function
 *
 * @author chenck
 * @date 2020/5/15 12:37
 */
public class LoadFunction implements Function<Object, Object> {

    private final Logger logger = LoggerFactory.getLogger(LoadFunction.class);

    private final ExtendCache extendCache;
    private final Callable<?> valueLoader;

    public LoadFunction(ExtendCache extendCache, Callable<?> valueLoader) {
        this.extendCache = extendCache;
        this.valueLoader = valueLoader;
    }

    @Override
    public Object apply(Object key) {
        try {
            logger.debug("[LoadFunction] load cache, cacheName={}, key={}", extendCache.getName(), key);
            // 走到此处，表明已经从本地缓存中没有获取到数据，所以先从redis中获取数据
            Object value = extendCache.getRedisValue(key);

            if (value != null) {
                logger.debug("[LoadFunction] get cache from redis, cacheName={}, key={}, value={}", extendCache.getName(), key, value);
                // 从redis中获取到数据后不需要显示设置到本地缓存，利用Caffeine本身的机制进行设置
                return value;
            }
            // TODO 此处可添加分布式锁来控制全局只有一个节点加载数据

            // 执行业务方法获取数据
            value = extendCache.toStoreValueWrap(this.valueLoader.call());
            logger.debug("[LoadFunction] load data from method, cacheName={}, key={}, value={}", extendCache.getName(), key, value);

            extendCache.setRedisValue(key, value);

            extendCache.cacheChangePush(key, CacheConsts.CACHE_REFRESH);

            return value;
        } catch (Exception ex) {
            throw new Cache.ValueRetrievalException(key, this.valueLoader, ex);
        }
    }
}