package com.coy.l2cache.test;

import com.coy.l2cache.cache.CaffeineCache;
import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.schedule.NullValueCacheClearTask;
import com.coy.l2cache.schedule.NullValueClearSupport;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;
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


    @Test
    public void getIfPresentTest() throws InterruptedException {
        String cacheName = "getIfPresentTestCache";
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
//                .refreshAfterWrite(5, TimeUnit.SECONDS)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(5)
                .removalListener((key, value, cause) -> {
                    System.out.println("[getIfPresentTestCache] remove removalCause={}, cacheName=" + cacheName + ", key=" + key + ", value=" + value);
                })
                .build(key -> {
                    System.out.println("load value = 0");
                    return 0;
                });

        Integer key1 = 1;
        cache.put(key1, 1);

        System.out.println("[缓存未过期] get key = " + key1 + ", value =" + cache.get(key1));
        Thread.sleep(5000);

        // refreshAfterWrite模式下：获取已过期的缓存，这种情况下会触发load
        // expireAfterWrite模式下：获取已过期的缓存，这种情况下不会触发load
        System.out.println("[缓存已过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // refreshAfterWrite模式下：获取未过期的缓存，这种情况下不会触发load
        // expireAfterWrite模式下：获取未过期的缓存，这种情况下不会触发load
        System.out.println("[缓存未过期] getIfPresent key = " + key1 + ", value =" + cache.getIfPresent(key1));

        // refreshAfterWrite模式下：获取不存在的缓存，这种情况下不会触发load
        // expireAfterWrite模式下：获取不存在的缓存，这种情况下不会触发load
        Integer key2 = 2;
        System.out.println("[缓存不存在] getIfPresent key = " + key2 + ", value =" + cache.getIfPresent(key2));
        System.out.println();
    }
}
