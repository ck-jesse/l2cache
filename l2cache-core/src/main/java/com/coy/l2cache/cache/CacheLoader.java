package com.coy.l2cache.cache;


import com.coy.l2cache.cache.sync.CacheSyncPolicy;

import java.util.concurrent.Callable;

/**
 * 缓存加载器
 *
 * @author chenck
 * @date 2020/7/2 10:21
 */
public interface CacheLoader<K, V> {
    /**
     * 设置加载数据的处理器
     * 注：在获取缓存时动态设置valueLoader，来达到实现不同缓存调用不同的加载数据逻辑的目的。
     */
    void addValueLoader(Object key, Callable<?> valueLoader);

    /**
     * 设置二级缓存
     */
    void setLevel2Cache(L2Cache level2Cache);

    /**
     * 设置缓存过期策略
     */
    void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy);

    /**
     * Computes or retrieves the value corresponding to {@code key}
     * 计算或检索对应的值
     */
    V load(K key) throws Exception;
}
