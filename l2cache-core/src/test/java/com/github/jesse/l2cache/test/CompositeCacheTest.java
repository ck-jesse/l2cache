package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.builder.CompositeCacheBuilder;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.cache.expire.DefaultCacheExpiredListener;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.content.NullValue;
import com.github.jesse.l2cache.sync.RedisCacheSyncPolicy;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RMap;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        Set<String> l1ManualKeySet = new HashSet<>();
        l1ManualKeySet.add("actLimitMarkupCache:11_2");
        l1ManualKeySet.add("compositeCache:key0");
        l1ManualKeySet.add("compositeCache:key1");
        l1ManualKeySet.add("compositeCache:key2");
        l1ManualKeySet.add("compositeCache:key3");
        l1ManualKeySet.add("compositeCache:name00");
        l1ManualKeySet.add("compositeCache:1");
        l1ManualKeySet.add("compositeCache:4");

        Set<String> L1ManualCacheNameSet = new HashSet<>();
        L1ManualCacheNameSet.add("goodsSpecCache");


        // 组合缓存 CAFFEINE + REDIS 测试
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .setAllowNullValues(true)
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.REDIS.name())
                .setL1AllOpen(true)
                .setL1Manual(true)
                .setL1ManualKeySet(l1ManualKeySet)
                .setL1ManualCacheNameSet(L1ManualCacheNameSet);
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,refreshAfterWrite=10m,recordStats")
                .setAutoRefreshExpireCache(true);
        cacheConfig.getRedis()
                .setExpireTime(5000000)
//                .setMaxIdleTime(5000)
//                .setMaxSize(200)// 注意如果与caffeine中最大数量大小不一致，容易造成歧义，所以
                .setRedissonYamlConfig("redisson.yaml");

        CacheBuilder builder = new CompositeCacheBuilder()
                .setCacheConfig(cacheConfig)
                .setExpiredListener(new DefaultCacheExpiredListener())
                .setCacheSyncPolicy(new RedisCacheSyncPolicy());
        cache = (CompositeCache) builder.build("compositeCache");

        callable = new Callable<String>() {
            AtomicInteger count = new AtomicInteger(1);

            @Override
            public String call() throws Exception {
               /* String result = "loader_value" + count.getAndAdd(1);
                System.out.println("loader value from valueLoader, return " + result);
                return result;*/
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
        while (true) {
            Thread.sleep(1000);
        }
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
        String key = "ck123";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));

        /*// 3 get and load from Callable
        String key = "key_loader";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println(String.format("get key=%s, value=%s", key, cache.get(key)));
        while (true) {
            Thread.sleep(2000);
            System.out.println(String.format("get key=%s, value=%s", key, cache.get(key, callable)));
        }*/
    }

    @Test
    public void refreshTest() throws InterruptedException {
        String key = "ck123";
        String value = cache.get(key, callable);
        System.out.println(String.format("get key=%s, value=%s", key, value));
        System.out.println(String.format("get key=%s, value=%s", key, value));

        Level1Cache level1Cache = cache.getLevel1Cache();
        level1Cache.refresh(key);
        while (true){

        }
    }

    @Test
    public void refreshTest1() throws InterruptedException {
        String key = "ck123";
//        Object value = cache.get(key);
//        System.out.println(String.format("get key=%s, value=%s", key, value));
//        System.out.println(String.format("get key=%s, value=%s", key, value));

        Level1Cache level1Cache = cache.getLevel1Cache();
        level1Cache.refresh(key);
        while (true){

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

    @Test
    public void batchGet1() {
        List<Integer> keyList = new ArrayList<>();
        keyList.add(1);
        keyList.add(2);
        keyList.add(3);
        Map<Integer, Object> resultMap = cache.batchGet(keyList);
        System.out.println(resultMap);

        resultMap = cache.batchGetOrLoad(keyList, null);
        System.out.println(resultMap);
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
        Map<Object, Object> list1 = new HashMap<>();
//        list1 = cache.batchGet(keyList);
//        System.out.println(list1);

        // key 完全匹配
//        Map<Object, Object> list2 = cache.batchGet(keyList);
//        System.out.println(list2);

        // key 全部存在(少于缓存中的key)
//        keyList.remove(1);
//        list1 = cache.batchGet(keyList);
//        System.out.println(list1);

        // key 部分存在缓存，部分不存在缓存
        keyList.add("other");
        list1 = cache.batchGet(keyList);
        System.out.println(list1);
    }

    @Test
    public void batchPutBuilderCacheKey() {
        // 模拟数据(业务key为DTO)
        Map<UserDTO, User> map = new HashMap<>();
        for (int i = 0; i < 3; i++) {
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
//        cache.batchPut(map, cacheKeyBuilder);

        // 批量get
        List<UserDTO> keyList = new ArrayList<>(map.keySet());
        Map<UserDTO, User> getMap = cache.batchGet(keyList, cacheKeyBuilder);
        System.out.println(getMap);

        // 转换为
        map.put(new UserDTO("name88", "88"), new User("name88", "addr88"));
        Map<UserDTO, Object> keyMap = map.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> cacheKeyBuilder.apply(entry.getKey())));
        getMap = cache.batchGet(keyMap, true);
        System.out.println(getMap);

        getMap = cache.batchGetOrLoad(keyMap, null, true);
        System.out.println(getMap);

        // 通过参数DTO可以直接获取到对应的数据
        Map<UserDTO, User> finalGetMap = getMap;
        keyList.forEach(userDTO -> {
            System.out.println("key=" + userDTO + ", value=" + finalGetMap.get(userDTO));
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
                    // 模拟从db只获取部分数据，此时未返回的应该缓存NullValue
                    if (i == 2) {
                        newMap.put(new UserDTO(userDTO.getName(), userDTO.getUserId()), null);
                        break;
                    }
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

        mapNew = cache.batchGetOrLoad(keyList, valueLoader);
        System.out.println(mapNew);

//        // 模拟增加一个不存在的key
//        keyList.add(new UserDTO("name60", "60"));
//        keyList.add(new UserDTO("name70", "70"));
//        mapNew = cache.batchGetOrLoad(keyList, valueLoader2);
//        System.out.println(mapNew);
//
//        // 模拟获取一个不存在的key
//        mapNew = cache.batchGetOrLoad(keyList, valueLoader2);
//        System.out.println(mapNew);
    }


    @Test
    public void batchGetOrLoad1() {
        Map<Object, String> dbQueryMap = new HashMap<>();
        dbQueryMap.put(1, "1");
        dbQueryMap.put(2, "2");
        dbQueryMap.put(3, "3");

        List<Integer> keyList = new ArrayList<>();
        keyList.add(1);
        keyList.add(2);
        keyList.add(3);
        keyList.add(4);

        cache.batchPut(dbQueryMap);

        Function cacheKeyBuilder = new Function<Integer, Object>() {
            @Override
            public Object apply(Integer integer) {
                return integer;
            }
        };

        Function valueLoader = new Function<List<Integer>, Map<Integer, String>>() {
            @Override
            public Map<Integer, String> apply(List<Integer> integers) {
                Map<Integer, String> resultMap = new HashMap<>();
                for (Integer integer : integers) {
                    String s = dbQueryMap.get(integer);
                    if (s != null) {
                        resultMap.put(integer, s);
                    }
                }
                return resultMap;
            }
        };
        Map<Integer, Object> resultMap = cache.batchGetOrLoad(keyList, cacheKeyBuilder, valueLoader, true);

        resultMap = cache.batchGetOrLoad(keyList, cacheKeyBuilder, valueLoader);

        System.out.println(resultMap);
    }

}
