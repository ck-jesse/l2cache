package com.coy.l2cache.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/10/5 19:29
 */
public class CaffeineCacheTest1 {
    @Test
    public void test1() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(1000)
                .softValues()
                .build(key -> null);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        System.out.println("size=" + valueLoaderCache.estimatedSize());

        while (valueLoaderCache.estimatedSize() > 0) {
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            valueLoaderCache.cleanUp();
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            Thread.sleep(100);
        }
    }

    /**
     * expireAfterWrite 配合 cleanUp 来清理过期缓存
     */
    @Test
    public void test2() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(1000)
                .softValues()
                .build(key -> null);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        System.out.println("size=" + valueLoaderCache.estimatedSize());

        while (valueLoaderCache.estimatedSize() > 0) {
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            valueLoaderCache.cleanUp();
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            Thread.sleep(100);
        }
    }
}
