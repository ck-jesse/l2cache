package com.coy.l2cache.test;

import com.coy.l2cache.CacheBuilder;
import com.coy.l2cache.HotKeyService;
import com.coy.l2cache.spi.ServiceLoader;

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

        HotKeyService hotKeyService = ServiceLoader.load(HotKeyService.class, "jd");
        boolean isHotKey = hotKeyService.ifHotKey(1, cacheKeyBuilder);
        System.out.println(isHotKey);
    }
}
