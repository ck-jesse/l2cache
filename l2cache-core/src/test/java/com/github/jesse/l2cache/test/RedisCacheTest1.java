package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.builder.RedisCacheBuilder;
import com.github.jesse.l2cache.cache.RedissonRBucketCache;
import com.github.jesse.l2cache.consts.CacheType;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RedissonCache 中各个方法的单元测试
 */
public class RedisCacheTest1 {

    CacheConfig cacheConfig = new CacheConfig();
    RedissonRBucketCache cache;
    Callable<String> callable;

    @Before
    public void before() {
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .setAllowNullValues(true)
                .getRedis()
                .setExpireTime(300000)
                .setLock(true)
//                .setDuplicate(true)
//                .setDuplicateALlKey(false)
//                .setDefaultDuplicateSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        // 模拟应用中已经存在 RedissonClient
        RedissonClient redissonClient = Redisson.create(cacheConfig.getRedis().getRedissonConfig());

        RedisCacheBuilder builder = (RedisCacheBuilder) new RedisCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setActualCacheClient(redissonClient);

        // 构建cache
        cache = builder.build("redisCache");

//        cacheConfig.getRedis().getDuplicateCacheNameMap().put(cache.getCacheName(), 3);
//        cacheConfig.getRedis().getDuplicateKeyMap().put("redisCache:user_key", 5);

        callable = new Callable<String>() {
            AtomicInteger count = new AtomicInteger(1);

            @Override
            public String call() throws Exception {
                String result = "loader_value" + count.getAndAdd(1);
                System.out.println("loader value from valueLoader, return " + result);
                return result;
            }
        };

        System.out.println("cacheType: " + cache.getCacheType());
        System.out.println("cacheName: " + cache.getCacheName());
        System.out.println("actualCache: " + cache.getActualCache().getClass().getName());
        System.out.println();
    }


    private void printCache(Object key) {
        Object value = cache.get(key);
        System.out.println(String.format("L2 缓存值 key=%s, value=%s", key, value));
        System.out.println();
    }

    @Test
    public void test() {
        System.out.println(TimeUnit.MILLISECONDS.toMillis(5000));
    }

    @Test
    public void putNullTest() throws InterruptedException {
        String key = "key_null";
        cache.put(key, null);
        printCache(key);
        System.out.println(cache.get(key));
    }

    @Test
    public void putUserTest() throws InterruptedException {
        String key = "user_keybbb";
        User user = new User();
        user.setName("test");
        user.setAddr(key);
        user.setCurrTime(System.currentTimeMillis());
        cache.put(key, user);
        printCache(key);
    }

    @Test
    public void putAndGetTest() throws InterruptedException {
        String key = "key";
        String value = "valueaaaaaa";

//        cacheConfig.getRedis().setDuplicate(false);
        cache.put(key, value);
        printCache(key);

//        cacheConfig.getRedis().setDuplicate(false);
        cache.put(key, value);
        printCache(key);

        value = cache.get(key, String.class);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println();

//        cacheConfig.getRedis().setDuplicate(false);
        // 2 put and get(key, type)
        String key1 = "key111";
        cache.put(key1, "NullValue.INSTANCEaaaaaa");
        printCache(key1);

//        NullValue value1 = cache.get(key1, NullValue.class);
        String value1 = cache.get(key1, String.class);
        System.out.println(String.format("get key1=%s, value1=%s", key1, value1));
        System.out.println();
    }

    @Test
    public void getAndLoadTest() throws InterruptedException {
        // 3 get and load from Callable
        String key = "key_loader123";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));

//        cacheConfig.getRedis().setDuplicate(false);
        System.out.println(cache.get(key));
    }

    @Test
    public void putIfAbsentTest() throws InterruptedException {
        String key = "key2";
        String value = "value2";
//        cacheConfig.getRedis().setDuplicate(true);
        cache.put(key, value);
        printCache(key);

//        cacheConfig.getRedis().setDuplicate(false);
        // key1 已经存在，所有putIfAbsent失败，并返回已经存在的值value1
        Object oldValue = cache.putIfAbsent(key, "value123");
        System.out.println(String.format("putIfAbsent key=%s, oldValue=%s", key, oldValue));
        System.out.println();

        // newkey1 不存在，putIfAbsent成功，并返回null
        String newkey1 = "newkey2";
        oldValue = cache.putIfAbsent(newkey1, "newvalue1");
        System.out.println(String.format("putIfAbsent key=%s, oldValue=%s, value=%s", newkey1, oldValue, cache.get(newkey1)));
        System.out.println();

        System.out.println("缓存中所有的元素");
    }

    @Test
    public void evictTest() throws InterruptedException {
        String key = "key1";
        String value = "value1";
        // 此时会有副本
//        cacheConfig.getRedis().setDuplicate(true);
        cache.put(key, value);
        System.out.println(String.format("put key=%s, value=%s", key, value));
        System.out.println();

//        cacheConfig.getRedis().setDuplicate(false);

        printCache(key);
        // 删除指定的缓存项
        cache.evict(key);
        printCache(key);

        // 此时会有副本
//        cacheConfig.getRedis().setDuplicate(true);
        cache.put(key, "valuebbbb");

        // 此时副本会删除
//        cacheConfig.getRedis().setDuplicate(false);
        cache.put(key, "valuecccc");
    }

    @Test
    public void clearTest() throws InterruptedException {
        // 初始化缓存项
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, "value" + i);
        }

        System.out.println("clear前：缓存中所有的元素");

        cache.clear();
        System.out.println("clear后：缓存中所有的元素");
    }

    @Test
    public void isExistsTest() throws InterruptedException {
        String key = "key1";
        cache.put(key, "value");
        boolean rslt = cache.isExists(key);
        System.out.println(rslt);
    }


    @Test
    public void batchPut() {
        Map<Object, User> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put("user" + i, new User("name" + i, "addr" + i));
        }
        System.out.println("batch put " + map);

        // 批量put
        cache.batchPut(map);

//        // key 完全匹配
//        List<Object> keyList = new ArrayList<>(map.keySet());
//        Map<Object,Object> list1 = cache.batchGetObject(keyList);
//        System.out.println("batch get 1" + list1);
//
//        // key 完全匹配
//        Map<Object,Object> list2 = cache.batchGetObject(keyList);
//        System.out.println("batch get 2" + list2);
//
//        // key 全部存在(少于缓存中的key)
//        keyList.remove(1);
//        list1 = cache.batchGetObject(keyList);
//        System.out.println("batch get 3" + list1);
//
//        // key 部分存在缓存，部分不存在缓存
//        keyList.add("other");
//        list1 = cache.batchGetObject(keyList);
//        System.out.println("batch get 4" + list1);
    }
}
