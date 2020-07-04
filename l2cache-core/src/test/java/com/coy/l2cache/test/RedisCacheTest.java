package com.coy.l2cache.test;

import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.NullValue;
import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.cache.builder.RedisCacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * RedissonCache 中各个方法的单元测试
 */
public class RedisCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    RedissonCache cache;

    @Before
    public void before() {
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .setAllowNullValues(true)
                .getRedis()
                .setExpireTime(2000)
                .setMaxIdleTime(2000)
                .setMaxSize(20)
                .setRedissonYamlConfig("redisson.yaml");

        // 构建cache
        cache = (RedissonCache) new RedisCacheBuilder()
                .setCacheConfig(cacheConfig)
                .build("redisCache");

        System.out.println("cacheName: " + cache.getCacheName());
        System.out.println("level: " + cache.getCacheName());
        System.out.println("actualCache: " + cache.getActualCache().getClass().getName());
        System.out.println();
    }

    // 因为get()可能会触发load操作，所以打印数据时使用该方法
    private void printAllCache() {
        Map map1 = cache.getActualCache().readAllMap();
        map1.forEach((o1, o2) -> {
            System.out.println(String.format("key=%s, value=%s", o1, o2));
        });
        System.out.println();
    }

    private void printCache(Object key) {
        Object value = cache.get(key);
        System.out.println(String.format("key=%s, value=%s", key, value));
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
        String key = "key";
        String value = "value";

        // 1 put and get
        cache.put(key, value);
        printCache(key);

        value = cache.get(key, String.class);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println();

        // 2 put and get(key, type)
        String key1 = "key1";
        cache.put(key1, NullValue.INSTANCE);
        printCache(key1);

        NullValue value1 = cache.get(key1, NullValue.class);
        System.out.println(String.format("get key1=%s, value1=%s", key, value1));
        System.out.println();
    }

    @Test
    public void getAndLoadTest() throws InterruptedException {
        // 3 get and load from Callable
        String key = "key_loader";
        String value = cache.get(key, () -> {
            String result = "loader_value";
            System.out.println("loader value from valueLoader, return " + result);
            return result;
        });
        System.out.println(String.format("get key=%s, value=%s", key, value));
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
        System.out.println(String.format("get key=%s, value=%s", key, cache.get(key, String.class)));
        System.out.println();

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

}
