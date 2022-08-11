package com.jd.platform.hotkey.dashboard.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
public class CaffeineBuilder {

    public static Cache<String, Object> cache(int duration) {
        return cache(128, 1000000, duration);
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
