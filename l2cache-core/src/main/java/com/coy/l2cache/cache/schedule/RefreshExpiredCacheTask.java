package com.coy.l2cache.cache.schedule;

import com.coy.l2cache.cache.L1Cache;

/**
 * 刷新过期缓存Task
 * <p>
 * 该Task的主要目的是尽可能的保证 L1Cache 中是最新的数据。如guava、caffeine在访问时，若数据过期则先返回旧数据，再执行数据加载。
 * 如果 L1Cache 是 LoadingCache，并且自定义CuntomCacheLoader中 L2Cache 不为空，则同时刷新L1Cache和L2Cache。
 *
 * @author chenck
 * @date 2020/7/6 9:58
 */
public class RefreshExpiredCacheTask implements Runnable {

    private final L1Cache l1Cache;

    public RefreshExpiredCacheTask(L1Cache l1Cache) {
        this.l1Cache = l1Cache;
    }

    @Override
    public void run() {
        this.l1Cache.refreshAllExpireCache();
    }
}
