package com.coy.l2cache.example.controller;

import com.coy.l2cache.example.cache.BrandCacheService;
import com.coy.l2cache.example.cache.GoodsPriceRevisionCacheService;
import com.coy.l2cache.example.dto.BrandRespBO;
import com.coy.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.coy.l2cache.example.dto.GoodsPriceRevisionRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TestController {

    @Autowired
    GoodsPriceRevisionCacheService goodsPriceRevisionCacheService;

    @Autowired
    BrandCacheService brandCacheService;

    @RequestMapping(value = "/priceGet")
    public GoodsPriceRevisionRespBO priceGet() {
        GoodsPriceRevisionIdsReqDTO dto = new GoodsPriceRevisionIdsReqDTO();
        dto.setGoodsGroupId(0);
        dto.setOrganizationId(0);
        dto.setGroupId(0);
        dto.setGoodsId(0);
        return goodsPriceRevisionCacheService.get(dto);
    }

    @RequestMapping(value = "/priceGetOrLoad")
    public GoodsPriceRevisionRespBO priceGetOrLoad() {
        GoodsPriceRevisionIdsReqDTO dto = new GoodsPriceRevisionIdsReqDTO();
        dto.setGoodsGroupId(0);
        dto.setOrganizationId(0);
        dto.setGroupId(0);
        dto.setGoodsId(0);
        return goodsPriceRevisionCacheService.getOrLoad(dto);
    }

    @RequestMapping(value = "/brandGet")
    public BrandRespBO brandGet() {
        return brandCacheService.get(11);
    }

    @RequestMapping(value = "/brandGetOrLoad")
    public BrandRespBO brandGetOrLoad() {
        return brandCacheService.getOrLoad(11);
    }
}
