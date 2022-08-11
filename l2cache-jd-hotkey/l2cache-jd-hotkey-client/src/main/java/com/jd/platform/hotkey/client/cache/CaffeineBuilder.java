package com.jd.platform.hotkey.client.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jd.platform.hotkey.client.Context;

import java.util.concurrent.TimeUnit;

/**
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
public class CaffeineBuilder {

    public static Cache<String, Object> cache(int duration) {
        return cache(128, Context.CAFFEINE_SIZE, duration);
    }

    public static Cache<String, Object> cache() {
        return cache(128, Context.CAFFEINE_SIZE, 60);
    }

    /**
     * 构建所有来的要缓存的key getCache
     */
    public static Cache<String, Object> cache(int minSize, int maxSize, int expireSeconds) {
        return Caffeine.newBuilder()
                .initialCapacity(minSize)//初始大小
                .maximumSize(maxSize)//最大数量
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)//过期时间
                .build();
    }

}
