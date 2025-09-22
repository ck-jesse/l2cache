package com.github.jesse.l2cache.cache;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.load.CacheLoader;
import com.github.jesse.l2cache.load.LoadFunction;

import java.util.Collection;
import java.util.Set;

/**
 * 一级缓存
 *
 * @author chenck
 * @date 2020/6/30 10:54
 */
public interface Level1Cache extends Cache {

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
     * 设置指定key的缓存项
     * 内部put方法，支持控制是否发送同步消息
     * <p>
     * 主要场景：一级缓存不存在，二级缓存存在，传入publishMessage=false，仅仅将缓存数据写入一级缓存，但不需要同步消息给其他节点
     *
     * @param key            缓存key
     * @param value          缓存值
     * @param publishMessage 是否发送同步消息，true=业务主动更新需要同步，false=从缓存加载无需同步
     */
    void put(Object key, Object value, boolean publishMessage);

    /**
     * 清理本地缓存
     */
    void clearLocalCache(Object key);

    /**
     * 异步加载{@code key}的新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see Level1Cache#isLoadingCache() 为true才能执行refresh方法
     * @see LoadFunction#apply(Object)
     */
    void refresh(Object key);

    /**
     * 异步加载所有新值
     * 当新值加载时，get操作将继续返回原值（如果有），除非将其删除;如果新值加载成功，则替换缓存中的前一个值。
     *
     * @see Level1Cache#isLoadingCache() 为true才能执行该方法
     * @see LoadFunction#apply(Object)
     */
    void refreshAll();

    /**
     * 刷新过期缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     *
     * @see Level1Cache#isLoadingCache() 为true才能执行该方法
     */
    void refreshExpireCache(Object key);

    /**
     * 刷新所有过期的缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     *
     * @see Level1Cache#isLoadingCache() 为true才能执行该方法
     */
    void refreshAllExpireCache();

    /**
     * returns the number in this cache
     */
    long size();

    /**
     * return all keys
     */
    default Set<Object> keys() {
        return null;
    }

    /**
     * return all values
     */
    default Collection<Object> values() {
        return null;
    }
}
