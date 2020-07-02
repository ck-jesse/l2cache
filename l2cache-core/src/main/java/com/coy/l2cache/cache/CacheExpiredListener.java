package com.coy.l2cache.cache;

/**
 * 缓存过期监听器
 *
 * @author chenck
 * @date 2020/7/1 20:54
 */
public interface CacheExpiredListener<K, V> {

    /**
     * 缓存过期后触发
     */
    void onExpired(K key, V value);
}
