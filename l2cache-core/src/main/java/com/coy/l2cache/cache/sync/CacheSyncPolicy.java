package com.coy.l2cache.cache.sync;

/**
 * 缓存同步策略
 *
 * @author chenck
 * @date 2020/6/29 17:45
 */
public interface CacheSyncPolicy {

    /**
     * 缓存变更时通知其他节点清理本地缓存
     *
     * @param key
     * @param optType 操作类型 refresh/clear
     */
    void publish(Object key, String optType);

}
