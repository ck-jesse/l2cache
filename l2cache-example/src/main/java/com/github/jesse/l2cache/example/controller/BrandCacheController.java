package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.example.cache.BrandCacheService;
import com.github.jesse.l2cache.example.cache.GoodsPriceRevisionCacheService;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/brand")
public class BrandCacheController {

    @Autowired
    GoodsPriceRevisionCacheService goodsPriceRevisionCacheService;

    @Autowired
    BrandCacheService brandCacheService;

    @RequestMapping(value = "/priceGet")
    public GoodsPriceRevisionRespBO priceGet() {
        GoodsPriceRevisionIdsReqDTO dto = new GoodsPriceRevisionIdsReqDTO();
        dto.setGoodsGroupId(0);
        dto.setGoodsId(0);
        return goodsPriceRevisionCacheService.get(dto);
    }

    @RequestMapping(value = "/priceGetOrLoad")
    public GoodsPriceRevisionRespBO priceGetOrLoad() {
        GoodsPriceRevisionIdsReqDTO dto = new GoodsPriceRevisionIdsReqDTO();
        dto.setGoodsGroupId(0);
        dto.setGoodsId(0);
        return goodsPriceRevisionCacheService.getOrLoad(dto);
    }

    @RequestMapping(value = "/get")
    public BrandRespBO get() {
        return brandCacheService.get(11);
    }

    @RequestMapping(value = "/getOrLoad")
    public BrandRespBO getOrLoad() {
        return brandCacheService.getOrLoad(11);
    }
}
