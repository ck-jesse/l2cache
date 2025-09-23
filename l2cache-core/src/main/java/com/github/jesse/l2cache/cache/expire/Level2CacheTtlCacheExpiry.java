package com.github.jesse.l2cache.cache.expire;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.cache.Level2Cache;
import com.github.jesse.l2cache.cache.NoneCache;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.CacheSupport;
import com.github.jesse.l2cache.util.ExpireTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义缓存过期策略：从L2(Redis)中获取key的TTL，并设置为L1(Caffeine)的过期时间，实现多节点L1缓存过期时间的统一
 * <p>
 * 说明：当缓存类型为composite时，才需要从L2(redis)中获取key的过期时间
 * <p>
 * 1、优点：
 * - 根本解决过期时间不一致问题：所有节点的一级缓存过期时间与Redis保持同步
 * - 数据一致性更强：避免了某些节点持有已过期数据的情况
 * - 配置灵活：可以通过开关控制是否启用该功能
 * 2、缺点：
 * - 性能开销：每次缓存新增或修改操作都需要查询Redis TTL
 * - 网络依赖：依赖于与Redis的网络连接稳定性
 * - 复杂度增加：增加了系统的复杂性
 * 3、适用场景：
 * - 对缓存一致性要求极高的业务场景
 * - 缓存命中率较高，TTL查询开销相对可控的场景
 * - Redis网络环境稳定的场景
 *
 * @author chenck
 * @date 2025/1/15 10:00
 */
public class Level2CacheTtlCacheExpiry implements CacheExpiry<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(Level2CacheTtlCacheExpiry.class);

    private final String cacheName;
    private final L2CacheConfig.CacheConfig cacheConfig;
    private long defaultExpireTime;

    public Level2CacheTtlCacheExpiry(String cacheName, L2CacheConfig.CacheConfig cacheConfig) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
    }

    public Level2CacheTtlCacheExpiry(String cacheName, L2CacheConfig.CacheConfig cacheConfig, long defaultExpireTime) {
        this.cacheName = cacheName;
        this.cacheConfig = cacheConfig;
        this.defaultExpireTime = defaultExpireTime;
    }

    @Override
    public void setDefaultExpireTime(long defaultExpireTime) {
        this.defaultExpireTime = defaultExpireTime;
    }

    /**
     * 从L2(Redis)中获取key的过期时间
     */
    @Override
    public long getTtl(Object key, Object value) {
        try {
            // 只有composite缓存，才需要从L2中获取key的剩余过期时间
            if (!CacheType.COMPOSITE.name().equalsIgnoreCase(cacheConfig.getCacheType())) {
                logger.warn("[TTL]缓存类型不为composite, 使用默认过期时间, cacheName={}, cacheType={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, cacheConfig.getCacheType(), key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime));
                return defaultExpireTime;
            }

            Level2Cache level2Cache = CacheSupport.getLevel2Cache(cacheConfig.getComposite().getL2CacheType(), cacheName);
            if (level2Cache == null) {
                logger.warn("[TTL] 未找对二级缓存对象, 使用默认过期时间, cacheName={}, cacheType={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, cacheConfig.getCacheType(), key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime));
                return defaultExpireTime;
            }
            if (level2Cache instanceof NoneCache) {
                logger.warn("[TTL] 二级缓存对象为NoneCache, 使用默认过期时间, cacheName={}, cacheType={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, cacheConfig.getCacheType(), key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime));
                return defaultExpireTime;
            }

            // 获取L2中key的剩余过期时间（毫秒）
            long remainTimeToLive = level2Cache.getTimeToLive(key);

            if (remainTimeToLive > 0) {
                logger.info("[TTL] L2存在且有过期时间，用于设置为一级缓存的过期时间, cacheName={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, key, remainTimeToLive, ExpireTimeUtil.toStr(remainTimeToLive));
                return remainTimeToLive;
            } else if (remainTimeToLive == -1) {
                // Redis key存在但没有过期时间（永不过期）
                logger.info("[TTL] L2存在但没有过期时间, 使用默认过期时间, cacheName={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime));
                return defaultExpireTime;
            } else {
                // Redis key不存在或已过期
                logger.info("[TTL] L2不存在或已过期, 使用默认过期时间, cacheName={}, key={}, ttl={}ms, expireTimeStr={}", cacheName, key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime));
                return defaultExpireTime;
            }
        } catch (Exception e) {
            logger.warn("[TTL] L2剩余过期时间获取异常, 使用默认过期时间, cacheName={}, key={}, ttl={}ms, expireTimeStr={}, error={}", cacheName, key, defaultExpireTime, ExpireTimeUtil.toStr(defaultExpireTime), e.getMessage());
            return defaultExpireTime;
        }
    }

}