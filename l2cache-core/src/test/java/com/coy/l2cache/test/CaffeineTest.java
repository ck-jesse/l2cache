package com.coy.l2cache.test;

import com.coy.l2cache.content.CustomCaffeineSpec;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenck
 * @date 2022/2/12 15:40
 */
public class CaffeineTest {

    private static Logger LOGGER = LoggerFactory.getLogger(CaffeineTest.class);

    private AtomicInteger waitRefreshNum = new AtomicInteger();

    @Test
    public void test() throws InterruptedException {

        CustomCaffeineSpec caffeineSpec = CustomCaffeineSpec.parse("initialCapacity=10,maximumSize=200,refreshAfterWrite=1s,recordStats");

        LoadingCache<String, Integer> cache = caffeineSpec.toBuilder().build(key -> {
            Integer num = waitRefreshNum.getAndIncrement();
            LOGGER.info("key={}, value={}", key, num);
            Thread.sleep(10000);
            LOGGER.info("key={}, value={} end", key, num);
            return num;
        });

        String key1 = "key1";
        String key2 = "key2";
//        cache.put(key1, 100);
//        cache.put(key2, 200);
//        Thread.sleep(2000);
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                LOGGER.info("获取：key={}", key1);
                LOGGER.info("获取：key={}, value={}", key1, cache.get(key1));
            }, "Thead" + i).start();
        }

        LOGGER.info("获取：key={}", key1);
        Integer value1 = cache.get(key1);
        LOGGER.info("获取：key={}, value={}", key1, value1);

        cache.put(key1, 10000);
        LOGGER.info("再次设置：key={}, value={}", key1, 10000);

        value1 = cache.get(key1);
        LOGGER.info("再次获取：key={}, value={}", key1, value1);

        LOGGER.info("获取：key={}", key2);
        Integer value2 = cache.get(key2);
        LOGGER.info("获取：key={}, value={}", key2, value2);

        System.out.println();
        while (true) {
            value1 = cache.get(key1);
            LOGGER.info("获取：key={}, value={}", key1, value1);
            Thread.sleep(2000);
        }
    }
}
