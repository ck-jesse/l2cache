package com.github.jesse.l2cache.example.cache;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.biz.mutil.CacheNameRedissonClientAnno;
import com.github.jesse.l2cache.example.dto.OrderRespBO;
import com.github.jesse.l2cache.spring.biz.AbstractCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多redis实例的场景：指定redissonClient实例
 */
@CacheNameRedissonClientAnno(instanceId = "redissonClient2")
@Component
@Slf4j
public class OrderCacheService extends AbstractCacheService<String, OrderRespBO> {
    public static final String CACHE_NAME = "orderCache";

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public OrderRespBO queryData(String orderId) {
        OrderRespBO orderRespBO = new OrderRespBO();
        orderRespBO.setOrderId(orderId);
        orderRespBO.setUserName("test");
        orderRespBO.setGoodsNum(1);
        log.info("查询订单相关信息,orderId={},orderRespBO={}", orderId, JSON.toJSONString(orderRespBO));
        return orderRespBO;
    }

    @Override
    public Map<String, OrderRespBO> queryDataList(List<String> notHitCacheKeyList) {
        Map<String, OrderRespBO> map = new HashMap<>();
        // 模拟返回数据，业务系统中可直接从DB加载数据
        for (int i = 0; i < notHitCacheKeyList.size(); i++) {
            String orderId = notHitCacheKeyList.get(i);
            OrderRespBO orderRespBO = new OrderRespBO();
            orderRespBO.setOrderId(orderId);
            orderRespBO.setUserName("test" + i);
            orderRespBO.setGoodsNum(1);
            map.put(orderId, orderRespBO);
        }
        log.info("[批量获取订单信息] valueLoader 分页获取订单信息, result={}", JSON.toJSONString(map));
        return map;
    }
}
