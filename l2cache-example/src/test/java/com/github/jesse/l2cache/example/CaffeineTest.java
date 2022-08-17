package com.github.jesse.l2cache.example;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.util.RandomUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 * @author chenck
 * @date 2020/9/25 10:28
 */
public class CaffeineTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // caffeine 不设置过期时间的验证
        LoadingCache<String, String> cache = CacheBuilder.newBuilder().initialCapacity(1).maximumSize(100)
                .expireAfterWrite(-1, TimeUnit.SECONDS)
                .removalListener(notification -> {
                    System.out.println("remove " + JSON.toJSONString(notification));
                })
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        return RandomUtil.getUUID();
                    }
                });

        String key = "test";
        int count = 1;
        while (true) {
            Thread.sleep(1000);
            String value = cache.get(key);
            System.out.println(value);
            if (count % 5 == 0) {
                cache.put(key, "setvalue" + count);
            }
            count++;
        }
    }
}
