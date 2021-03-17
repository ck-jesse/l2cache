package com.coy.l2cache.cache;

import com.coy.l2cache.Cache;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.exception.L2CacheException;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractAdaptingCache implements Cache {

    /**
     * 缓存实例id
     */
    private String instanceId;
    /**
     * 缓存名字
     */
    private final String cacheName;
    /**
     * 是否允许为空
     */
    private final boolean allowNullValues;
    /**
     * NullValue的过期时间，单位秒
     */
    private long nullValueExpireTimeSeconds;

    public AbstractAdaptingCache(String cacheName, CacheConfig cacheConfig) {
        this.instanceId = cacheConfig.getInstanceId();
        this.cacheName = cacheName;
        this.allowNullValues = cacheConfig.isAllowNullValues();
        this.nullValueExpireTimeSeconds = cacheConfig.getNullValueExpireTimeSeconds();
        if (this.nullValueExpireTimeSeconds < 0) {
            this.nullValueExpireTimeSeconds = 60;
        }
    }

    @Override
    public boolean isAllowNullValues() {
        return this.allowNullValues;
    }

    @Override
    public long getNullValueExpireTimeSeconds() {
        return this.nullValueExpireTimeSeconds;
    }

    @Override
    public String getInstanceId() {
        return this.instanceId;
    }

    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    @Override
    public <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader, boolean returnNullValueKey) {
        // 获取命中缓存的数据列表
        Map<K, V> hitCacheMap = this.batchGet(keyMap, true);// 此处returnNullValueKey固定为true，不要修改防止缓存穿透

        // 获取未命中缓存的key列表
        Map<K, Object> notHitCacheKeyMap = keyMap.entrySet().stream()
                .filter(entry -> !hitCacheMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

        if (null == valueLoader) {
            logger.info("[{}] batchGetOrLoad valueLoader is null return hitCacheMap, cacheName={}, cacheKeyMap={}, returnNullValueKey={}", this.getClass().getSimpleName(), this.getCacheName(), keyMap.values(), returnNullValueKey);
            return this.filterNullValue(hitCacheMap, returnNullValueKey);
        }

        // 全部命中缓存，直接返回
        if (CollectionUtils.isEmpty(notHitCacheKeyMap) && !CollectionUtils.isEmpty(hitCacheMap)) {
            logger.info("[{}] batchGetOrLoad all_hit cache, cacheName={}, cacheKeyMap={}, returnNullValueKey={}", this.getClass().getSimpleName(), this.getCacheName(), keyMap.values(), returnNullValueKey);
            return this.filterNullValue(hitCacheMap, returnNullValueKey);
        }

        Map<K, V> valueLoaderHitMap = this.loadAndPut(valueLoader, notHitCacheKeyMap);
        if (!CollectionUtils.isEmpty(valueLoaderHitMap)) {
            hitCacheMap.putAll(valueLoaderHitMap);// 合并数据
        }
        return this.filterNullValue(hitCacheMap, returnNullValueKey);
    }

    /**
     * 过滤掉null值
     *
     * @param returnNullValueKey true 表示把value=NullValue的key包含在Map中返回(实际返回null)
     */
    protected <K, V> Map<K, V> filterNullValue(Map<K, V> hitCacheMap, boolean returnNullValueKey) {
        if (returnNullValueKey) {
            return hitCacheMap;
        }
        return hitCacheMap.entrySet().stream()
                .filter(entry -> {
                    if (null == entry.getValue()) {
                        logger.info("[{}] filter null from hitCacheMap, cacheName={}, key={}, value={}", this.getClass().getSimpleName(), this.getCacheName(), entry.getKey(), entry.getValue());
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    /**
     * 加载数据并put到缓存
     */
    protected <K, V> Map<K, V> loadAndPut(Function<List<K>, Map<K, V>> valueLoader, Map<K, Object> notHitCacheKeyMap) {
        try {
            Map<K, V> valueLoaderHitMap = valueLoader.apply(new ArrayList<>(notHitCacheKeyMap.keySet()));

            // 从DB获取数据，一个都没有命中，直接返回
            if (CollectionUtils.isEmpty(valueLoaderHitMap)) {
                // 对未命中的key缓存空值，防止缓存穿透
                Map<Object, V> nullValueMap = new HashMap<>();
                notHitCacheKeyMap.forEach((k, cacheKey) -> {
                    nullValueMap.put(cacheKey, null);
                });
                this.batchPut(nullValueMap);
                logger.info("[{}] batchGetOrLoad not loaded to data from valueLoader, put null, cacheName={}, cacheKey={}", this.getClass().getSimpleName(), this.getCacheName(), nullValueMap.keySet());
                return valueLoaderHitMap;
            }

            // 将命中DB的数据按照cacheKey的方式来组装以便put到缓存
            // 如果K是自定义DTO，且没有重写hashCode()和equals(Object)，那么通过K从hitMap中可能获取不到数据，所以要特别注意
            Map<Object, V> batchPutDataMap = notHitCacheKeyMap.entrySet().stream()
                    .filter(entry -> valueLoaderHitMap.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> entry.getValue(), entry -> valueLoaderHitMap.get(entry.getKey())));
            this.batchPut(batchPutDataMap);
            logger.info("[{}] batchGetOrLoad batch put loaded data from valueLoader, cacheName={}, notHitCacheKeyList={}", this.getClass().getSimpleName(), this.getCacheName(), batchPutDataMap.keySet());

            // 处理没有查询到数据的key，缓存空值，防止缓存穿透
            if (valueLoaderHitMap.size() != notHitCacheKeyMap.size()) {
                Map<Object, V> nullValueMap = new HashMap<>();
                notHitCacheKeyMap.forEach((k, cacheKey) -> {
                    if (!valueLoaderHitMap.containsKey(k)) {
                        nullValueMap.put(cacheKey, null);
                    }
                });
                this.batchPut(nullValueMap);
                logger.info("[{}] batchGetOrLoad loaded to part data from valueLoader, put null, cacheName={}, cacheKey={}", this.getClass().getSimpleName(), this.getCacheName(), nullValueMap.keySet());
            }
            return valueLoaderHitMap;
        } catch (Exception e) {
            logger.error("[{}] batchGetOrLoad error, cacheKeyList={}", this.getClass().getSimpleName(), notHitCacheKeyMap.values(), e);
            throw new L2CacheException("batchGetOrLoad error," + e.getMessage());
        }
    }
}
