package com.github.jesse.l2cache.example.controller;

import com.github.jesse.l2cache.example.cache.NewBrandCacheService;
import com.github.jesse.l2cache.example.cache.OrderCacheService;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.example.dto.OrderIdListBO;
import com.github.jesse.l2cache.example.dto.OrderRespBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(value = "/order")
public class OrderCacheController {

    @Autowired
    OrderCacheService orderCacheService;

    @RequestMapping(value = "/get")
    public OrderRespBO get(String orderId) {
        return orderCacheService.get(orderId);
    }

    @RequestMapping(value = "/getOrLoad")
    public OrderRespBO getOrLoad(String orderId) {
        return orderCacheService.getOrLoad(orderId);
    }

    @PostMapping(value = "/put")
    public OrderRespBO put(@RequestBody OrderRespBO bo) {
        return orderCacheService.put(bo.getOrderId(), bo);
    }

    @RequestMapping(value = "/reload")
    public OrderRespBO reload(String orderId) {
        return orderCacheService.reload(orderId);
    }

    @RequestMapping(value = "/evict")
    public Boolean evict(String orderId) {
        orderCacheService.evict(orderId);
        return true;
    }

    @RequestMapping(value = "/clear")
    public Boolean clear() {
        orderCacheService.clear();
        return true;
    }

    @PostMapping(value = "/batchGet")
    public Map<String, OrderRespBO> batchGet(@RequestBody OrderIdListBO bo) {
        Map<String, OrderRespBO> map = orderCacheService.batchGet(bo.getOrderIdList());
        return map;
    }

    @PostMapping(value = "/batchGetOrLoad")
    public Map<String, OrderRespBO> batchGetOrLoad(@RequestBody OrderIdListBO bo) {
        Map<String, OrderRespBO> map = orderCacheService.batchGetOrLoad(bo.getOrderIdList());
        return map;
    }

    @PostMapping(value = "/batchReload")
    public Map<String, OrderRespBO> batchReload(@RequestBody OrderIdListBO bo) {
        Map<String, OrderRespBO> map = orderCacheService.batchReload(bo.getOrderIdList());
        return map;
    }

    @RequestMapping(value = "/batchEvict")
    public Boolean batchEvict(@RequestBody OrderIdListBO bo) {
        orderCacheService.batchEvict(bo.getOrderIdList());
        return true;
    }
}
