package com.coy.l2cache.cache;

/**
 * 缓存过期监听器
 *
 * @author chenck
 * @date 2020/7/1 20:54
 */
public interface CacheExpiredListener<K, V> {

    /**
     * 监听器通知缓存已过期，可以发送消息通知，记录日志等
     */
    void notifyElementExpired(K key, V value);
}
