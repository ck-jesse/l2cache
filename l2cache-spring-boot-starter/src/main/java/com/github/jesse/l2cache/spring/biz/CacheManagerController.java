package com.github.jesse.l2cache.spring.biz;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.exception.L2CacheException;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import com.github.jesse.l2cache.util.ServiceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * * 缓存管理，提供缓存管理API便于管理缓存。
 * * <p>
 * * TODO 对于缓存管理，需要根据缓存名字找到对应的业务加载方法，这样才可以进行缓存管理，后续再实现通用的方案
 *
 * @author chenck
 * @date 2021/8/13 17:33
 */
@Slf4j
@RestController
@RequestMapping(value = "/l2cache/manager")
public class CacheManagerController {

    @Autowired
    L2CacheCacheManager l2CacheCacheManager;

    /**
     * 根据缓存维度获取缓存信息
     *
     * @param cacheName
     * @return
     */
    public Cache getCache(String cacheName) {
        Cache cache = (Cache) l2CacheCacheManager.getCache(cacheName).getNativeCache();
        if (null != cache) {
            return cache;
        }
        throw new L2CacheException("未找到Cache对象，请检查缓存配置是否正确");
    }

    /**
     * 获取缓存名字列表
     */
    @RequestMapping(value = "/getCacheNames")
    public ServiceResult getCacheNames() {
        return ServiceResult.succ(l2CacheCacheManager.getCacheNames());
    }

    /**
     * 获取缓存配置
     */
    @RequestMapping(value = "/getCacheConfig")
    public ServiceResult getCacheConfig(String cacheName) {
        L2CacheConfig.CacheConfig cacheConfig = L2CacheConfigUtil.getCacheConfig(l2CacheCacheManager.getL2CacheConfig(), cacheName);
        return ServiceResult.succ(cacheConfig);
    }

    /**
     * 获取缓存
     */
    @RequestMapping(value = "/get")
    public ServiceResult get(String cacheName, String key) {
        return ServiceResult.succ(this.getCache(cacheName).get(key));
    }


    /**
     * 清理缓存
     * 注：先删除redis，然后再删除本地缓存
     */
    @RequestMapping(value = "/evict")
    public ServiceResult evict(String cacheName, String key) {
        this.getCache(cacheName).evict(key);
        return ServiceResult.succ();
    }

}
