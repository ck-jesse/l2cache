package com.github.l2cache.example.controller;

import com.github.l2cache.example.cache.NewBrandCacheService;
import com.github.l2cache.example.dto.BrandIdListBO;
import com.github.l2cache.example.dto.BrandRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public BrandRespBO get(Integer brandId) {
        return newBrandCacheService.get(brandId);
    }

    @RequestMapping(value = "/getOrLoad")
    public BrandRespBO getOrLoad(Integer brandId) {
        return newBrandCacheService.getOrLoad(brandId);
    }

    @PostMapping(value = "/put")
    public BrandRespBO put(@RequestBody BrandRespBO brandRespBO) {
        return newBrandCacheService.put(brandRespBO.getBrandId(), brandRespBO);
    }

    @RequestMapping(value = "/reload")
    public BrandRespBO reload(Integer brandId) {
        return newBrandCacheService.reload(brandId);
    }

    @RequestMapping(value = "/evict")
    public Boolean evict(Integer brandId) {
        newBrandCacheService.evict(brandId);
        return true;
    }

    @PostMapping(value = "/batchGet")
    public Map<Integer, BrandRespBO> batchGet(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchGet(bo.getBrandIdList());
        return map;
    }

    @PostMapping(value = "/batchGetOrLoad")
    public Map<Integer, BrandRespBO> batchGetOrLoad(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchGetOrLoad(bo.getBrandIdList());
        return map;
    }

    @PostMapping(value = "/batchReload")
    public Map<Integer, BrandRespBO> batchReload(@RequestBody BrandIdListBO bo) {
        Map<Integer, BrandRespBO> map = newBrandCacheService.batchReload(bo.getBrandIdList());
        return map;
    }
}
