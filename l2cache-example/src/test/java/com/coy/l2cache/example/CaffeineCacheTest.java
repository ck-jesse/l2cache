package com.coy.l2cache.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * @author chenck
 * @date 2020/10/5 19:29
 */
public class CaffeineCacheTest {
    /**
     * -Xmx5M -Xms5M -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps
     */
    public static void main(String[] args) {
        Cache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .maximumSize(1000)
                .build();
        // Caffeine 的淘汰机制是异步的，所以基于大小的淘汰可能没有那么及时。
        for (int i = 0; i < 10000000; i++) {
            if (i % 10000 == 0) {
                System.out.println(valueLoaderCache.estimatedSize());
            }
            valueLoaderCache.put("key" + i, "valuevvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv" + i);
        }
        System.out.println(valueLoaderCache.estimatedSize());
    }
}
