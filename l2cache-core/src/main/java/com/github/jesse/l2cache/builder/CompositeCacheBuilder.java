package com.github.jesse.l2cache.builder;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.cache.Level2Cache;
import com.github.jesse.l2cache.cache.expire.CacheExpiry;
import com.github.jesse.l2cache.cache.expire.Level2CacheTtlCacheExpiry;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.spi.ServiceLoader;

/**
 * @author chenck
 * @date 2020/7/2 9:37
 */
public class CompositeCacheBuilder extends AbstractCacheBuilder<CompositeCache> {

    @Override
    public CompositeCache build(String cacheName) {
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(this.getL2CacheConfig(), cacheName);

        String l1CacheType = cacheConfig.getComposite().getL1CacheType();
        String l2CacheType = cacheConfig.getComposite().getL2CacheType();
        if (StrUtil.isEmpty(l1CacheType) && StrUtil.isEmpty(l2CacheType)) {
            throw new IllegalArgumentException("must be configured l1CacheType and l2CacheType");
        }
        // 缓存类型为空时设置为 NoneCache
        if (StrUtil.isEmpty(l1CacheType)) {
            l1CacheType = CacheType.NONE.name();
        }
        if (StrUtil.isEmpty(l2CacheType)) {
            l2CacheType = CacheType.NONE.name();
        }
        if (l1CacheType.equalsIgnoreCase(l2CacheType)) {
            throw new IllegalArgumentException("l1CacheType and l2CacheType can't be the same value " + l1CacheType);
        }
        // 限制不能为自己，否则会出现循环构建CompositeCache的情况
        if (l1CacheType.equalsIgnoreCase(CacheType.COMPOSITE.name()) || l2CacheType.equalsIgnoreCase(CacheType.COMPOSITE.name())) {
            throw new IllegalArgumentException("l1CacheType and l2CacheType can't be the CompositeCache, " +
                    "Otherwise, loop building CompositeCache causes java.lang.StackOverflowError");
        }

        // 针对L1的剩余过期时间处理策略
        Level2CacheTtlCacheExpiry cacheExpiry = new Level2CacheTtlCacheExpiry(cacheName, cacheConfig);

        // 构建L1
        Cache level1Cache = this.getCacheInstance(l1CacheType, cacheName, cacheExpiry);
        if (!(level1Cache instanceof Level1Cache)) {
            throw new IllegalArgumentException("level1Cache must be implements Level1Cache, l1CacheType=" + l1CacheType);
        }

        // 构建L2
        Cache level2Cache = this.getCacheInstance(l2CacheType, cacheName, null);
        if (!(level2Cache instanceof Level2Cache)) {
            throw new IllegalArgumentException("level2Cache must be implements Level2Cache, l2CacheType=" + l2CacheType);
        }

        return this.buildActualCache(cacheName, cacheConfig, (Level1Cache) level1Cache, (Level2Cache) level2Cache, this.getL2CacheConfig().getHotkey().getType());
    }

    /**
     * 构建组合缓存，传入L1和L2是为了与应用中已经存在的L1和L2进行集成
     */
    protected CompositeCache buildActualCache(String cacheName, L2CacheConfig.CacheConfig cacheConfig, Level1Cache level1Cache, Level2Cache level2Cache, String hotkeyType) {
        return new CompositeCache(cacheName, cacheConfig, level1Cache, level2Cache, hotkeyType);
    }

    /**
     * 获取缓存实例
     */
    private Cache getCacheInstance(String cacheType, String cacheName, CacheExpiry cacheExpiry) {
        Cache cache = CacheSupport.getCache(cacheType, cacheName);
        if (null != cache) {
            return cache;
        }
        // 基于SPI机制构建CacheBuilder
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, cacheType);
        cacheBuilder.copyFrom(this);
        if (null != cacheExpiry) {
            cacheBuilder.setCacheExpiry(cacheExpiry);
        }

        return CacheSupport.getCache(cacheType, cacheName, cacheBuilder);
    }
}
