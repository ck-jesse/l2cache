package com.coy.l2cache.context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;

/**
 * @author chenck
 * @date 2020/5/15 12:47
 */
public interface ExtendCache extends Cache {

    /**
     * 是否为LoadingCache
     */
    boolean isLoadingCache();

    /**
     * 获取redis过期时间(ms)
     */
    long getExpireTime();

    /**
     * build redis key
     */
    Object getRedisKey(@NonNull Object key);

    /**
     * get redis value
     */
    Object getRedisValue(@NonNull Object key);

    /**
     * set redis value
     */
    void setRedisValue(@NonNull Object key, Object value);

    /**
     * 值转换
     */
    Object toStoreValueWrap(@Nullable Object userValue);

    /**
     * 缓存变更时通知其他节点清理本地缓存
     *
     * @param key
     * @param optType 操作类型 refresh/clear
     */
    void cacheChangePush(@NonNull Object key, String optType);

    /**
     * 清理本地缓存
     */
    void clearLocalCache(Object key);

    /**
     * 异步加载{@code key}的新值
     * 如果新值加载成功，则替换缓存中的前一个值
     *
     * @see LoadFunction#apply(Object)
     */
    void refresh(@NonNull Object key);

    /**
     * 异步加载所有新值
     * 如果新值加载成功，则替换缓存中的前一个值
     *
     * @see LoadFunction#apply(Object)
     */
    void refreshAll();

    /**
     * 刷新过期缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     */
    void refreshExpireCache(@NonNull Object key);

    /**
     * 刷新所有过期的缓存
     * 注：通过LoadingCache.get(key)来刷新过期缓存，若缓存未到过期时间则不刷新
     */
    void refreshAllExpireCache();
}
