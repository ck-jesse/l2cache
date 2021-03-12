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
import java.util.stream.Collectors;

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
     * @param keyList         业务维度的key集合（K可能是自定义DTO）
     * @param cacheKeyBuilder 自定义的cacheKey构建器
     */
    default <K, R> Map<K, R> batchGet(List<K> keyList, Function<K, Object> cacheKeyBuilder) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO
        Map<K, Object> keyMap = new HashMap<>();// <K, cacheKey>
        if (null != cacheKeyBuilder) {
            keyList.forEach(key -> keyMap.put(key, cacheKeyBuilder.apply(key)));
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

        keyMap.forEach((k, cacheKey) -> {
            // 仅仅获取
            V value = (V) this.getIfPresent(cacheKey);
            logger.debug("batchGet, cacheName={}, key={}, value={}", this.getCacheName(), cacheKey, value);
            if (null != value) {
                hitMap.put(k, value);
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
     * @param keyList         业务维度的key集合（K可能是自定义DTO）
     * @param cacheKeyBuilder 自定义的cacheKey构建器
     * @param valueLoader     值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<K, Object> cacheKeyBuilder, Function<List<K>, Map<K, V>> valueLoader) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO
        Map<K, Object> keyMap = new HashMap<>();// <K, cacheKey>
        if (null != cacheKeyBuilder) {
            keyList.forEach(key -> keyMap.put(key, cacheKeyBuilder.apply(key)));
        } else {
            keyList.forEach(key -> keyMap.put(key, key));
        }
        return this.batchGetOrLoad(keyMap, valueLoader);
    }

    /**
     * 批量get或load
     * 注：调用方自己组装好参数
     * 【特别注意】如果K是自定义DTO，那么必须重写hashCode()和equals(Object)，以便后续业务逻辑中可以通过K从hitMap中获取对应的数据
     *
     * @param keyMap      将List<K>转换后的 cacheKey Map
     * @param valueLoader 值加载器，返回的Map<K, V>对象中的 K 必须与传入的一致
     */
    default <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader) {
        try {
            // 获取命中缓存的数据列表
            // TODO 此处对于缓存了NullValue的key，会获取到null，导致继续往下执行，相当于会存在缓存穿透，所以需要看怎么修改 this.batchGet(keyMap)
            Map<K, V> hitCacheMap = this.batchGet(keyMap);

            if (null == valueLoader) {
                logger.info("batchGetOrLoad valueLoader is null return hitCacheMap, cacheName={}, cacheKeyList={}", this.getCacheName(), keyMap.values());
                return hitCacheMap;
            }

            // 过滤未命中缓存的key列表
            Map<K, Object> notHitCacheKeyMap = keyMap.entrySet().stream()
                    .filter(entry -> !hitCacheMap.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

            // 全部命中缓存，直接返回
            if (CollectionUtils.isEmpty(notHitCacheKeyMap) && !CollectionUtils.isEmpty(hitCacheMap)) {
                logger.info("batchGetOrLoad all_hit cache, cacheName={}, notHitCacheKeyList={}", this.getCacheName(), notHitCacheKeyMap.values());
                return hitCacheMap;
            }

            Map<K, V> valueLoaderHitMap = valueLoader.apply(new ArrayList<>(notHitCacheKeyMap.keySet()));

            // 从DB获取数据，一个都没有命中，直接返回
            if (CollectionUtils.isEmpty(valueLoaderHitMap)) {
                // 对未命中的key缓存空值，防止缓存穿透
                Map<Object, V> nullValueMap = new HashMap<>();
                notHitCacheKeyMap.forEach((k, cacheKey) -> {
                    nullValueMap.put(cacheKey, null);
                });
                this.batchPut(nullValueMap);
                logger.info("batchGetOrLoad all key is not exist, put null, cacheName={}, cacheKey={}", this.getCacheName(), nullValueMap.keySet());
                return hitCacheMap;
            }

            // 合并数据
            hitCacheMap.putAll(valueLoaderHitMap);

            // 将命中DB的数据按照cacheKey的方式来组装以便put到缓存
            // 如果K是自定义DTO，且没有重写hashCode()和equals(Object)，那么通过K从hitMap中可能获取不到数据，所以要特别注意
            Map<Object, V> batchPutDataMap = notHitCacheKeyMap.entrySet().stream()
                    .filter(entry -> valueLoaderHitMap.containsKey(entry.getKey()))
                    .collect(Collectors.toMap(entry -> entry.getValue(), entry -> valueLoaderHitMap.get(entry.getKey())));
            this.batchPut(batchPutDataMap);
            logger.info("batchGetOrLoad batch put hit db data, cacheName={}, notHitCacheKeyList={}", this.getCacheName(), batchPutDataMap.keySet());

            // 处理没有查询到数据的key，缓存空值，防止缓存穿透
            if (valueLoaderHitMap.size() != notHitCacheKeyMap.size()) {
                Map<Object, V> nullValueMap = new HashMap<>();
                notHitCacheKeyMap.forEach((k, cacheKey) -> {
                    if (!valueLoaderHitMap.containsKey(k)) {
                        nullValueMap.put(cacheKey, null);
                    }
                });
                this.batchPut(nullValueMap);
                logger.info("batchGetOrLoad key is not exist, put null, cacheName={}, cacheKey={}", this.getCacheName(), nullValueMap.keySet());
            }
            return hitCacheMap;
        } catch (Exception e) {
            logger.error("batchGetOrLoad error, keyList={}", keyMap.values(), e);
            throw new L2CacheException("batchGetOrLoad error," + e.getMessage());
        }
    }

    /**
     * 批量put
     *
     * @param dataMap         缓存数据集合（K可能是自定义DTO）
     * @param cacheKeyBuilder 自定义的cacheKey构建器
     */
    default <K, V> void batchPut(Map<K, V> dataMap, Function<K, Object> cacheKeyBuilder) {
        if (null == dataMap || dataMap.size() == 0) {
            return;
        }
        if (null == cacheKeyBuilder) {
            Map<Object, V> batchMap = new HashMap<>();
            dataMap.forEach((key, value) -> batchMap.put(key, value));
            this.batchPut(batchMap);
            return;
        }
        Map<Object, V> dataMapTemp = new HashMap<>();
        dataMap.forEach((key, value) -> {
            // 将 key 转换为cacheKey，因K可能是自定义DTO
            dataMapTemp.put(cacheKeyBuilder.apply(key), value);
        });
        this.batchPut(dataMapTemp);
    }

    /**
     * 批量put
     *
     * @param dataMap 缓存数据集合（K可能是自定义DTO）
     */
    default <V> void batchPut(Map<Object, V> dataMap) {
        if (CollectionUtils.isEmpty(dataMap)) {
            return;
        }
        dataMap.forEach((key, value) -> {
            this.put(key, value);
        });
    }
}
