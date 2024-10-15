package com.github.jesse.l2cache.example.test;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author chenck
 * @date 2020/10/5 19:29
 */
public class CaffeineCacheTest {
    /**
     * -Xmx10M -Xms10M -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps
     * <p>
     * Caffeine 基于大小的淘汰机制是异步线程池的方式来执行的清理任务。
     * 所以在put大量不同key的情况下，清理任务可能出现堆积，也就是说极端情况下会出现缓存项未被及时清理掉，而占用大量内存的情况出现。
     * 导致频繁的gc，甚至最终出现OOM。
     */
    public static void main(String[] args) {
        Cache<String, L2CacheConfig> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .maximumSize(1000)
                .build();

        L2CacheConfig cacheConfig = new L2CacheConfig();
//        cacheConfig.setInstanceId("123123");
        valueLoaderCache.put("test", cacheConfig);

//        cacheConfig.setInstanceId("sdfdsfsdsdf");
        System.out.println();

        Short isLimitMarkup = 1;
        System.out.println(isLimitMarkup == 1);
    }
}
