package com.coy.l2cache.manager;

import com.coy.l2cache.context.ExtendCacheManager;
import com.coy.l2cache.util.ServiceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存管理，提供缓存管理API便于管理缓存。
 * 注：该缓存管理类本不应该放到此处，但因为是通用功能暂时放在此处。
 * <p>
 *
 * @author chenck
 * @date 2020/5/26 11:43
 */
@Slf4j
@RestController
@RequestMapping(value = "/l2cache/manager")
public class CacheManagerController {

    @Autowired
    ExtendCacheManager extendCacheManager;

    /**
     * 获取缓存名字列表
     */
    @RequestMapping(value = "/getCacheNames")
    public ServiceResult getCacheNames() {
        return ServiceResult.succ(extendCacheManager.getCacheNames());
    }

    /**
     * 获取缓存
     */
    @RequestMapping(value = "/get")
    public ServiceResult get(String cacheName, String key) {
        Cache cache = extendCacheManager.getCache(cacheName);
        if (null == cache) {
            return ServiceResult.error("缓存实例不存在，cacheName=" + cacheName);
        }
        return ServiceResult.succ(cache.get(key));
    }

    /**
     * 重新加载缓存
     * 异步加载{@code key}的新值，如果新值加载成功，则替换缓存中的前一个值，并且会发送refresh消息，其他节点接收到refresh消息，刷新缓存
     */
    @RequestMapping(value = "/refresh")
    public ServiceResult refresh(String cacheName, String key) {
        extendCacheManager.refresh(cacheName, key);
        return ServiceResult.succ();
    }

    /**
     * 清理缓存
     * 注：先删除redis，再发送clear消息，然后再删除本地缓存；其他节点接收到clear消息，调用ExtendCacheManager#clearLocalCache()清理本地缓存。
     */
    @RequestMapping(value = "/clear")
    public ServiceResult clear(String cacheName, String key) {
        extendCacheManager.clear(cacheName, key);
        return ServiceResult.succ();
    }
}
