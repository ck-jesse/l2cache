package com.coy.l2cache.test;

import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.CacheSyncPolicy;
import com.coy.l2cache.builder.CaffeineCacheBuilder;
import com.coy.l2cache.cache.CaffeineCache;
import com.coy.l2cache.cache.expire.DefaultCacheExpiredListener;
import com.coy.l2cache.consts.CacheSyncPolicyType;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.sync.CacheMessageListener;
import com.coy.l2cache.sync.RedisCacheSyncPolicy;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CaffeineCache 中各个方法的单元测试
 */
public class CaffeineCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    CaffeineCache cache;
    Callable<String> callable;

    @Before
    public void before() {
        // 默认配置 CAFFEINE
        cacheConfig.setCacheType(CacheType.CAFFEINE.name())
                .setAllowNullValues(true)
                .getCaffeine()
                //.setDefaultSpec("initialCapacity=10,maximumSize=200,expireAfterWrite=2s,recordStats")
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=30s,recordStats")
                .setAutoRefreshExpireCache(false)
                .setRefreshPoolSize(3)
                .setRefreshPeriod(5L)
        ;

        cacheConfig.getCacheSyncPolicy()
                .setType(CacheSyncPolicyType.REDIS.name());

        // 构建缓存同步策略
        CacheSyncPolicy cacheSyncPolicy = new RedisCacheSyncPolicy()
                .setCacheConfig(cacheConfig)
                .setCacheMessageListener(new CacheMessageListener(cacheConfig.getInstanceId()))
                .setActualClient(Redisson.create());
        cacheSyncPolicy.connnect();//

        // 构建cache
        cache = (CaffeineCache) new CaffeineCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(cacheSyncPolicy)
                .build("localCache");

        callable = new Callable<String>() {
            AtomicInteger count = new AtomicInteger(1);

            @Override
            public String call() throws Exception {
//                String result = "loader_value" + count.getAndAdd(1);
//                System.out.println("loader value from valueLoader, return " + result);
//                return result;
                return null;
            }
        };

        System.out.println("cacheType: " + cache.getCacheType());
        System.out.println("cacheName: " + cache.getCacheName());
        System.out.println("actualCache: " + cache.getActualCache().getClass().getName());
        System.out.println();
    }

    // 因为get()可能会触发load操作，所以打印数据时使用该方法
    private void printAllCache() {
        System.out.println("L1 所有的缓存值");
        ConcurrentMap map1 = cache.getActualCache().asMap();
        map1.forEach((o1, o2) -> {
            System.out.println(String.format("key=%s, value=%s", o1, o2));
        });
        System.out.println();
    }

    private void printCache(Object key) {
        ConcurrentMap map1 = cache.getActualCache().asMap();
        Object value = map1.get(key);
        System.out.println(String.format("L1 缓存值 key=%s, value=%s", key, value));
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
        while (true) {
            Thread.sleep(2000);
            System.out.println(String.format("get key=%s, value=%s", key, cache.get(key)));
            System.out.println(String.format("get callable key=%s, value=%s", key, cache.get(key, callable)));
        }
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
    public void clearTest() throws InterruptedException {
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

    @Test
    public void clearLocalCacheTest() throws InterruptedException {
        String key1 = "key_loader1";
        cache.get(key1, callable);
        printAllCache();

        cache.clearLocalCache(key1);
        printAllCache();
    }

    @Test
    public void refreshTest() throws InterruptedException {
        String key1 = "key_loader1";
        cache.get(key1, callable);
        printAllCache();

        cache.refresh(key1);
        printAllCache();
    }

    @Test
    public void refreshAllTest() throws InterruptedException {
        String key1 = "key_loader1";
        String key2 = "key_loader2";
        cache.get(key1, callable);
        cache.get(key2, callable);
        printAllCache();

        cache.refreshAll();
        printAllCache();
    }

    @Test
    public void refreshExpireCacheTest() throws InterruptedException {
        String key = "key_loader1";
        cache.get(key, callable);

        // 未过期时refresh，不加载
        cache.refreshExpireCache(key);
        printAllCache();

        Thread.sleep(2000);
        // 过期时refresh，加载新值
        cache.refreshExpireCache(key);
        printAllCache();
    }

    @Test
    public void refreshAllExpireCacheTest() throws InterruptedException {
        String key1 = "key_loader1";
        String key2 = "key_loader2";
        String key3 = "key_loader3";
        cache.get(key1, callable);
        cache.get(key2, callable);
        cache.get(key3, callable);

        // 未过期时，不会触发加载新值
        cache.refreshAllExpireCache();
        printAllCache();

        Thread.sleep(2000);
        // 过期时，触发加载新值
        cache.refreshAllExpireCache();
        printAllCache();
    }

    @Test
    public void batchPut() {
        Map<Object, User> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put("key" + i, new User("name" + i, "addr" + i));
        }
        System.out.println(map);

        // 批量put
        cache.batchPut(map);

        // key 完全匹配
        List<Object> keyList = new ArrayList<>(map.keySet());
        Map<Object, Object> list1 = cache.batchGetObject(keyList);
        System.out.println(list1);

        // key 完全匹配
        Map<Object, Object> list2 = cache.batchGetObject(keyList);
        System.out.println(list2);

        // key 全部存在(少于缓存中的key)
        keyList.remove(1);
        list1 = cache.batchGetObject(keyList);
        System.out.println(list1);

        // key 部分存在缓存，部分不存在缓存
        keyList.add("other");
        list1 = cache.batchGetObject(keyList);
        System.out.println(list1);
    }

    @Test
    public void batchGet() {
        List<Object> keyList = new ArrayList<>();

    }

}
