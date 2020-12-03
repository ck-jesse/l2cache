package com.coy.l2cache;


import com.coy.l2cache.load.LoadFunction;
import com.coy.l2cache.util.NullValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定义公共缓存操作的接口
 *
 * <b>Note:</b> 由于缓存的一般用途，建议实现允许存储null值（例如缓存返回{@code null}的方法）
 *
 * @author chenck
 * @date 2020/6/16 19:49
 */
public interface Cache {
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
     */
    Object get(Object key);

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
     * 注：只能用于相同的入参与出能类型查询
     * 注：仅仅获取缓存，缓存数据不存在时，不会加载。
     */
    default <T> Map<String, T> batchGet(List<String> keyList) {
        Map<String, T> resultMap = new HashMap<>();
        if (null == keyList || keyList.size() == 0) {
            return resultMap;
        }
        keyList.forEach(key -> {
            resultMap.put(key, (T) get(key));
        });
        return resultMap;
    }

    /**
     * 批量put
     */
        default <R, T> void batchPut(Map<R, T> dataMap) {
        if (null == dataMap || dataMap.size() == 0) {
            return;
        }
        dataMap.forEach((key, value) -> {
            put(key, value);
        });
    }

}
