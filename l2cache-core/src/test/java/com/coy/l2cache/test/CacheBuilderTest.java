package com.coy.l2cache.test;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.cache.DefaultCacheExpiredListener;
import com.coy.l2cache.builder.CacheBuilder;
import com.coy.l2cache.builder.CaffeineCacheBuilder;
import com.coy.l2cache.builder.CompositeCacheBuilder;
import com.coy.l2cache.builder.RedisCacheBuilder;
import com.coy.l2cache.config.CacheConfig;
import org.junit.Test;

/**
 * @author chenck
 * @date 2020/7/2 16:21
 */
public class CacheBuilderTest {

    @Test
    public void caffeineCacheBuilderTest() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
        // 默认配置 CAFFEINE
        cacheConfig.setCacheType(CacheType.CAFFEINE.name())
                .setAllowNullValues(true)
                .getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=2s,recordStats")
                .setAutoRefreshExpireCache(true);

        Cache cache = new CaffeineCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(null)
                .build("test");

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
        CacheConfig cacheConfig = new CacheConfig();
        // 组合缓存 CAFFEINE + NONE
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.NONE.name());
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=2s,recordStats")
                .setAutoRefreshExpireCache(true);

        Cache cache = new CompositeCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(null)
                .build("test");

        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));

        System.out.println("get " + cache.get(key));
        Thread.sleep(3000);// 缓存过期时间为为2s，此处休眠3s，目的是为了让缓存过期
        System.out.println("get " + cache.get(key));// 过期后第一次获取，可以获取到值，并且触发异步清理过期缓存
        System.out.println("get " + cache.get(key));// 过期后第二次获取，获取到null值
    }

    /**
     *
     */
    @Test
    public void redisCacheBuilderTest() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
        // 默认配置 CAFFEINE
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .getRedis()
                .setExpireTime(1000)
                .setMaxIdleTime(5000)
                .setMaxSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        CacheBuilder builder = new RedisCacheBuilder().setCacheConfig(cacheConfig);
        Cache cache1 = builder.build("test3");
        Cache cache2 = builder.build("test4");

        String key = "key3";

        cache1.put(key, "key1");
        System.out.println(String.format("cache1 put key=%s, value=key1", key));
        String value = (String) cache1.putIfAbsent(key, "key3");
        System.out.println(String.format("cache1 put key=%s, value=%s", key, value));

        cache2.put(key, "value2");
        System.out.println(String.format("cache2 put key=%s, value=value2", key));

        System.out.println("get " + cache1.get(key, String.class));
        Thread.sleep(4000);// 缓存过期时间为为2s，此处休眠3s，目的是为了让缓存过期
        System.out.println("get " + cache1.get(key));// 过期后第一次获取，获取到null值

        System.out.println("get " + cache2.get(key, String.class));// 过期后第一次获取，获取到null值
        while (true) {
            Thread.sleep(1000);
        }
    }

    @Test
    public void redisCacheBuilderTest1() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
        // 默认配置 CAFFEINE
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .getRedis()
                .setExpireTime(3000)
                .setMaxIdleTime(3000)
                .setMaxSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        CacheBuilder builder = new RedisCacheBuilder().setCacheConfig(cacheConfig);
        Cache cache1 = builder.build("test1");

        String key = "key3";

        cache1.put(key, "key1");
        System.out.println(String.format("cache1 put key=%s, value=key1", key));

        System.out.println("get " + cache1.get(key, String.class));
        Thread.sleep(4000);// 缓存过期时间为为2s，此处休眠3s，目的是为了让缓存过期
        System.out.println("get " + cache1.get(key));// 过期后第一次获取，获取到null值
        while (true) {
            Thread.sleep(1000);
        }
    }
}
