package com.github.jesse.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
import com.github.jesse.l2cache.spring.biz.AbstractCacheService;
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

    public static final String CACHE_NAME = "newGoodsPriceRevisionCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public String buildCacheKey(GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        StringBuilder sb = new StringBuilder();
        // 可在此处动态拼接多租户ID，分隔符也可按照自己的述求进行选择
        // 比如冒号 ":"，冒号的用处，当缓存在redis中时，冒号可以查看
        // 比如下划线 "_"，下划线就仅仅是一个普通的分隔符了
        sb.append(goodsPriceRevisionIdsReqDTO.getTenantId());
        sb.append(":").append(goodsPriceRevisionIdsReqDTO.getGoodsGroupId());
        sb.append("_").append(goodsPriceRevisionIdsReqDTO.getGoodsId());
        return sb.toString();
    }

    @Override
    public GoodsPriceRevisionRespBO queryData(GoodsPriceRevisionIdsReqDTO key) {
        GoodsPriceRevisionRespBO goodsPriceRevisionRespBO = new GoodsPriceRevisionRespBO();
        goodsPriceRevisionRespBO.setGoodsPriceRevisionId(0);
        goodsPriceRevisionRespBO.setGroupId(0);
        goodsPriceRevisionRespBO.setOrganizationId(0);
        goodsPriceRevisionRespBO.setGoodsId(key.getGoodsId());
        goodsPriceRevisionRespBO.setGoodsGroupId(key.getGoodsGroupId());
        goodsPriceRevisionRespBO.setAddTime(0L);
        goodsPriceRevisionRespBO.setUpdateTime(0L);
        goodsPriceRevisionRespBO.setState(0);
        log.info("查询信息,dto={},goodsPriceRevisionRespBO={}", JSON.toJSONString(key), JSON.toJSONString(goodsPriceRevisionRespBO));
        return goodsPriceRevisionRespBO;
    }

    @Override
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> queryDataList(List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = new HashMap<>();

        // 模拟返回数据，业务系统中可直接从DB加载数据
        keyList.forEach(goodsPriceRevisionIdsReqDTO -> {
            GoodsPriceRevisionRespBO goodsPriceRevisionRespBO = new GoodsPriceRevisionRespBO();
            goodsPriceRevisionRespBO.setGoodsPriceRevisionId(0);
            goodsPriceRevisionRespBO.setGroupId(111);
            goodsPriceRevisionRespBO.setOrganizationId(0);
            goodsPriceRevisionRespBO.setGoodsId(goodsPriceRevisionIdsReqDTO.getGoodsId());
            goodsPriceRevisionRespBO.setGoodsGroupId(goodsPriceRevisionIdsReqDTO.getGoodsGroupId());
            goodsPriceRevisionRespBO.setAddTime(0L);
            goodsPriceRevisionRespBO.setUpdateTime(0L);
            goodsPriceRevisionRespBO.setState(0);
            map.put(goodsPriceRevisionIdsReqDTO, goodsPriceRevisionRespBO);
        });
        log.info("[批量获取品牌信息] valueLoader 分页获取品牌信息, result={}", JSON.toJSONString(map));
        return map;
    }
}
