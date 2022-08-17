package com.github.jesse.l2cache;

import com.github.jesse.l2cache.spi.SPI;
import com.github.jesse.l2cache.sync.CacheMessage;
import com.github.jesse.l2cache.sync.MessageListener;

import java.io.Serializable;

/**
 * 缓存同步策略
 *
 * @author chenck
 * @date 2020/6/29 17:45
 */
@SPI
public interface CacheSyncPolicy extends Serializable {

    /**
     * 获取缓存配置
     */
    CacheConfig getCacheConfig();

    /**
     * 设置缓存配置
     */
    CacheSyncPolicy setCacheConfig(CacheConfig cacheConfig);

    /**
     * 获取缓存消息监听器
     */
    MessageListener getCacheMessageListener();

    /**
     * 设置缓存消息监听器
     */
    CacheSyncPolicy setCacheMessageListener(MessageListener cacheMessageListener);

    /**
     * 获取真实的Client实例
     */
    Object getActualClient();

    /**
     * 设置真实的Client实例
     * 注：留一个扩展点，可以直接设置应用中已经存在的Client实例，如：RedissonClient、RedisTemplate 等
     */
    CacheSyncPolicy setActualClient(Object actualClient);

    /**
     * 建立连接，并订阅消息
     */
    void connnect();

    /**
     * 发布，缓存变更时通知其他节点清理本地缓存
     *
     * @param message
     */
    void publish(CacheMessage message);

    /**
     * 断开连接
     */
    void disconnect();

}
