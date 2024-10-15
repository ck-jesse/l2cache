package com.github.jesse.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
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
 * 为了简化业务开发，重新优化 CacheServcie 顶层抽象接口。
 * 优化前，需要实现的方法较多，且有很多共性的代码
 * 优化后，最多只需要实现3个业务相关的方法，使得开发更加简单，维护也更加简单
 *
 * @author chenck
 * @date 2022/8/4 22:23
 */
@Component
@Slf4j
public class GoodsPriceRevisionCacheService extends AbstractCacheService<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> {

    public static final String CACHE_NAME = "goodsPriceRevisionCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        StringBuilder sb = new StringBuilder();
        sb.append(goodsPriceRevisionIdsReqDTO.getTenantId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getGoodsGroupId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getGoodsId());
        return sb.toString();
    }

    @Cacheable(value = CACHE_NAME, key = "#dto.tenantId+'_'+#dto.goodsGroupId+'_'" + "+#dto.goodsId", sync = true)
    @Override
    public GoodsPriceRevisionRespBO getOrLoad(GoodsPriceRevisionIdsReqDTO dto) {
        return this.reload(dto);
    }

    @CachePut(value = CACHE_NAME, key = "#dto.tenantId+'_'+#dto.goodsGroupId+'_'" + "+#dto.goodsId")
    @Override
    public GoodsPriceRevisionRespBO put(GoodsPriceRevisionIdsReqDTO dto, GoodsPriceRevisionRespBO goodsPriceRevisionRespBO) {
        return goodsPriceRevisionRespBO;
    }

    @CachePut(value = CACHE_NAME, key = "#dto.tenantId+'_'+#dto.goodsGroupId+'_'" + "+#dto.goodsId")
    @Override
    public GoodsPriceRevisionRespBO reload(GoodsPriceRevisionIdsReqDTO dto) {
        return this.queryData(dto);
    }

    @CacheEvict(value = CACHE_NAME, key = "#dto.tenantId+'_'+#dto.goodsGroupId+'_'" + "+#dto.goodsId")
    @Override
    public void evict(GoodsPriceRevisionIdsReqDTO dto) {
        log.info("[evict] cacheName={}, goodsPriceRevisionIdsReqDTO={}", CACHE_NAME, JSON.toJSONString(dto));
    }

    @Override
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGet(List<GoodsPriceRevisionIdsReqDTO> keyList) {
        // 定义缓存key构建器
        Function<GoodsPriceRevisionIdsReqDTO, Object> cacheKeyBuilder = dto -> GoodsPriceRevisionCacheService.this.buildCacheKey(dto);

        // 批量查询信息
        return this.getNativeL2cache().batchGet(keyList, cacheKeyBuilder);
    }

    @Override
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGetOrLoad(List<GoodsPriceRevisionIdsReqDTO> keyList) {
        // 定义缓存key构建器
        Function<GoodsPriceRevisionIdsReqDTO, Object> cacheKeyBuilder = dto -> GoodsPriceRevisionCacheService.this.buildCacheKey(dto);

        // 从DB中加载数据
        Function<List<GoodsPriceRevisionIdsReqDTO>, Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO>> valueLoader = notHitCacheKeyList -> this.queryDataList(notHitCacheKeyList);

        // 批量查询信息
        return this.getNativeL2cache().batchGetOrLoad(keyList, cacheKeyBuilder, valueLoader);
    }


    @Override
    public GoodsPriceRevisionRespBO queryData(GoodsPriceRevisionIdsReqDTO dto) {
        GoodsPriceRevisionRespBO goodsPriceRevisionRespBO = new GoodsPriceRevisionRespBO();
        goodsPriceRevisionRespBO.setGoodsPriceRevisionId(0);
        goodsPriceRevisionRespBO.setGroupId(0);
        goodsPriceRevisionRespBO.setOrganizationId(0);
        goodsPriceRevisionRespBO.setGoodsId(0);
        goodsPriceRevisionRespBO.setGoodsGroupId(0);
        goodsPriceRevisionRespBO.setAddTime(0L);
        goodsPriceRevisionRespBO.setUpdateTime(0L);
        goodsPriceRevisionRespBO.setState(0);
        log.info("查询信息,dto={},goodsPriceRevisionRespBO={}", JSON.toJSONString(dto), JSON.toJSONString(goodsPriceRevisionRespBO));
        return goodsPriceRevisionRespBO;
    }

    @Override
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> queryDataList(List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = new HashMap<>();

        // 模拟返回数据，业务系统中可直接从DB加载数据
        keyList.forEach(goodsPriceRevisionIdsReqDTO -> {
            GoodsPriceRevisionRespBO goodsPriceRevisionRespBO = new GoodsPriceRevisionRespBO();
            goodsPriceRevisionRespBO.setGoodsPriceRevisionId(0);
            goodsPriceRevisionRespBO.setGroupId(0);
            goodsPriceRevisionRespBO.setOrganizationId(0);
            goodsPriceRevisionRespBO.setGoodsId(0);
            goodsPriceRevisionRespBO.setGoodsGroupId(0);
            goodsPriceRevisionRespBO.setAddTime(0L);
            goodsPriceRevisionRespBO.setUpdateTime(0L);
            goodsPriceRevisionRespBO.setState(0);
            map.put(goodsPriceRevisionIdsReqDTO, goodsPriceRevisionRespBO);
        });
        log.info("[批量获取品牌信息] valueLoader 分页获取品牌信息, result={}", JSON.toJSONString(map));
        return map;
    }
}
