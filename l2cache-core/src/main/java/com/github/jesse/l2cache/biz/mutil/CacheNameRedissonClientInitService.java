package com.github.jesse.l2cache.biz.mutil;

/**
 * 加载多redis实例场景的初始化配置Service
 * 说明：目前仅实现Spring方式的初始化，基于class获取下面所有子类的方式暂不做实现，使用者可按需实现
 *
 * @author chenck
 * @date 2024/12/3 16:41
 */
public interface CacheNameRedissonClientInitService {

    /**
     * 初始化 cacheNameRedissonClientMap
     */
    void initCacheNameRedissonClientMap();

    /**
     * 初始化 redissonClientMap
     */
    void initRedissonClientMap();
}
