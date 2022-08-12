package com.github.l2cache.test;

import com.github.l2cache.CacheBuilder;
import com.github.l2cache.HotKey;
import com.github.l2cache.spi.ServiceLoader;

import java.util.function.Function;

/**
 * @author chenck
 * @date 2020/7/2 18:17
 */
public class ServiceLoaderTest {

    public static void main(String[] args) {
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, "REDIS");
        System.out.println(cacheBuilder.getClass().getName());

        cacheBuilder = ServiceLoader.load(CacheBuilder.class, "caffeine");
        System.out.println(cacheBuilder.getClass().getName());

        // 自定义cacheKey的构建方式
        Function<Integer, Object> cacheKeyBuilder = info -> {
            StringBuilder builder = new StringBuilder();
            builder.append("goodsCache");
            builder.append(":");
            builder.append(info);
            return builder.toString();
        };

        HotKey hotKey = ServiceLoader.load(HotKey.class, "jd");
        boolean isHotKey = hotKey.ifHotKey(1, cacheKeyBuilder);
        System.out.println(isHotKey);
    }
}
