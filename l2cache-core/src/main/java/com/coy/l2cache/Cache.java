package com.coy.l2cache;

import com.coy.l2cache.context.LoadFunction;

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
    String getName();

    /**
     * 获取实际缓存对象
     */
    Object getActualCache();

    /**
     * 是否为 LoadingCache
     * 注意：如果是LoadingCache，则值由缓存自动加载，并存储在缓存中，直到被回收或手动失效，且LoadingCache一般会提供refresh()方法来刷新缓存。
     *
     * @see com.google.common.cache.LoadingCache
     * @see com.github.benmanes.caffeine.cache.LoadingCache
     * @see com.github.benmanes.caffeine.cache.AsyncLoadingCache
     */
    boolean isLoadingCache();

    /**
     * 获取指定key的缓存项
     */
    Object get(Object key);

    /**
     * 获取指定key的缓存项，并返回指定类型的返回值
     */
    <T> T get(Object key, Class<T> type);

    /**
     * 获取指定key的缓存项，如果缓存项不存在则通过{@code valueLoader}获取值
     * <p>
     * 含义：如果已缓存，则返回；否则，则创建、缓存并返回
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

    /**
     * 缓存变更时通知其他节点清理本地缓存
     *
     * @param key
     * @param optType 操作类型 refresh/clear
     */
    void cacheChangePush(Object key, String optType);

    /**
     * 清理本地缓存
     */
    void clearLocalCache(Object key);

    /**
     * 异步加载{@code key}的新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see LoadFunction#apply(Object)
     */
    void refresh(Object key);

    /**
     * 异步加载所有新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see LoadFunction#apply(Object)
     */
    void refreshAll();

    /**
     * 刷新过期缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     */
    void refreshExpireCache(Object key);

    /**
     * 刷新所有过期的缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     */
    void refreshAllExpireCache();
}
