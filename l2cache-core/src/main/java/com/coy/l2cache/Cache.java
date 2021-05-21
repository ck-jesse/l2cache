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
     *
     * @param keyList 业务维度的key集合（K可能是自定义DTO）
     */
    default <K, V> Map<K, V> batchGet(List<K> keyList) {
        return this.batchGet(keyList, null);
    }

    /**
     * 批量get
     *
     * @param keyList         业务维度的key集合（K可能是自定义DTO）
     * @param cacheKeyBuilder 自定义的cacheKey构建器
     */
    default <K, V> Map<K, V> batchGet(List<K> keyList, Function<K, Object> cacheKeyBuilder) {
        // 将keyList 转换为cacheKey，因K可能是自定义DTO，同时包含去重的能力
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
     * @param keyMap 缓存key集合, Map<K=表示DTO或其他基本类型, Object=完整的cacheKey>
     */
    default <K, V> Map<K, V> batchGet(Map<K, Object> keyMap) {
        return this.batchGet(keyMap, false);
    }

    /**
     * 批量get
     * <p>
     * 1.如果K是自定义DTO，那么必须重写hashCode()和equals()，以便后续业务逻辑中可以通过K从hitMap中获取对应的数据
     * <p>
     * 2.参数 returnNullValueKey=true 的作用：在batchGetOrLoad中调用batchGet时，把值为NullValue的key返回，表示该key存在缓存中，无需往下执行，防止缓存穿透到下层
     *
     * @param keyMap             缓存key集合, Map<K=表示DTO或其他基本类型, Object=完整的cacheKey>
     * @param returnNullValueKey true 表示把value=NullValue的key包含在Map中返回
     */
    default <K, V> Map<K, V> batchGet(Map<K, Object> keyMap, boolean returnNullValueKey) {
        return new HashMap<>();
    }


    /**
     * 批量get或load
     *
     * @param keyList     业务维度的key集合（K可能是自定义DTO）
     * @param valueLoader 值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<List<K>, Map<K, V>> valueLoader) {
        return this.batchGetOrLoad(keyList, null, valueLoader, false);
    }

    /**
     * 批量get或load
     *
     * @param keyList         业务维度的key集合（K可能是自定义DTO）
     * @param cacheKeyBuilder 自定义的cacheKey构建器
     * @param valueLoader     值加载器
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<K, Object> cacheKeyBuilder, Function<List<K>, Map<K, V>> valueLoader) {
        return this.batchGetOrLoad(keyList, cacheKeyBuilder, valueLoader, false);
    }

    /**
     * 批量get或load
     *
     * @param keyList            业务维度的key集合（K可能是自定义DTO）
     * @param cacheKeyBuilder    自定义的cacheKey构建器
     * @param valueLoader        值加载器
     * @param returnNullValueKey true 表示把value=NullValue的key包含在Map中返回
     */
    default <K, V> Map<K, V> batchGetOrLoad(List<K> keyList, Function<K, Object> cacheKeyBuilder, Function<List<K>, Map<K, V>> valueLoader, boolean returnNullValueKey) {
        // 如果keyList为空，则直接返回
        if (CollectionUtils.isEmpty(keyList)) {
            return new HashMap<>();
        }

        // 将keyList 转换为cacheKey，因K可能是自定义DTO
        Map<K, Object> keyMap = new HashMap<>();// <K, cacheKey>
        if (null != cacheKeyBuilder) {
            keyList.forEach(key -> keyMap.put(key, cacheKeyBuilder.apply(key)));
        } else {
            keyList.forEach(key -> keyMap.put(key, key));
        }
        return this.batchGetOrLoad(keyMap, valueLoader, returnNullValueKey);
    }

    /**
     * 批量get或load
     * <p>
     * 1.如果K是自定义DTO，那么必须重写hashCode()和equals()，以便后续业务逻辑中可以通过K从hitMap中获取对应的数据
     * <p>
     * 2.参数 returnNullValueKey=true 的作用：在batchGetOrLoad中调用batchGet时，把值为NullValue的key返回，表示该key存在缓存中，无需往下执行，防止缓存穿透到下层
     *
     * @param keyMap             缓存key集合, Map<K=表示DTO或其他基本类型, Object=完整的cacheKey>
     * @param valueLoader        值加载器，返回的Map<K, V>对象中的 K 必须与传入的一致
     * @param returnNullValueKey true 表示把value=NullValue的key包含在Map中返回
     */
    default <K, V> Map<K, V> batchGetOrLoad(Map<K, Object> keyMap, Function<List<K>, Map<K, V>> valueLoader, boolean returnNullValueKey) {
        return new HashMap<>();
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
        logger.info("[{}] batchPut cache, cacheName={}, size={}", this.getClass().getSimpleName(), this.getCacheName(), dataMap.size());
    }
}
