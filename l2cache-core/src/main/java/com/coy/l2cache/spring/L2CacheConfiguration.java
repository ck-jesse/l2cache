package com.coy.l2cache.spring;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSyncPolicy;
import com.coy.l2cache.cache.expire.CacheExpiredListener;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.spi.ServiceLoader;
import com.coy.l2cache.sync.CacheMessageListener;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

/**
 * L2Cache Configuration
 *
 * @author chenck
 * @date 2020/4/29 10:58
 */
@EnableCaching // 启用spring-cache
@Configuration
@EnableConfigurationProperties(L2CacheProperties.class)
public class L2CacheConfiguration {

    @Autowired
    L2CacheProperties l2CacheProperties;

    @Autowired(required = false)
    CacheExpiredListener expiredListener;

    @Autowired(required = false)
    RedissonClient redissonClient;

    /**
     * 自定义缓存管理器
     */
    @Bean
    public CacheManagerCustomizers cacheManagerCustomizers(ObjectProvider<CacheManagerCustomizer<?>> customizers) {
        return new CacheManagerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
    }

    /**
     * 定义 CacheMessageListener
     */
    @Bean
    public CacheMessageListener cacheMessageListener() {
        return new CacheMessageListener(l2CacheProperties.getConfig().getInstanceId());
    }

    /**
     * 定义 CacheManager
     */
    @Bean
    public L2CacheCacheManager cacheManager(CacheManagerCustomizers customizers) {
        CacheConfig cacheConfig = l2CacheProperties.getConfig();

        L2CacheCacheManager cacheManager = new L2CacheCacheManager(cacheConfig);

        // 创建缓存同步策略实例
        CacheSyncPolicy cacheSyncPolicy = createCacheSyncPolicy(cacheConfig);
        if (null != cacheSyncPolicy) {
            cacheManager.setCacheSyncPolicy(cacheSyncPolicy);
        }

        if (null != expiredListener) {
            cacheManager.setExpiredListener(expiredListener);
        }

        if (null != redissonClient && isUseRedis(cacheConfig)) {
            cacheManager.setActualCacheClient(redissonClient);
        }

        // 扩展点，源码中有很多可以借鉴的点
        return customizers.customize(cacheManager);
    }

    /**
     * 判断是否使用redis
     */
    private boolean isUseRedis(CacheConfig cacheConfig) {
        String cacheType = cacheConfig.getCacheType();
        if (CacheType.REDIS.name().equalsIgnoreCase(cacheType)) {
            return true;
        }
        if (CacheType.COMPOSITE.name().equalsIgnoreCase(cacheType)) {
            cacheType = cacheConfig.getComposite().getL2CacheType();
            if (CacheType.REDIS.name().equalsIgnoreCase(cacheType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建缓存同步策略实例
     */
    private CacheSyncPolicy createCacheSyncPolicy(CacheConfig cacheConfig) {
        CacheSyncPolicy cacheSyncPolicy = ServiceLoader.load(CacheSyncPolicy.class, cacheConfig.getCacheSyncPolicy().getType());
        if (null == cacheSyncPolicy) {
            return null;
        }
        cacheSyncPolicy.setCacheConfig(cacheConfig);
        cacheSyncPolicy.setCacheMessageListener(cacheMessageListener());
        // redis 处理，以便重用
        if (null != redissonClient && isUseRedis(cacheConfig)) {
            cacheSyncPolicy.setActualClient(redissonClient);
        }
        cacheSyncPolicy.connnect();// 启动订阅
        return cacheSyncPolicy;
    }

}
