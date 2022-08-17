package com.github.jesse.l2cache.sync;

/**
 * @author chenck
 * @date 2020/7/7 15:33
 */
public interface MessageListener {

    /**
     * 缓存同步消息处理
     */
    void onMessage(CacheMessage message);
}
