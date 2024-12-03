package com.github.jesse.l2cache.spring.config;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.cache.expire.CacheExpiredListener;
import com.github.jesse.l2cache.spi.ServiceLoader;
import com.github.jesse.l2cache.spring.L2CacheProperties;
import com.github.jesse.l2cache.spring.biz.CacheManagerController;
import com.github.jesse.l2cache.spring.biz.SpringCacheNameRedissonClientInitService;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import com.github.jesse.l2cache.sync.CacheMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

/**
 * L2Cache Configuration
 *
 * @author chenck
 * @date 2020/4/29 10:58
 */
@EnableCaching(proxyTargetClass = true) // 启用spring-cache
@Configuration
@EnableConfigurationProperties(L2CacheProperties.class)
@Slf4j
public class L2CacheConfiguration {

    @Autowired
    L2CacheProperties l2CacheProperties;

    @Autowired(required = false)
    CacheExpiredListener expiredListener;

    /**
     * 当一个服务中存在一个或多个RedissonClient实例时，会匹配到名字为 redissonClient 的实例
     */
    @Autowired(required = false)
    @Qualifier("redissonClient")
    RedissonClient redissonClient;

    @Autowired
    ApplicationContext context;

    /**
     * 定义 加载多redis实例的初始化配置service
     */
    @Bean
    public SpringCacheNameRedissonClientInitService initService(ApplicationContext context) {
        return new SpringCacheNameRedissonClientInitService(context);
    }

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
        return new CacheMessageListener(L2CacheConfig.INSTANCE_ID);
    }

    /**
     * 定义 CacheManager
     */
    @Bean
    public L2CacheCacheManager cacheManager(CacheManagerCustomizers customizers) {
        L2CacheConfig l2CacheConfig = l2CacheProperties.getConfig();

        L2CacheCacheManager cacheManager = new L2CacheCacheManager(l2CacheConfig);

        // 创建缓存同步策略实例
        CacheSyncPolicy cacheSyncPolicy = createCacheSyncPolicy(l2CacheConfig);
        if (null != cacheSyncPolicy) {
            cacheManager.setCacheSyncPolicy(cacheSyncPolicy);
        }

        if (null != expiredListener) {
            cacheManager.setExpiredListener(expiredListener);
        }

        if (null != redissonClient) {
            cacheManager.setActualCacheClient(redissonClient);
        }

        // 扩展点，源码中有很多可以借鉴的点
        return customizers.customize(cacheManager);
    }

    @Bean
    public CacheManagerController cacheManagerController() {
        return new CacheManagerController();
    }

    /**
     * 创建缓存同步策略实例
     */
    private CacheSyncPolicy createCacheSyncPolicy(L2CacheConfig cacheConfig) {
        CacheSyncPolicy cacheSyncPolicy = ServiceLoader.load(CacheSyncPolicy.class, cacheConfig.getCacheSyncPolicy().getType());
        if (null == cacheSyncPolicy) {
            return null;
        }
        cacheSyncPolicy.setCacheConfig(cacheConfig);
        cacheSyncPolicy.setCacheMessageListener(cacheMessageListener());
        // redis 处理，以便重用
        if (null != redissonClient) {
            cacheSyncPolicy.setActualClient(redissonClient);
        }
        cacheSyncPolicy.connnect();// 启动订阅
        return cacheSyncPolicy;
    }

}
