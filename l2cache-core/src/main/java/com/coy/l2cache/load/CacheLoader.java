package com.coy.l2cache.load;


import com.coy.l2cache.cache.Level2Cache;
import com.coy.l2cache.CacheSyncPolicy;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.concurrent.Callable;

/**
 * 缓存加载器
 *
 * @author chenck
 * @date 2020/7/2 10:21
 */
public interface CacheLoader<K, V> {
    /**
     * 设置加载数据的处理器
     * 注：在获取缓存时动态设置valueLoader，来达到实现不同缓存调用不同的加载数据逻辑的目的。
     */
    void addValueLoader(Object key, Callable<?> valueLoader);

    /**
     * 删除valueLoader
     */
    void delValueLoader(Object key);

    /**
     * 设置二级缓存
     */
    void setLevel2Cache(Level2Cache level2Cache);

    /**
     * 设置缓存过期策略
     */
    void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy);

    /**
     * 设置是否存储空值
     */
    void setAllowNullValues(boolean allowNullValues);

    /**
     * 存放NullValue的key，用于控制NullValue对象的有效时间
     */
    void setNullValueCache(Cache<Object, Integer> nullValueCache);

    /**
     * Computes or retrieves the value corresponding to {@code key}
     * 计算或检索对应的值
     */
    V load(K key);

}
