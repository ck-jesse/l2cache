package com.coy.l2cache.test;

import com.coy.l2cache.builder.CacheBuilder;
import com.coy.l2cache.spi.ServiceLoader;

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
    }
}
