package com.github.jesse.l2cache.test;

import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 问题：模拟 Redisson 的 RMapCache(hash结构)存储大量有过期时间的key时，若大量key到了过期时间，会出现如下问题：
 * 1、redis中的key没有被及时删除掉的
 * 2、key过期未被删除的情况下，判断key是否存在时返回true，但是获取的值为null，在容易导致业务维度出现问题。
 * 分析：
 * 因为RMapCache是通过EvictionScheduler来定期清理的，每次最多淘汰300个过期key，任务的启动时间将根据上次实际清理数量自动调整，间隔时间趋于1秒到1小时之间。
 * 所以大量key同时过期的情况下，不能精准控制key的过期，会导致出现缓存使用混乱的情况。
 * 方案：
 * 使用 RBucket(String结构)来替代RMapCache(hash结构)，可以解决上面两个问题。
 * 过期未删除问题，通过redis本身的淘汰机制来控制，若被淘汰掉，则判断key是否存在时返回false，同时解决了第二个问题。
 *
 * @author chenck
 * @date 2020/10/3 19:34
 */
public class RedissonRMapCacheTest {

    RedissonClient redissonClient;
    RMapCache<Object, Object> mapCache;

    @Before
    public void before() {
        redissonClient = Redisson.create();
        mapCache = redissonClient.getMapCache("RMapCache");
    }

    /**
     * 通过 RMapCache 设置大量key到缓存中
     */
    @Test
    public void putTest() {
        String prefix = "map:test";
        for (int i = 1; i < 100000; i++) {
            String key = prefix + i;
            System.out.println("put " + key);
            mapCache.fastPut(key, key, 5, TimeUnit.MINUTES);
        }
    }

    /**
     * 模拟获取某个指定的key
     */
    @Test
    public void getTest() throws InterruptedException {
        String key = "map:test99999";
        while (true) {
            // 判断key是否存在
            if (mapCache.isExists()) {
                // 存在的情况下，获取缓存值
                // 结论：若过期，则获取到的值为null
                Object value = mapCache.get(key);
                if (null == value) {
                    System.out.println("key is exists, value is null, key=" + key);
                } else {
                    System.out.println("key is exists, key=" + key + ", value=" + value);
                }
            } else {
                System.out.println("key is not exists, key=" + key);
            }
            Thread.sleep(500);
        }
    }
}
