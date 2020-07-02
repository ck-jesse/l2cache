package com.coy.l2cache.test;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.DefaultCacheExpiredListener;
import com.coy.l2cache.cache.builder.CaffeineCacheBuilder;
import com.coy.l2cache.cache.builder.CompositeCacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * @author chenck
 * @date 2020/7/2 16:21
 */
public class CacheBuilderTest {

    CacheConfig cacheConfig = new CacheConfig();

    @Before
    public void before() {
        // 默认配置 CAFFEINE
        cacheConfig.setCacheType(CacheType.CAFFEINE.name());
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=2s,recordStats")
                .setAutoRefreshExpireCache(true);
    }

    @Test
    public void caffeineCacheBuilderTest() throws InterruptedException {

        Cache cache = new CaffeineCacheBuilder()
                .cacheName("test")
                .cacheConfig(cacheConfig)
                .expiredListener(new DefaultCacheExpiredListener())
                .cacheSyncPolicy(null)
                .build();

        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));

        System.out.println("get " + cache.get(key));
        Thread.sleep(3000);// 缓存过期时间为为2s，此处休眠3s，目的是为了让缓存过期
        System.out.println("get " + cache.get(key));// 过期后第一次获取，可以获取到值，并且触发异步清理过期缓存
        System.out.println("get " + cache.get(key));// 过期后第二次获取，获取到null值
    }

    @Test
    public void compositeCacheBuilderTest() throws InterruptedException {
        // 组合缓存 CAFFEINE + NONE
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.NONE.name());

        Cache cache = new CompositeCacheBuilder()
                .cacheName("test")
                .cacheConfig(cacheConfig)
                .expiredListener(new DefaultCacheExpiredListener())
                .cacheSyncPolicy(null)
                .build();

        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));

        System.out.println("get " + cache.get(key));
        Thread.sleep(3000);// 缓存过期时间为为2s，此处休眠3s，目的是为了让缓存过期
        System.out.println("get " + cache.get(key));// 过期后第一次获取，可以获取到值，并且触发异步清理过期缓存
        System.out.println("get " + cache.get(key));// 过期后第二次获取，获取到null值
    }
}
