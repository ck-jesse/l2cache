package com.coy.l2cache.context;

import com.github.benmanes.caffeine.cache.RemovalListener;
import org.springframework.cache.CacheManager;

/**
 * @author chenck
 * @date 2020/5/14 20:58
 */
public interface ExtendCacheManager extends CacheManager {

    /**
     * 设置移除监听器
     */
    void setRemovalListener(RemovalListener<Object, Object> removalListener);

    /**
     * 清理缓存
     * 注：先删除redis，再发送clear消息，然后再删除本地缓存；其他节点接收到clear消息，调用ExtendCacheManager#clearLocalCache()清理本地缓存
     */
    void clear(String cacheName, Object key);

    /**
     * 清理本地缓存
     */
    void clearLocalCache(String cacheName, Object key);

    /**
     * 加载缓存
     */
    void load(String cacheName, Object key);

    /**
     * 刷新缓存
     */
    void refresh(String cacheName, Object key);

    /**
     * 判断是否为当前缓存实例
     */
    boolean currentCacheInstance(String instanceId);

}
