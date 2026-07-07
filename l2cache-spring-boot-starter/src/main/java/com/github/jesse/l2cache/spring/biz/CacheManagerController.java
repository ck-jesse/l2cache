package com.github.jesse.l2cache.spring.biz;

import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.L2CacheConfigUtil;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.exception.L2CacheException;
import com.github.jesse.l2cache.metrics.MetricsRecorder;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import com.github.jesse.l2cache.spring.consistency.ConsistencyCheckService;
import com.github.jesse.l2cache.util.PageResult;
import com.github.jesse.l2cache.util.ServiceResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

    @Autowired
    ConsistencyCheckService consistencyCheckService;

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

    /**
     * 分页查询缓存 key 列表
     * <p>
     * simple-by-design: 当前仅支持 CompositeCache 的 L1 缓存 key 列表查询。
     */
    @RequestMapping(value = "/listKeys")
    public PageResult listKeys(String cacheName,
                               @RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "20") int pageSize) {
        Cache cache = this.getCache(cacheName);
        Set<Object> keys = this.getCacheKeys(cache);
        List<String> keyList = new ArrayList<>(keys.size());
        for (Object key : keys) {
            keyList.add(key == null ? "" : key.toString());
        }
        int total = keyList.size();
        int fromIndex = (pageNum - 1) * pageSize;
        if (fromIndex >= total || fromIndex < 0) {
            return PageResult.succ(Collections.emptyList(), (long) pageNum, (long) pageSize, (long) total);
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        return PageResult.succ(keyList.subList(fromIndex, toIndex), (long) pageNum, (long) pageSize, (long) total);
    }

    /**
     * 清空指定缓存
     */
    @RequestMapping(value = "/clear", method = {RequestMethod.GET, RequestMethod.POST})
    public ServiceResult clear(String cacheName) {
        this.getCache(cacheName).clear();
        return ServiceResult.succ();
    }

    /**
     * 热 key 排行榜
     */
    @RequestMapping(value = "/hotKeyRanking")
    public ServiceResult hotKeyRanking(String cacheName, @RequestParam(defaultValue = "10") int topN) {
        MetricsRecorder recorder = this.getMetricsRecorder();
        if (recorder == null) {
            return ServiceResult.succ(Collections.emptyList());
        }
        return ServiceResult.succ(recorder.getHotKeyRanking(cacheName, topN));
    }

    /**
     * 大 key 排行榜
     */
    @RequestMapping(value = "/bigKeyRanking")
    public ServiceResult bigKeyRanking(String cacheName, @RequestParam(defaultValue = "10") int topN) {
        MetricsRecorder recorder = this.getMetricsRecorder();
        if (recorder == null) {
            return ServiceResult.succ(Collections.emptyList());
        }
        return ServiceResult.succ(recorder.getBigKeyRanking(cacheName, topN));
    }

    /**
     * 获取 MetricsRecorder
     */
    private MetricsRecorder getMetricsRecorder() {
        return l2CacheCacheManager.getMetricsRecorder();
    }

    /**
     * 一致性检测
     *
     * @param cacheName  缓存名称
     * @param mode       检测模式：count / value
     * @param keys       value 模式下指定对比的 key（逗号分隔）
     * @param sampleSize value 模式下未指定 key 时的采样大小，默认 100
     * @return 一致性检测结果
     */
    @RequestMapping(value = "/checkConsistency")
    public ServiceResult checkConsistency(String cacheName,
                                          @RequestParam(defaultValue = "count") String mode,
                                          @RequestParam(required = false) String keys,
                                          @RequestParam(defaultValue = "100") int sampleSize) {
        return ServiceResult.succ(consistencyCheckService.check(cacheName, mode, keys, sampleSize));
    }

    /**
     * 获取缓存 key 集合
     */
    private Set<Object> getCacheKeys(Cache cache) {
        if (cache instanceof CompositeCache) {
            Level1Cache l1 = ((CompositeCache) cache).getLevel1Cache();
            if (l1 != null) {
                Set<Object> keys = l1.keys();
                return keys == null ? Collections.emptySet() : keys;
            }
        }
        return Collections.emptySet();
    }

}
