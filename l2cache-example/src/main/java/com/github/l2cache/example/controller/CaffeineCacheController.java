package com.github.l2cache.example.controller;

import com.github.l2cache.cache.CompositeCache;
import com.github.l2cache.cache.Level1Cache;
import com.github.l2cache.consts.CacheConsts;
import com.github.l2cache.example.service.CaffeineCacheService;
import com.github.l2cache.example.dto.User;
import com.github.l2cache.spring.L2CacheCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author chenck
 * @date 2020/4/26 19:40
 */
@Slf4j
@RestController
public class CaffeineCacheController {

    @Autowired
    CaffeineCacheService caffeineCacheService;

    @Autowired
    L2CacheCacheManager cacheManager;

    @RequestMapping(value = "/queryUser")
    public User queryUser(String userId) {
        return caffeineCacheService.queryUser(userId);
    }

    @RequestMapping(value = "/queryUserSync")
    public User queryUserSync(String userId) {
        return caffeineCacheService.queryUserSync(userId);
    }

    @RequestMapping(value = "/queryUserSyncList")
    public List<User> queryUserSyncList(String userId) {
        return caffeineCacheService.queryUserSyncList(userId);
    }

    @RequestMapping(value = "/putUser")
    public String putUser(String userId) {
        User user = new User(userId, "addr");
        caffeineCacheService.putUser(userId, user);
        return "success";
    }

    @RequestMapping(value = "/evictUserSync")
    public String evictUserSync(String userId) {
        return caffeineCacheService.evictUserSync(userId);
    }

    /**
     * 清除缓存
     */
    @RequestMapping(value = "/evictCache")
    public String evictCache(String cacheName, String key, String optType) {
        Cache cache = cacheManager.getCache(cacheName);
        if (null == cache) {
            return "fail cache is not exists: " + cacheName;
        }
        if (CacheConsts.CACHE_REFRESH.equals(optType)) {
            cache.evict(key);
        } else {
            cache.clear();
        }
        return "success";
    }

    @RequestMapping(value = "/delValueLoader")
    public String delValueLoader(String userId) {
        com.github.l2cache.Cache l2cache = (com.github.l2cache.Cache) cacheManager.getCache("queryUserSync").getNativeCache();
        if (l2cache instanceof CompositeCache) {
            Level1Cache level1Cache = ((CompositeCache) l2cache).getLevel1Cache();
            level1Cache.getCacheLoader().delValueLoader(userId);
        }
        return "true";
    }

    @RequestMapping(value = "/justput")
    public String justput(String userId) {
        User user = new User(userId, "justput");
        com.github.l2cache.Cache l2cache = (com.github.l2cache.Cache) cacheManager.getCache("queryUserSync").getNativeCache();
        l2cache.put(userId, user);
        return "justput";
    }

    @PostMapping(value = "/batchGetUser")
    public Map<String, User> batchGetUser(@RequestBody List<String> userIdList) {
        return caffeineCacheService.batchGetUser(userIdList);
    }

    @RequestMapping(value = "/batchGetOrLoadUser")
    public Map<String, User> batchGetOrLoadUser(@RequestBody List<String> userIdList) {
        return caffeineCacheService.batchGetOrLoadUser(userIdList);
    }
}
