package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenck
 * @date 2020/10/10 11:40
 */
@Slf4j
@RestController
public class CompositeTestController {

    @Autowired
    L2CacheCacheManager cacheManager;

    @RequestMapping(value = "/composite/put")
    public String put(String cacheName, String key, String value) throws InterruptedException {
        Cache cache = cacheManager.getCache(cacheName);
        if (null == cache) {
            return "fail cache is not exists: " + cacheName;
        }
        cache.put(key, value);
        return "";
    }

    @RequestMapping(value = "/composite/get")
    public String get(String cacheName, String key) throws InterruptedException {
        Cache cache = cacheManager.getCache(cacheName);
        if (null == cache) {
            return "fail cache is not exists: " + cacheName;
        }
        String value = cache.get(key, String.class);
        System.out.println(value);
        return "";
    }

    @RequestMapping(value = "/composite/evict")
    public String evict(String cacheName, String key, String value) throws InterruptedException {
        Cache cache = cacheManager.getCache(cacheName);
        if (null == cache) {
            return "fail cache is not exists: " + cacheName;
        }
        cache.put(key, value);
        cache.evict(key);
        String value2 = cache.get(key, String.class);
        System.out.println(value2);
        return "";
    }

}
