package com.github.jesse.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.spring.biz.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三阶段：重新优化 抽象出的缓存使用规范 CacheService，进一步简化业务开发
 * <p>
 * 优点：极简开发，完全屏蔽复杂的缓存实现细节，仅需实现几个业务方法，使得开发和维护更加简单，且解决随着业务迭代，代码坏味道递增的问题
 * 缺点：目前暂未发现明显缺点，若有，请留言。
 *
 * @author chenck
 * @date 2022/8/4 22:23
 */
@Component
@Slf4j
public class NewBrandCacheService extends AbstractCacheService<Integer, BrandRespBO> {

    public static final String CACHE_NAME = "newBrandCache";

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
        if (null == brandId || 1002 == brandId) {
            return null;
        }
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
            if (1002 == brandId) {
                continue;
            }
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
