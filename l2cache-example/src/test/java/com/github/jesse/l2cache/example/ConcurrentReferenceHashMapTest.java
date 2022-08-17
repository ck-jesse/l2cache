package com.github.jesse.l2cache.example;

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

/**
 * @author chenck
 * @date 2020/10/5 19:30
 */
public class ConcurrentReferenceHashMapTest {

    /**
     * -Xmx5M -Xms5M -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps
     * 结论：频繁的GC
     */
    public static void main(String[] args) {
        Map<String, String> valueLoaderCache = new ConcurrentReferenceHashMap<>();
        for (int i = 0; i < 10000000; i++) {
            if (i % 10000 == 0) {
                System.out.println(valueLoaderCache.size());
            }
            valueLoaderCache.put("key" + i, "valuevvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv" + i);
        }
        System.out.println(valueLoaderCache.size());
    }
}
