package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.schedule.NullValueCacheClearTask;
import com.github.jesse.l2cache.schedule.NullValueClearSupport;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/12/4 0:25
 */
public class Test3 {

    private static final Logger logger = LoggerFactory.getLogger(Test3.class);

    public static void main(String[] args) throws InterruptedException {

        String cacheName = "actDiscountCache";
        Cache<Object, Integer> nullValueCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(2)
                .removalListener((key, value, cause) -> {
                    logger.info("[NullValueCache] remove NullValue, removalCause={}, cacheName={}, key={}, value={}", cause, cacheName, key, value);
                })
                .build();

        // 定期清理 NullValue
        NullValueClearSupport.getInstance().scheduleWithFixedDelay(new NullValueCacheClearTask(cacheName, nullValueCache), 5,
                5, TimeUnit.SECONDS);


        String key = "";
        for (int i = 0; i < 10; i++) {
            key = "key" + i;
            logger.info("[LoadFunction] NullValueCache put, cacheName={}, key={}, value=1", cacheName, key);
            nullValueCache.put(key, 1);
        }

        while (true) {
            Thread.sleep(300);
        }
    }

}
