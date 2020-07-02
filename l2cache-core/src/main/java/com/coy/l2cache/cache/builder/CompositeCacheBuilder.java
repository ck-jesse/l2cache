package com.coy.l2cache.cache.builder;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CompositeCache;
import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.provider.CacheSupport;
import com.coy.l2cache.cache.spi.ServiceLoader;
import org.springframework.util.StringUtils;

/**
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class CompositeCacheBuilder extends AbstractCacheBuilder<CompositeCache> {

    @Override
    public CompositeCache build(String cacheName) {
        String l1CacheType = this.getCacheConfig().getComposite().getL1CacheType();
        String l2CacheType = this.getCacheConfig().getComposite().getL2CacheType();
        if (StringUtils.isEmpty(l1CacheType) && StringUtils.isEmpty(l2CacheType)) {
            throw new IllegalArgumentException("must be configured l1CacheType and l2CacheType");
        }
        // 缓存类型为空时设置为 NoneCache
        if (StringUtils.isEmpty(l1CacheType)) {
            l1CacheType = CacheType.NONE.name();
        }
        if (StringUtils.isEmpty(l2CacheType)) {
            l2CacheType = CacheType.NONE.name();
        }
        if (l1CacheType.equalsIgnoreCase(l2CacheType)) {
            throw new IllegalArgumentException("l1CacheType and l2CacheType can't be the same, l1CacheType=" + l1CacheType);
        }

        // 构建L1
        Cache level1Cache = this.getCacheInstance(l1CacheType, cacheName);

        // 构建L2
        Cache level2Cache = this.getCacheInstance(l2CacheType, cacheName);

        return this.build(cacheName, level1Cache, level2Cache);
    }

    /**
     * 根据传入的L1和L2构建组合缓存
     */
    public CompositeCache build(String cacheName, Cache level1Cache, Cache level2Cache) {
        return new CompositeCache(cacheName, level1Cache, level2Cache);
    }

    /**
     * 获取缓存实例
     */
    private Cache getCacheInstance(String cacheType, String cacheName) {
        Cache cache = CacheSupport.getInstance(cacheType, cacheName);
        if (null != cache) {
            return cache;
        }
        // 基于SPI机制构建CacheBuilder
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, cacheType);
        cacheBuilder.copyFrom(this);

        return CacheSupport.getInstance(cacheType, cacheName, cacheBuilder);
    }
}
