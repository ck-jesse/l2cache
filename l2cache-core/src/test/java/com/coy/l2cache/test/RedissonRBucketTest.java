package com.coy.l2cache.test;

import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * 模拟大量
 *
 * @author chenck
 * @date 2020/10/3 19:34
 */
public class RedissonRBucketTest {

    RedissonClient redissonClient;

    @Before
    public void before() {
        redissonClient = Redisson.create();
    }

    @Test
    public void putTest() {
        String prefix = "bucket_test";
        for (int i = 1; i < 100000; i++) {
            String key = prefix + i;
            System.out.println("put " + key);
            RBucket<Object> bucket = redissonClient.getBucket(key);
            bucket.set(key, 5, TimeUnit.MINUTES);
        }
    }

    @Test
    public void getTest() throws InterruptedException {
        String key = "bucket_test99999";
        RBucket<Object> bucket = redissonClient.getBucket(key);
        while (true) {
            if (bucket.isExists()) {
                Object value = bucket.get();
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
