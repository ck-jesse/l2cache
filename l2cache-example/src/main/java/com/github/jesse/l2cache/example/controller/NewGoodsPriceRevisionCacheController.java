package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.example.cache.NewBrandCacheService;
import com.github.jesse.l2cache.example.cache.NewGoodsPriceRevisionCacheService;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsPutReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/new/revision")
public class NewGoodsPriceRevisionCacheController {

    @Autowired
    NewGoodsPriceRevisionCacheService newGoodsPriceRevisionCacheService;

    @PostMapping(value = "/get")
    public GoodsPriceRevisionRespBO get(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return newGoodsPriceRevisionCacheService.get(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/getOrLoad")
    public GoodsPriceRevisionRespBO getOrLoad(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return newGoodsPriceRevisionCacheService.getOrLoad(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/put")
    public GoodsPriceRevisionRespBO put(@RequestBody GoodsPriceRevisionIdsPutReqDTO param) {
        return newGoodsPriceRevisionCacheService.put(param.getGoodsPriceRevisionIdsReqDTO(), param.getGoodsPriceRevisionRespBO());
    }

    @PostMapping(value = "/reload")
    public GoodsPriceRevisionRespBO reload(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return newGoodsPriceRevisionCacheService.reload(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/evict")
    public Boolean evict(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        newGoodsPriceRevisionCacheService.evict(goodsPriceRevisionIdsReqDTO);
        return true;
    }

    @PostMapping(value = "/batchGet")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGet(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = newGoodsPriceRevisionCacheService.batchGet(keyList);
        return map;
    }

    @PostMapping(value = "/batchGetOrLoad")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGetOrLoad(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = newGoodsPriceRevisionCacheService.batchGetOrLoad(keyList);
        return map;
    }

    @PostMapping(value = "/batchReload")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchReload(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = newGoodsPriceRevisionCacheService.batchReload(keyList);
        return map;
    }
}
