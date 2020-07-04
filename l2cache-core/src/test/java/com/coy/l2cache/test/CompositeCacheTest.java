package com.coy.l2cache.test;

import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.CompositeCache;
import com.coy.l2cache.cache.DefaultCacheExpiredListener;
import com.coy.l2cache.cache.NullValue;
import com.coy.l2cache.cache.builder.CompositeCacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RMap;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CompositeCache 中各个方法的单元测试
 *
 * @author chenck
 * @date 2020/7/3 17:18
 */
public class CompositeCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    CompositeCache cache;
    Callable<String> callable;

    @Before
    public void before() {
        // 组合缓存 CAFFEINE + REDIS 测试
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .setAllowNullValues(true)
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.REDIS.name());
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=5s,recordStats")
                .setAutoRefreshExpireCache(true);
        cacheConfig.getRedis()
                .setExpireTime(5000)
                .setMaxIdleTime(5000)
                .setMaxSize(200)// 注意如果与caffeine中最大数量大小不一致，容易造成歧义，所以
                .setRedissonYamlConfig("redisson.yaml");

        cache = (CompositeCache) new CompositeCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(null)
                .build("compositeCache");

        callable = new Callable<String>() {
            AtomicInteger count = new AtomicInteger(1);

            @Override
            public String call() throws Exception {
                String result = "loader_value" + count.getAndAdd(1);
                System.out.println("loader value from valueLoader, return " + result);
                return result;
            }
        };

        System.out.println("cacheName: " + cache.getCacheName());
        System.out.println("level: " + cache.getCacheName());
        System.out.println("actualCache: " + cache.getActualCache().getClass().getName());
        System.out.println();
    }

    // 因为get()可能会触发load操作，所以打印数据时使用该方法
    private void printAllCache() {
        // L1
        System.out.println("L1 所有的缓存值");
        ConcurrentMap map1 = ((Cache) cache.getLevel1Cache().getActualCache()).asMap();
        map1.forEach((o1, o2) -> {
            System.out.println(String.format("key=%s, value=%s", o1, o2));
        });
        System.out.println();
        // L2
        System.out.println("L2 所有的缓存值");
        Map map2 = ((RMap) cache.getLevel2Cache().getActualCache()).readAllMap();
        map2.forEach((o1, o2) -> {
            System.out.println(String.format("key=%s, value=%s", o1, o2));
        });
        System.out.println();
    }

    private void printCache(Object key) {
        ConcurrentMap map1 = ((Cache) cache.getLevel1Cache().getActualCache()).asMap();
        System.out.println(String.format("L1 缓存值 key=%s, value=%s", key, map1.get(key)));
        System.out.println();

        Object value = cache.getLevel2Cache().get(key);
        System.out.println(String.format("L2 缓存值 key=%s, value=%s", key, value));
        System.out.println();
    }

    @Test
    public void putNullTest() throws InterruptedException {
        String key = "key_null";
        cache.put(key, null);
        printCache(key);
        System.out.println(cache.get(key));
    }

    @Test
    public void putAndGetTest() throws InterruptedException {
        String key = "key1";
        String value = "value1";

        // 1 put and get
        cache.put(key, value);
        printCache(key);

        Object value1 = cache.get(key);
        System.out.println(String.format("get key=%s, value=%s", key, value1));
        System.out.println();

        // 2 put and get(key, type)
        cache.put(key, NullValue.INSTANCE);
        printCache(key);

        NullValue value2 = cache.get(key, NullValue.class);
        System.out.println(String.format("get key=%s, value=%s", key, value2));
        System.out.println();
    }

    @Test
    public void getAndLoadTest() throws InterruptedException {
        // 3 get and load from Callable
        String key = "key_loader";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println(String.format("get key=%s, value=%s", key, cache.get(key)));
    }

    @Test
    public void putIfAbsentTest() throws InterruptedException {
        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        printCache(key);

        // key1 已经存在，所有putIfAbsent失败，并返回已经存在的值value1
        Object oldValue = cache.putIfAbsent(key, "value123");
        System.out.println(String.format("putIfAbsent key=%s, oldValue=%s", key, oldValue));
        System.out.println();

        // newkey1 不存在，putIfAbsent成功，并返回null
        String newkey1 = "newkey1";
        oldValue = cache.putIfAbsent(newkey1, "newvalue1");
        System.out.println(String.format("putIfAbsent key=%s, oldValue=%s, value=%s", newkey1, oldValue, cache.get(newkey1)));
        System.out.println();

        System.out.println("缓存中所有的元素");
        printAllCache();
    }

    @Test
    public void evictTest() throws InterruptedException {
        String key = "key1";
        String value = "value1";
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));
        System.out.println();

        printCache(key);
        // 删除指定的缓存项
        cache.evict(key);
        printCache(key);
    }

    @Test
    public void clearTest() {
        // 初始化缓存项
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, "value" + i);
        }

        System.out.println("clear前：缓存中所有的元素");
        printAllCache();

        cache.clear();
        System.out.println("clear后：缓存中所有的元素");
        printAllCache();
    }
}
