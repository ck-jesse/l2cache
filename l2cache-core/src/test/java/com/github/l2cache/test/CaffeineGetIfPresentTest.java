package com.github.l2cache.test;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2021/5/8 18:28
 */
public class CaffeineGetIfPresentTest {


    /**
     * 验证：refreshAfterWrite模式下，getIfPresent() 是否会触发数据加载
     * 结论：
     * refreshAfterWrite模式下：获取存在但已过期的缓存会触发load【此情况需特别注意，当valueLoader为null时】
     * refreshAfterWrite模式下：获取存在但未过期的缓存不会触发load
     * refreshAfterWrite模式下：获取不存在的缓存不会触发load
     */
    @Test
    public void getIfPresentTest1() throws InterruptedException {
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
                .refreshAfterWrite(3, TimeUnit.SECONDS)// 重点
                .maximumSize(5)
                .removalListener((key, value, cause) -> {
                    System.out.println("remove removalCause={}, key=" + key + ", value=" + value);
                })
                .build(key -> {
                    System.out.println("load value = 0");
                    return 0;
                });

        valid(cache);
    }

    /**
     * 验证：expireAfterWrite模式下，getIfPresent() 是否会触发数据加载
     * 结论：
     * expireAfterWrite模式下：获取存在但已过期/存在但未过期/不存在的缓存，均不会触发load
     */
    @Test
    public void getIfPresentTest2() throws InterruptedException {
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)// 重点
                .maximumSize(5)
                .removalListener((key, value, cause) -> {
                    System.out.println("remove removalCause={}, key=" + key + ", value=" + value);
                })
                .build(key -> {
                    System.out.println("load value = 0");
                    return 0;
                });

        valid(cache);
    }

    private void valid(LoadingCache<Integer, Integer> cache) throws InterruptedException {

        Integer key1 = 1;
        cache.put(key1, 1);

        System.out.println("[缓存未过期] get key = " + key1 + ", value =" + cache.get(key1));
        Thread.sleep(4000);// 休眠4s，让缓存过期

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
