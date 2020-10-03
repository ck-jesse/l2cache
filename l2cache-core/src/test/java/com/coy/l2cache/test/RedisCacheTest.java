package com.coy.l2cache.test;

import com.coy.l2cache.cache.RedissonRBucketCache;
import com.coy.l2cache.content.NullValue;
import com.coy.l2cache.builder.RedisCacheBuilder;
import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.CacheConfig;
import com.coy.l2cache.consts.CacheType;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RedissonCache 中各个方法的单元测试
 */
public class RedisCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    RedissonRBucketCache cache;
    Callable<String> callable;

    @Before
    public void before() {
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .setAllowNullValues(true)
                .getRedis()
                .setExpireTime(30000)
                .setLock(true)
//                .setMaxIdleTime(2000)
//                .setMaxSize(20)
                .setRedissonYamlConfig("redisson.yaml");

        // 模拟应用中已经存在 RedissonClient
        RedissonClient redissonClient = Redisson.create(cacheConfig.getRedis().getRedissonConfig());

        RedisCacheBuilder builder = (RedisCacheBuilder) new RedisCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setActualCacheClient(redissonClient);

        // 构建cache
        cache = builder.build("redisCache");
//        cache = builder.build("redisCache2");

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

    // 因为get()可能会触发load操作，所以打印数据时使用该方法
    private void printAllCache() {
//        System.out.println("L2 所有的缓存值");
//        Map map1 = cache.getActualCache().readAllMap();
//        map1.forEach((o1, o2) -> {
//            System.out.println(String.format("key=%s, value=%s", o1, o2));
//        });
//        System.out.println();
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
        String key = "user_key";
        User user = new User();
        user.setName("test");
        user.setAddr(key);
        user.setCurrTime(System.currentTimeMillis());
        cache.put(key, user);
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
        String key = "key_loader123";
        String value = cache.get(key, callable);
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
    public void isExistsTest() throws InterruptedException {
        String key = "key1";
        cache.put(key, "value");
        boolean rslt = cache.isExists(key);
        System.out.println(rslt);
    }

}
