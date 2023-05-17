package com.github.jesse.l2cache.spring.biz;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.biz.CacheService;
import com.github.jesse.l2cache.exception.L2CacheException;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 抽象缓存类
 * 定义该类的初始目的是为了简化 get()和isExist() 的开发，抽象公共逻辑，避免每个实现类中都定义类似的代码逻辑。
 *
 * @author chenck
 * @date 2021/4/13 14:04
 */
@Component
public abstract class AbstractCacheService<K, R> implements CacheService<K, R> {

    @Autowired
    L2CacheCacheManager l2CacheCacheManager;

    @Override
    public Cache getNativeL2cache() {
        Cache cache = (Cache) l2CacheCacheManager.getCache(this.getCacheName()).getNativeCache();
        if (null == cache) {
            throw new L2CacheException("未找到Cache对象，请检查缓存配置是否正确");
        }
        return cache;
    }

}
