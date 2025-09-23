package com.github.jesse.l2cache.cache.expire;

/**
 * 自定义过期策略:获取缓存剩余过期时间策略
 * 主要是针对一级缓存定义该功能，如guava cache和caffeine，实现多节点L1缓存过期时间的统一
 *
 * @author chenck
 * @date 2025/9/23 19:37
 */
public interface CacheExpiry<K, V> {

    /**
     * 设置默认剩余过期时间（毫秒）
     *
     * @param defaultExpireTime 默认剩余过期时间（毫秒）
     */
    void setDefaultExpireTime(long defaultExpireTime);

    /**
     * 获取剩余过期时间（毫秒）
     *
     * @param key         缓存key
     * @param value       缓存value
     */
    long getTtl(K key, V value);

}
