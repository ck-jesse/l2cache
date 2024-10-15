package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.example.cache.BrandCacheService;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/brand")
public class BrandCacheController {

    @Autowired
    BrandCacheService brandCacheService;

    @RequestMapping(value = "/get")
    public BrandRespBO get(Integer brandId) {
        return brandCacheService.get(brandId);
    }

    @RequestMapping(value = "/getOrLoad")
    public BrandRespBO getOrLoad(Integer brandId) {
        return brandCacheService.getOrLoad(brandId);
    }

    @PostMapping(value = "/put")
    public BrandRespBO put(@RequestBody BrandRespBO brandRespBO) {
        return brandCacheService.put(brandRespBO.getBrandId(), brandRespBO);
    }

    @RequestMapping(value = "/reload")
    public BrandRespBO reload(Integer brandId) {
        return brandCacheService.reload(brandId);
    }

    @RequestMapping(value = "/evict")
    public Boolean evict(Integer brandId) {
        brandCacheService.evict(brandId);
        return true;
    }

    @RequestMapping(value = "/clear")
    public Boolean clear() {
        brandCacheService.clear();
        return true;
    }

    @PostMapping(value = "/batchGet")
    public Map<Integer, BrandRespBO> batchGet(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = brandCacheService.batchGet(bo.getBrandIdList());
        return map;
    }

    @PostMapping(value = "/batchGetOrLoad")
    public Map<Integer, BrandRespBO> batchGetOrLoad(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = brandCacheService.batchGetOrLoad(bo.getBrandIdList());
        return map;
    }

    @PostMapping(value = "/batchReload")
    public Map<Integer, BrandRespBO> batchReload(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = brandCacheService.batchReload(bo.getBrandIdList());
        return map;
    }

    @RequestMapping(value = "/batchEvict")
    public Boolean batchEvict(@RequestBody BrandIdListBO bo) {
        brandCacheService.batchEvict(bo.getBrandIdList());
        return true;
    }
}
