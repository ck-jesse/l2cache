package com.coy.l2cache.spring.biz;

import com.coy.l2cache.Cache;
import com.coy.l2cache.exception.L2CacheException;
import com.coy.l2cache.spring.L2CacheCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

    @Override
    public R reload(K key) {
        // 抽取公共逻辑，简化开发
        R value = this.queryData(key);
        this.getNativeL2cache().put(this.buildCacheKey(key), value);
        return value;
    }

    @Override
    public Map<K, R> batchReload(List<K> keyList) {
        // 抽取公共逻辑，简化开发
        Map<K, R> value = this.queryDataList(keyList);
        this.getNativeL2cache().batchPut(value, k -> this.buildCacheKey(k));
        return value;
    }

    /**
     * 查询单个缓存数据
     *
     * @author chenck
     * @date 2022/8/4 22:02
     */
    protected abstract R queryData(K key);

    /**
     * 查询缓存数据列表
     * 注：返回的缓存数据列表，需要根据指定的格式来组装，由于不同的业务场景，可能组装的数据不一样，所以，下放到业务中去实现
     *
     * @author chenck
     * @date 2022/8/4 22:02
     */
    protected abstract Map<K, R> queryDataList(List<K> keyList);
}
