package com.coy.l2cache.sync;

/**
 * 缓存同步策略
 *
 * @author chenck
 * @date 2020/6/29 17:45
 */
public interface CacheSyncPolicy {

    /**
     * 发布，缓存变更时通知其他节点清理本地缓存
     *
     * @param message
     */
    void publish(CacheMessage message);

    /**
     * 订阅，
     *
     * @param
     */
    void subscribe();
}
