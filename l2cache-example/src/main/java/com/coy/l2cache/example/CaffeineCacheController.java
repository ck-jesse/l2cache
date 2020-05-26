package com.coy.l2cache.example;

import com.coy.l2cache.consts.CacheConsts;
import com.coy.l2cache.context.ExtendCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    ExtendCacheManager extendCacheManager;

    @RequestMapping(value = "/queryUser")
    public User queryUser(String userId) {
        return caffeineCacheService.queryUser(userId);
    }

    @RequestMapping(value = "/queryUserSync")
    public List<User> queryUserSync(String userId) {
        return caffeineCacheService.queryUserSync(userId);
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
        if (CacheConsts.CACHE_REFRESH.equals(optType)) {
            extendCacheManager.refresh(cacheName, key);
        } else {
            extendCacheManager.clear(cacheName, key);
        }
        return "success";
    }
}
