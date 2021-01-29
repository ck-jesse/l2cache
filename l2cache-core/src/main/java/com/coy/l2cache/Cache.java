package com.coy.l2cache;


import com.coy.l2cache.exception.L2CacheException;
import com.coy.l2cache.load.LoadFunction;
import com.coy.l2cache.util.NullValueUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * 定义公共缓存操作的接口
 *
 * <b>Note:</b> 由于缓存的一般用途，建议实现允许存储null值（例如缓存返回{@code null}的方法）
 *
 * @author chenck
 * @date 2020/6/16 19:49
 */
public interface Cache {

    Logger logger = LoggerFactory.getLogger(Cache.class);

    /**
     * 缓存中是否允许null值
     */
    boolean isAllowNullValues();

    /**
     * 获取null值的过期时间
     */
    long getNullValueExpireTimeSeconds();

    /**
     * 获取缓存实例id
     */
    String getInstanceId();

    /**
     * 获取缓存类型
     */
    String getCacheType();

    /**
     * 获取缓存名称
     */
    String getCacheName();

    /**
     * 获取实际缓存对象
     */
    Object getActualCache();

    /**
     * 获取指定key的缓存项
     * 注：本地缓存在loadingCache模式下，若缓存项不存在，则会加载缓存项并put到本地缓存
     */
    Object get(Object key);

    /**
     * 获取指定key的缓存项（如果存在，则获取并返回）
     * 注：仅仅只是获取，缓存项不存在，则不会加载
     */
    default Object getIfPresent(Object key) {
        return get(key);
    }

    /**
     * 获取指定key的缓存项，并返回指定类型的返回值
     */
    default <T> T get(Object key, Class<T> type) {
        Object value = get(key);
        if (null == value) {
            return null;
        }
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    /**
     * 获取指定key的缓存项，如果缓存项不存在则通过{@code valueLoader}获取值
     * <p>
     * 含义：如果已缓存，则返回；否则，则创建、缓存并返回
     *
     * @see LoadFunction#apply(java.lang.Object)
     */
    <T> T get(Object key, Callable<T> valueLoader);

    /**
     * 设置指定key的缓存项
     */
    void put(Object key, Object value);

    /**
     * 如果指定的key不存在，则设置缓存项，如果存在，则返回存在的值
     *
     * @see #put(Object, Object)
     */
    default Object putIfAbsent(Object key, Object value) {
        Object existingValue = get(key);
        if (existingValue == null) {
            put(key, value);
        }
        return existingValue;
    }

    /**
     * 从存储值解析为具体值
     */
    default Object fromStoreValue(Object storeValue) {
        return NullValueUtil.fromStoreValue(storeValue, this.isAllowNullValues());
    }

    /**
     * 转换为存储值
     */
    default Object toStoreValue(Object userValue) {
        return NullValueUtil.toStoreValue(userValue, this.isAllowNullValues(), this.getCacheName());
    }

    /**
     * 删除指定的缓存项（如果存在）
     */
    void evict(Object key);

    /**
     * 删除所有缓存项
     */
    void clear();

    /**
     * 检查key是否存在
     *
     * @return true 表示存在，false 表示不存在
     */
    boolean isExists(Object key);

    // ----- 批量操作

    /**
     * 批量get
     * 注：支持批量get时的灵活定义cacheKey的构建
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     */
    default <K, R> Map<K, R> batchGet(List<K> keyList) {
        return this.batchGet(keyList, null);
    }

    /**
     * 批量get
     * 注：支持批量get时的灵活定义cacheKey的构建
     *
     * @param keyList    业务维度的key集合（K可能是自定义DTO）
     * @param keyBuilder 自定义的cacheKey构建器
     */
    default <K, R> Map<K, R> batchGet(List<K> keyList, Function<Object, K> keyBuilder) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO
        Map<K, Object> keyMap = new HashMap<>();// <K, cacheKey>
        if (null != keyBuilder) {
            keyList.forEach(key -> keyMap.put(key, keyBuilder.apply(key)));
        } else {
            keyList.forEach(key -> keyMap.put(key, key));
        }
        return this.batchGet(keyMap);
    }

