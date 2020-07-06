package com.coy.l2cache.cache;

/**
 * 二级缓存
 *
 * @author chenck
 * @date 2020/6/30 11:03
 */
public interface Level2Cache extends Cache {

    /**
     * 获取redis过期时间(ms)
     */
    long getExpireTime();

    /**
     * 构建key
     */
    Object buildKey(Object key);
}
