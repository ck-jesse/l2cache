package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.example.cache.GoodsPriceRevisionCacheService;
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
@RequestMapping(value = "/revision")
public class GoodsPriceRevisionCacheController {

    @Autowired
    GoodsPriceRevisionCacheService goodsPriceRevisionCacheService;

    @PostMapping(value = "/get")
    public GoodsPriceRevisionRespBO get(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return goodsPriceRevisionCacheService.get(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/getOrLoad")
    public GoodsPriceRevisionRespBO getOrLoad(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return goodsPriceRevisionCacheService.getOrLoad(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/put")
    public GoodsPriceRevisionRespBO put(@RequestBody GoodsPriceRevisionIdsPutReqDTO param) {
        return goodsPriceRevisionCacheService.put(param.getGoodsPriceRevisionIdsReqDTO(), param.getGoodsPriceRevisionRespBO());
    }

    @PostMapping(value = "/reload")
    public GoodsPriceRevisionRespBO reload(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        return goodsPriceRevisionCacheService.reload(goodsPriceRevisionIdsReqDTO);
    }

    @PostMapping(value = "/evict")
    public Boolean evict(@RequestBody GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO) {
        goodsPriceRevisionCacheService.evict(goodsPriceRevisionIdsReqDTO);
        return true;
    }

    @RequestMapping(value = "/clear")
    public Boolean clear() {
        goodsPriceRevisionCacheService.clear();
        return true;
    }

    @PostMapping(value = "/batchGet")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGet(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = goodsPriceRevisionCacheService.batchGet(keyList);
        return map;
    }

    @PostMapping(value = "/batchGetOrLoad")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchGetOrLoad(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = goodsPriceRevisionCacheService.batchGetOrLoad(keyList);
        return map;
    }

    @PostMapping(value = "/batchReload")
    public Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> batchReload(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        Map<GoodsPriceRevisionIdsReqDTO, GoodsPriceRevisionRespBO> map = goodsPriceRevisionCacheService.batchReload(keyList);
        return map;
    }

    @PostMapping(value = "/batchEvict")
    public Boolean batchEvict(@RequestBody List<GoodsPriceRevisionIdsReqDTO> keyList) {
        goodsPriceRevisionCacheService.batchEvict(keyList);
        return true;
    }
}
