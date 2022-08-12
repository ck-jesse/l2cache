package com.github.l2cache.test;

import com.github.l2cache.cache.RedissonRBucketCache;
import com.github.l2cache.builder.RedisCacheBuilder;
import com.github.l2cache.CacheConfig;
import com.github.l2cache.consts.CacheType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * RedissonCache 中各个方法的单元测试
 */
@Slf4j
public class RedisCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    RedissonRBucketCache cache;
    Callable<String> callable;
    RedissonClient redissonClient;

    @Before
    public void before() {
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .setAllowNullValues(true)
                .getRedis()
                .setExpireTime(3000000)
                .setLock(true)
//                .setMaxIdleTime(2000)
//                .setMaxSize(20)
                .setDuplicate(false)
                .setRedissonYamlConfig("redisson.yaml");

        // 模拟应用中已经存在 RedissonClient
        redissonClient = Redisson.create(cacheConfig.getRedis().getRedissonConfig());

        RedisCacheBuilder builder = (RedisCacheBuilder) new RedisCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setActualCacheClient(redissonClient);

        // 构建cache
        cache = builder.build("redisCache");
//        cache = builder.build("redisCache2");

        cacheConfig.getRedis().getDuplicateCacheNameMap().put(cache.getCacheName(), 3);


        cacheConfig.getRedis().getDuplicateKeyMap().put("redisCache:key1", 5);

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
//        System.out.println("L2 所有的缓存值");
//        Map map1 = cache.getActualCache().readAllMap();
//        map1.forEach((o1, o2) -> {
//            System.out.println(String.format("key=%s, value=%s", o1, o2));
//        });
//        System.out.println();
    }

    //
    private void printCache(Object key) {
        Object value = cache.get(key);
        System.out.println(String.format("L2 缓存值 key=%s, value=%s", key, value));
        System.out.println();
    }

    //
//    @Test
//    public void test() {
//        System.out.println(TimeUnit.MILLISECONDS.toMillis(5000));
//    }
//
//    @Test
//    public void putNullTest() throws InterruptedException {
//        String key = "key_null";
//        cache.put(key, null);
//        printCache(key);
//        System.out.println(cache.get(key));
//    }
//
    @Test
    public void putUserTest() throws InterruptedException {

        String key = "user_key";
        System.out.println(cache.get(key));

//        String key = "user_key";
//        User user = new User();
//        user.setName("test");
//        user.setAddr(key);
//        user.setCurrTime(System.currentTimeMillis());
//        cache.put(key, user);
//        printCache(key);
//        System.out.println(cache.get(key));
    }

    @Test
    public void putAndGetTest() throws InterruptedException {
        String key = "key";
        String value = "valueaaaaaa";

        // 1 put and get
        cache.put(key, value);
//        printCache(key);

        value = cache.get(key, String.class);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println();

        // 2 put and get(key, type)
//        String key1 = "key111";
//        cache.put(key1, "NullValue.INSTANCEaaaaaa");
//        printCache(key1);
//
////        NullValue value1 = cache.get(key1, NullValue.class);
//        String value1 = cache.get(key1, String.class);
//        System.out.println(String.format("get key1=%s, value1=%s", key1, value1));
//        System.out.println();
    }

    @Test
    public void getAndLoadTest() throws InterruptedException {
        // 3 get and load from Callable
        String key = "key_loader123";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        value = cache.get(key, callable);
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
        Map<Object, Object> list1 = cache.batchGet(keyList);
        System.out.println("batch get 1" + list1);

        // key 完全匹配
        Map<Object, Object> list2 = cache.batchGet(keyList);
        System.out.println("batch get 2" + list2);

        // key 全部存在(少于缓存中的key)
        keyList.remove(1);
        list1 = cache.batchGet(keyList);
        System.out.println("batch get 3" + list1);

        // key 部分存在缓存，部分不存在缓存
        keyList.clear();
        keyList.add("other");
        list1 = cache.batchGet(keyList);
        System.out.println("batch get 4" + list1);
    }

    @Test
    public void batchPutBuilderCacheKey() {
        // 模拟数据(业务key为DTO)
        Map<UserDTO, User> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put(new UserDTO("name" + i, "" + i), new User("name" + i, "addr" + i));
        }
        System.out.println(map);

        // 自定义cacheKey的构建方式
        Function<UserDTO, Object> cacheKeyBuilder = new Function<UserDTO, Object>() {
            @Override
            public Object apply(UserDTO userDTO) {
                return userDTO.getName() + userDTO.getUserId();
            }
        };
        // 批量put
        cache.batchPut(map, cacheKeyBuilder);

        // 批量get
        List<UserDTO> keyList = new ArrayList<>(map.keySet());
        Map<UserDTO, User> getMap = cache.batchGet(keyList, cacheKeyBuilder);
        System.out.println(getMap);

        // 通过参数DTO可以直接获取到对应的数据
        keyList.forEach(userDTO -> {
            System.out.println("key=" + userDTO + ", value=" + getMap.get(userDTO));
        });
    }

    @Test
    public void batchGetOrLoad() {
        // 模拟数据(业务key为DTO)
        Map<UserDTO, User> map = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            map.put(new UserDTO("name" + i, "" + i), new User("name" + i, "addr" + i));
        }
        System.out.println(map);
        List<UserDTO> keyList = new ArrayList<>(map.keySet());

        Function<List<UserDTO>, Map<UserDTO, User>> valueLoader = new Function<List<UserDTO>, Map<UserDTO, User>>() {
            @Override
            public Map<UserDTO, User> apply(List<UserDTO> userDTOS) {
                Map<UserDTO, User> newMap = new HashMap<>();
                int i = 0;
                for (UserDTO userDTO : userDTOS) {
                    newMap.put(new UserDTO(userDTO.getName(), userDTO.getUserId()), new User("new_name" + i, "addr" + i));
                    i++;
                }
                return newMap;
            }
        };

        Function<List<UserDTO>, Map<UserDTO, User>> valueLoader2 = new Function<List<UserDTO>, Map<UserDTO, User>>() {
            @Override
            public Map<UserDTO, User> apply(List<UserDTO> userDTOS) {
                // 模拟从DB获取数据，部分获取到，部分没有获取到
                Map<UserDTO, User> newMap = new HashMap<>();
                int i = 0;
                for (UserDTO userDTO : userDTOS) {
                    newMap.put(new UserDTO(userDTO.getName(), userDTO.getUserId()), new User("new_name" + i, "addr" + i));
                    i++;
                    break;
                }
                return newMap;

                // 模拟从DB获取数据，一个都没有命中的场景
                // return null;
            }
        };
        Map<UserDTO, User> mapNew = cache.batchGetOrLoad(keyList, valueLoader);
        System.out.println(mapNew);

        // 模拟增加一个不存在的key
        keyList.add(new UserDTO("name60", "60"));
        keyList.add(new UserDTO("name70", "70"));
        mapNew = cache.batchGetOrLoad(keyList, valueLoader2);
        System.out.println(mapNew);

        // 模拟获取一个不存在的key
        mapNew = cache.batchGetOrLoad(keyList, valueLoader2);
        System.out.println(mapNew);
    }

}
