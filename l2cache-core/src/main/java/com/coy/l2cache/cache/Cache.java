package com.coy.l2cache.cache;

import java.util.concurrent.Callable;

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
     * 获取缓存名称
     */
    String getCacheName();

    /**
     * 缓存等级
     */
    String getLevel();

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
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
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
     * 如果指定的key不存在，则设置缓存项
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
     * 删除指定的缓存项（如果存在）
     */
    void evict(Object key);

    /**
     * 删除所有缓存项
     */
    void clear();

}
