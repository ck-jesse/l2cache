package com.coy.l2cache.cache;

import com.coy.l2cache.cache.load.CacheLoader;
import com.coy.l2cache.cache.sync.CacheSyncPolicy;
import com.coy.l2cache.context.LoadFunction;

/**
 * 一级缓存
 *
 * @author chenck
 * @date 2020/6/30 10:54
 */
public interface L1Cache extends Cache {

    /**
     * 缓存同步策略
     * 注：因为一级缓存为本地缓存，所以需要进行不同节点间的缓存数据同步
     */
    CacheSyncPolicy getCacheSyncPolicy();

    /**
     * 缓存加载器
     */
    CacheLoader getCacheLoader();

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
     * 清理本地缓存
     */
    void clearLocalCache(Object key);

    /**
     * 异步加载{@code key}的新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see L1Cache#isLoadingCache() 为true才能执行refresh方法
     * @see LoadFunction#apply(Object)
     */
    void refresh(Object key);

    /**
     * 异步加载所有新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see L1Cache#isLoadingCache() 为true才能执行该方法
     * @see LoadFunction#apply(Object)
     */
    void refreshAll();

    /**
     * 刷新过期缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     *
     * @see L1Cache#isLoadingCache() 为true才能执行该方法
     */
    void refreshExpireCache(Object key);

    /**
     * 刷新所有过期的缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     *
     * @see L1Cache#isLoadingCache() 为true才能执行该方法
     */
    void refreshAllExpireCache();
}
