package com.coy.l2cache.test;

import com.coy.l2cache.cache.Cache;
import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.DefaultCacheExpiredListener;
import com.coy.l2cache.cache.builder.CompositeCacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenck
 * @date 2020/7/3 17:18
 */
public class CompositeCacheBuilderTest {

    /**
     * 组合缓存 CAFFEINE + NONE 测试 -> 相当于只有一个本地缓存
     * <p>
     * Cache#put(key, value) 和 Cache#get(key) 测试
     */
    @Test
    public void compositeCacheBuilderTest() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
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
     * 组合缓存 CAFFEINE + REDIS 测试
     * <p>
     * Cache#put(key, value) 和 Cache#get(key) 测试
     */
    @Test
    public void compositeCacheBuilderTest1() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.REDIS.name());
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=5s,recordStats")
                .setAutoRefreshExpireCache(true);
        cacheConfig.getRedis()
                .setExpireTime(5000)
                .setMaxIdleTime(5000)
                .setMaxSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        Cache cache = new CompositeCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(null)
                .build("composite");

        String key = "key";
        String value = "value";
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));

        // 循环获取缓存
        // 缓存过期后第一次获取，可以获取到值并返回旧值
        // 并且触发异步清理过期缓存，并且触发 CustomCacheLoader.load()，因为是没有设置具体的valueLoader，所以返回null
        // 缓存过期后第二次获取，获取到null值
        while (true) {
            Thread.sleep(1000);
            System.out.println("get " + cache.get(key));
        }
    }

    /**
     * 组合缓存 CAFFEINE + REDIS 测试
     * <p>
     * CompositeCache#get(key, callable) 和 CompositeCache#get(key) 测试
     */
    @Test
    public void compositeCacheBuilderTest2() throws InterruptedException {
        CacheConfig cacheConfig = new CacheConfig();
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.REDIS.name());
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=5s,recordStats")
                .setAutoRefreshExpireCache(true);
        cacheConfig.getRedis()
                .setExpireTime(5000)
                .setMaxIdleTime(5000)
                .setMaxSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        Cache cache = new CompositeCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(null)
                .build("composite");

        // 请注意redis中key的值的变化
        String key = "key";
        Object value = cache.get(key, new Callable<Object>() {
            AtomicInteger count = new AtomicInteger(1);

            @Override
            public Object call() throws Exception {
                return "value_" + count.getAndAdd(2);
            }
        });
        System.out.println("get " + value);

        // 循环获取缓存
        // 缓存过期后第一次获取，可以获取到值并返回旧值
        // 并且触发异步清理过期缓存，并且触发 CustomCacheLoader.load()，因为上面有设置具体的valueLoader，所以返回会执行valueLoader获取新的值
        // 缓存过期后第二次获取，获取到新的值
        while (true) {
            Thread.sleep(2000);
            System.out.println("get " + cache.get(key));
        }
    }
}
