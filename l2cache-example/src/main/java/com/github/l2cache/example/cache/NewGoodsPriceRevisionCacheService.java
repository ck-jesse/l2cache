package com.github.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.l2cache.example.dto.GoodsPriceRevisionRespBO;
import com.github.l2cache.spring.biz.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class NewGoodsPriceRevisionCacheService extends AbstractCacheService<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> {

    public static final String CACHE_NAME = "goodsPriceRevisionCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        StringBuilder sb = new StringBuilder();
        sb.append(goodsPriceRevisionIdsReqDTO.getGoodsId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getGroupId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getOrganizationId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getGoodsGroupId());
        return sb.toString();
    }

    @Override
    protected GoodsPriceRevisionRespBO queryData(GoodsPriceRevisionIdsReqDTO key) {
        GoodsPriceRevisionRespBO goodsPriceRevisionRespBO = new GoodsPriceRevisionRespBO();
        goodsPriceRevisionRespBO.setGoodsPriceRevisionId(0);
        goodsPriceRevisionRespBO.setGroupId(0);
        goodsPriceRevisionRespBO.setOrganizationId(0);
        goodsPriceRevisionRespBO.setGoodsId(0);
        goodsPriceRevisionRespBO.setGoodsGroupId(0);
        goodsPriceRevisionRespBO.setAddTime(0L);
        goodsPriceRevisionRespBO.setUpdateTime(0L);
        goodsPriceRevisionRespBO.setState(0);
        log.info("查询信息,dto={},goodsPriceRevisionRespBO={}", JSON.toJSONString(key), JSON.toJSONString(goodsPriceRevisionRespBO));
        return goodsPriceRevisionRespBO;
    }

    @Override
    protected Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> queryDataList(List<GoodsPriceRevisionIdsReqDTO> keyList) {
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
