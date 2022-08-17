package com.github.jesse.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.spring.biz.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
/**
 * CacheService 的demo案例
 * 注：
 * 优化前的demo案例，需要实现的方法较多，且有很多共性的代码，业务开发中，相对不够简洁
 *
 * @author chenck
 * @date 2022/8/4 22:23
 */
@Component
@Slf4j
@Deprecated
public class BrandCacheService extends AbstractCacheService<Integer, BrandRespBO> {

    public static final String CACHE_NAME = "brandCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(Integer key) {
        return String.valueOf(key);
    }

    /**
     * 获取或加载缓存项
     * 注：@Cacheable(sync=true) 当缓存过期时，缓存刷新任务会重新加载缓存项
     *
     * @param brandId 品牌id
     */
    @Cacheable(value = CACHE_NAME, key = "#brandId", sync = true)
    @Override
    public BrandRespBO getOrLoad(Integer brandId) {
        return this.reload(brandId);
    }

    /**
     * 设置缓存项
     * 注：@CachePut 当缓存过期时，缓存刷新任务会淘汰缓存项
     *
     * @param brandId 品牌id
     */
    @CachePut(value = CACHE_NAME, key = "#brandId")
    @Override
    public BrandRespBO put(Integer brandId, BrandRespBO value) {
        return value;
    }

    /**
     * 重载缓存项
     * 注：@CachePut 当缓存过期时，缓存刷新任务会淘汰缓存项
     *
     * @param brandId 品牌id
     */
    @CachePut(value = CACHE_NAME, key = "#brandId")
    @Override
    public BrandRespBO reload(Integer brandId) {
        return this.queryData(brandId);
    }

    /**
     * 删除缓存项
     *
     * @param brandId 品牌id
     */
    @CacheEvict(value = CACHE_NAME, key = "#brandId")
    @Override
    public void evict(Integer brandId) {
        log.info("[evict] cacheName={}, key={}", CACHE_NAME, brandId);
    }

    /**
     * 批量获取或加载缓存项
     *
     * @param keyList 品牌id列表
     */
    @Override
    public Map<Integer, BrandRespBO> batchGetOrLoad(List<Integer> keyList) {
        // 从DB中加载数据
        Function<List<Integer>, Map<Integer, BrandRespBO>> valueLoader = notHitCacheKeyList -> this.queryDataList(notHitCacheKeyList);

        // 批量查询信息
        Cache cache = this.getNativeL2cache();
        return cache.batchGetOrLoad(keyList, valueLoader);
    }

    @Override
    public BrandRespBO queryData(Integer brandId) {
        BrandRespBO brandRespBO = new BrandRespBO();
        brandRespBO.setBrandId(0);
        brandRespBO.setGroupId(0);
        brandRespBO.setBrandName("");
        brandRespBO.setBrandNumber("");
        brandRespBO.setDescription("");
        brandRespBO.setState(0);
        log.info("查询获取品牌相关信息,brandId={},brandInfoRespBO={}", brandId, JSON.toJSONString(brandRespBO));
        return brandRespBO;
    }

    @Override
    public Map<Integer, BrandRespBO> queryDataList(List<Integer> keyList) {
        Map<Integer, BrandRespBO> map = new HashMap<>();
        // 模拟返回数据，业务系统中可直接从DB加载数据
        for (Integer brandId : keyList) {
            BrandRespBO brandRespBO = new BrandRespBO();
            brandRespBO.setBrandId(0);
            brandRespBO.setGroupId(0);
            brandRespBO.setBrandName("");
            brandRespBO.setBrandNumber("");
            brandRespBO.setDescription("");
            brandRespBO.setState(0);
            map.put(brandId, brandRespBO);
        }
        log.info("[批量获取品牌信息] valueLoader 分页获取品牌信息, result={}", JSON.toJSONString(map));
        return map;
    }

}