    /**
     * 批量get
     *
     * @param keyMap 将List<K>转换后的 cacheKey Map
     */
    default <K, V> Map<K, V> batchGet(Map<K, Object> keyMap) {
        // 命中列表
        Map<K, V> hitMap = new HashMap<>();

        keyMap.forEach((o, cacheKey) -> {
            // 仅仅获取
            V value = (V) this.getIfPresent(cacheKey);
            logger.debug("batchGet, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
            if (null != value) {
                hitMap.put(o, value);
            }
        });
        return hitMap;
    }


    /**
     * 批量get或load
     *
     * @param keyList     业务维度的key集合（K可能是自定义DTO）
     * @param valueLoader 值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<List<K>, Map<K, V>> valueLoader) {
        return this.batchGetOrLoad(keyList, null, valueLoader);
    }

    /**
     * 批量get或load
     *
     * @param keyList     业务维度的key集合（K可能是自定义DTO）
     * @param keyBuilder  自定义的cacheKey构建器
     * @param valueLoader 值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<Object, K> keyBuilder, Function<List<K>, Map<K, V>> valueLoader) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO
        Map<K, Object> keyMap = new HashMap<>();// <K, cacheKey>
        if (null != keyBuilder) {
            keyList.forEach(key -> keyMap.put(key, keyBuilder.apply(key)));
        } else {
            keyList.forEach(key -> keyMap.put(key, key));
        }
        return this.batchGetOrLoad(keyMap, valueLoader);
    }

    /**
     * 批量get或load
     * 注：调用方自己组装好参数
     *
     * @param keyMap      将List<K>转换后的 cacheKey Map
     * @param valueLoader 值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader) {
        try {
            // 获取命中列表
            Map<K, V> hitMap = this.batchGet(keyMap);

            // 过滤未命中key列表
            Map<K, Object> notHitKeyMap = new HashMap<>();
            keyMap.forEach((k, cacheKey) -> {
                if (hitMap.containsKey(k)) {
                    notHitKeyMap.put(k, cacheKey);
                }
            });

            // 全部命中，直接返回
            if (CollectionUtils.isEmpty(notHitKeyMap)) {
                logger.info("batchGetOrLoad all_hit, cacheName={}, notHitKey={}", this.getCacheName(), notHitKeyMap);
                return hitMap;
            }

            if (null == valueLoader) {
                logger.info("batchGetOrLoad valueLoader is null return hitMap, cacheName={}, notHitKey={}", this.getCacheName(), notHitKeyMap);
                return hitMap;
            }
            Map<K, V> notHitDataMap = valueLoader.apply(new ArrayList<>(notHitKeyMap.keySet()));

            // 一个都没有命中，直接返回
            if (CollectionUtils.isEmpty(notHitDataMap)) {
                // 对未命中的key缓存空值，防止缓存穿透
                notHitKeyMap.forEach((k, cacheKey) -> {
                    logger.info("batchGetOrLoad notHitKey is not exist, put null, cacheName={}, cacheKey={}", this.getCacheName(), cacheKey);
                    this.put(cacheKey, null);
                });
                return hitMap;
            }

            // 合并数据
            hitMap.putAll(notHitDataMap);

            // 将未命中缓存的数据按照cacheKey的方式来组装以便put到缓存
            Map<Object, V> batchPutDataMap = new HashMap<>();
            keyMap.entrySet().stream().filter(entry -> notHitDataMap.containsKey(entry.getKey())).forEach(entry -> batchPutDataMap.put(entry.getValue(), notHitDataMap.get(entry.getKey())));
            // 将未命中缓存的数据put到缓存
            this.batchPut(batchPutDataMap);
            logger.info("batchGetOrLoad batch put not hit cache data, cacheName={}, notHitKeyMap={}", this.getCacheName(), notHitKeyMap);

            // 处理没有查询到数据的key，缓存空值
            if (notHitDataMap.size() != notHitKeyMap.size()) {
                notHitKeyMap.forEach((k, cacheKey) -> {
                    if (!notHitDataMap.containsKey(k)) {
                        logger.info("batchGetOrLoad key is not exist, put null, cacheName={}, cacheKey={}", this.getCacheName(), cacheKey);
                        this.put(cacheKey, null);
                    }
                });
            }
            return hitMap;
        } catch (Exception e) {
            logger.error("batchGetOrLoad error, keyList={}", keyMap.values(), e);
            throw new L2CacheException("batchGetOrLoad error," + e.getMessage());
        }
    }

    /**
     * 批量put
     */
    default <V> void batchPut(Map<Object, V> dataMap) {
        if (null == dataMap || dataMap.size() == 0) {
            return;
        }
        dataMap.forEach((key, value) -> {
            put(key, value);
        });
    }

}
