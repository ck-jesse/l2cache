package com.github.jesse.l2cache.example.controller;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.example.dto.OrderIdListBO;
import com.github.jesse.l2cache.example.dto.OrderRespBO;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class OrderCacheControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void get() {
        String url = HOST + "/order/get?orderId=1001";
        OrderRespBO result = restTemplate.getForObject(url, OrderRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void getOrLoad() {
        String url = HOST + "/order/getOrLoad?orderId=1002";
        OrderRespBO result = restTemplate.getForObject(url, OrderRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void put() {
        String url = HOST + "/order/put";
        OrderRespBO orderRespBO = new OrderRespBO();
        orderRespBO.setOrderId("orderId");
        orderRespBO.setUserName("test");
        orderRespBO.setGoodsNum(1);
        ResponseEntity result = restTemplate.postForEntity(url, orderRespBO, BrandRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void reload() {
        String url = HOST + "/order/reload?orderId=1001";
        OrderRespBO result = restTemplate.getForObject(url, OrderRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void evict() {
        String url = HOST + "/order/evict?orderId=1003";
        Boolean result = restTemplate.getForObject(url, Boolean.class);
        System.out.println(result);
    }

    @Test
    public void clear() {
        String url = HOST + "/order/clear";
        Boolean result = restTemplate.getForObject(url, Boolean.class);
        System.out.println(result);
    }

    @Test
    public void batchGet() {
        String url = HOST + "/order/batchGet";
        OrderIdListBO param = new OrderIdListBO();
        param.addOrderId("1001");
        param.addOrderId("1002");
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchGetOrLoad() {
        String url = HOST + "/order/batchGetOrLoad";
        OrderIdListBO param = new OrderIdListBO();
        param.addOrderId("1001");
        param.addOrderId("1002");
        param.addOrderId("1003");
        param.addOrderId("1004");
        param.addOrderId("1005");
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchReload() {
        String url = HOST + "/order/batchReload";
        OrderIdListBO param = new OrderIdListBO();
        param.addOrderId("1001");
        param.addOrderId("1002");
        param.addOrderId("1003");
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchEvict() {
        String url = HOST + "/order/batchEvict";

        OrderIdListBO param = new OrderIdListBO();
        param.addOrderId("1001");
        param.addOrderId("1002");
        param.addOrderId("1003");
        ResponseEntity result = restTemplate.postForEntity(url, param, Boolean.class);
        System.out.println(JSON.toJSONString(result));
    }

}
