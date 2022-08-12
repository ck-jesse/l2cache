package com.github.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.l2cache.example.dto.BrandRespBO;
import com.github.l2cache.spring.biz.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CacheService 的demo案例
 * 注：
 * 为了进一步简化业务开发，重新优化 CacheService 顶层抽象接口。
 * 优化后的demo案例，只需要实现3个方法，使得开发更加简单，维护也更加简单，开发人员只需要聚焦在具体的业务逻辑
 *
 * @author chenck
 * @date 2022/8/4 22:23
 */
@Component
@Slf4j
public class NewBrandCacheService extends AbstractCacheService<Integer, BrandRespBO> {

    public static final String CACHE_NAME = "brandCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(Integer brandId) {
        return String.valueOf(brandId);
    }

    @Override
    public BrandRespBO queryData(Integer brandId) {
        // 模拟返回数据，业务系统中可直接从DB加载数据
        BrandRespBO brandRespBO = new BrandRespBO();
        brandRespBO.setBrandId(brandId);
        brandRespBO.setGroupId(0);
        brandRespBO.setBrandName("");
        brandRespBO.setBrandNumber("");
        brandRespBO.setDescription("brandId " + brandId);
        brandRespBO.setState(0);
        log.info("查询获取品牌相关信息,brandId={},brandInfoRespBO={}", brandId, JSON.toJSONString(brandRespBO));
        return brandRespBO;
    }

    @Override
    public Map<Integer, BrandRespBO> queryDataList(List<Integer> notHitCacheKeyList) {
        Map<Integer, BrandRespBO> map = new HashMap<>();
        // 模拟返回数据，业务系统中可直接从DB加载数据
        for (Integer brandId : notHitCacheKeyList) {
            BrandRespBO brandRespBO = new BrandRespBO();
            brandRespBO.setBrandId(brandId);
            brandRespBO.setGroupId(0);
            brandRespBO.setBrandName("");
            brandRespBO.setBrandNumber("");
            brandRespBO.setDescription("brandId " + brandId);
            brandRespBO.setState(0);
            map.put(brandId, brandRespBO);
        }
        log.info("[批量获取品牌信息] valueLoader 分页获取品牌信息, result={}", JSON.toJSONString(map));
        return map;
    }

}
