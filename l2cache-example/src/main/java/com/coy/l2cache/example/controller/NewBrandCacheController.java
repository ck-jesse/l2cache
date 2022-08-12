package com.coy.l2cache.example.controller;

import com.coy.l2cache.example.cache.NewBrandCacheService;
import com.coy.l2cache.example.dto.BrandRespBO;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.K;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/new/brand")
public class NewBrandCacheController {

    @Autowired
    NewBrandCacheService newBrandCacheService;

    @RequestMapping(value = "/get")
    public BrandRespBO get() {
        return newBrandCacheService.get(11);
    }

    @RequestMapping(value = "/getOrLoad")
    public BrandRespBO getOrLoad() {
        return newBrandCacheService.getOrLoad(11);
    }

    @RequestMapping(value = "/put")
    public BrandRespBO put() {
        Integer brandId = 0;
        BrandRespBO brandRespBO = new BrandRespBO();
        return newBrandCacheService.put(brandId, brandRespBO);
    }

    @RequestMapping(value = "/reload")
    public BrandRespBO reload() {
        Integer brandId = 0;
        return newBrandCacheService.reload(brandId);
    }

    @RequestMapping(value = "/evict")
    public Boolean evict() {
        newBrandCacheService.evict(11);
        return true;
    }

    @RequestMapping(value = "/batchGet")
    public Map<Integer, BrandRespBO> batchGet() {
        List<Integer> keyList = new ArrayList<>();
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchGet(keyList);
        return map;
    }

    @RequestMapping(value = "/batchGetOrLoad")
    public Map<Integer, BrandRespBO> batchGetOrLoad() {
        List<Integer> keyList = new ArrayList<>();
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchGetOrLoad(keyList);
        return map;
    }

    @RequestMapping(value = "/batchReload")
    public Map<Integer, BrandRespBO> batchReload() {
        List<Integer> keyList = new ArrayList<>();
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchReload(keyList);
        return map;
    }
}
