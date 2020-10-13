package com.coy.l2cache.example;

import com.coy.l2cache.util.DateUtils;
import com.coy.l2cache.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/10/5 19:29
 */
public class CaffeineCacheTest1 {

    /**
     * refreshAfterWrite 配合 cleanUp 来清理过期缓存
     * <p>
     * 结论：不能清理过期缓存
     */
    @Test
    public void refreshAfterWriteTest() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .refreshAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000)
                .softValues()
                .build(key -> {
                    return RandomUtil.getUUID();
                });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(100);
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        System.out.println("size=" + valueLoaderCache.estimatedSize());

        while (valueLoaderCache.estimatedSize() > 0) {
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            valueLoaderCache.cleanUp();
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            Thread.sleep(1000);
        }
    }

    /**
     * refreshAfterWrite 配合 cleanUp 来清理过期缓存
     * <p>
     * 结论：不能清理过期缓存
     */
    @Test
    public void refreshAfterWrite1() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .refreshAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(10000)
                .softValues()
                .build(key -> {
                    Thread.sleep(3000);
                    return RandomUtil.getUUID();
                });

        System.out.println(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_23) + " size=" + valueLoaderCache.estimatedSize());
        for (int i = 0; i < 100000; i++) {
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        System.out.println(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_23) + " size=" + valueLoaderCache.estimatedSize());
        System.out.println();

        int index = 1;
        while (valueLoaderCache.asMap().size() > 0) {
            System.out.println(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_23) + " size=" + valueLoaderCache.asMap().size() + " index=" + index);
            for (String key : valueLoaderCache.asMap().keySet()) {
                valueLoaderCache.get(key);
            }
            System.out.println(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_23) + " size=" + valueLoaderCache.asMap().size() + " index=" + index);
            Thread.sleep(2000);
            index++;
        }
    }

    /**
     * expireAfterWrite 配合 cleanUp 来清理过期缓存
     * <p>
     * 结论：可以清理过期缓存
     */
    @Test
    public void expireAfterWrite() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(1000)
                .softValues()
                .build(key -> null);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(50);
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        System.out.println("size=" + valueLoaderCache.estimatedSize());

        while (valueLoaderCache.estimatedSize() > 0) {
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            valueLoaderCache.cleanUp();
            System.out.println("size=" + valueLoaderCache.estimatedSize());
            Thread.sleep(1000);
        }
    }
}
