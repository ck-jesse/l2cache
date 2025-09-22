package com.github.jesse.l2cache.example.controller;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class NewBrandCacheControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void get() {
        String url = HOST + "/new/brand/get?brandId=1003";
        BrandRespBO result = restTemplate.getForObject(url, BrandRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void getOrLoad() {
        String url = HOST + "/new/brand/getOrLoad?brandId=1005";
        BrandRespBO result = restTemplate.getForObject(url, BrandRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void put() {
        String url = HOST + "/new/brand/put";
        BrandRespBO param = new BrandRespBO();
        param.setBrandId(1001);
        param.setGroupId(0);
        param.setBrandName("100199900023");
        param.setBrandNumber("1001");
        param.setDescription("brandId " + 1001);
        param.setState(0);
        ResponseEntity result = restTemplate.postForEntity(url, param, BrandRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void reload() {
        String url = HOST + "/new/brand/reload?brandId=1001";
        BrandRespBO result = restTemplate.getForObject(url, BrandRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void evict() {
        String url = HOST + "/new/brand/evict?brandId=1003";
        Boolean result = restTemplate.getForObject(url, Boolean.class);
        System.out.println(result);
    }

    @Test
    public void clear() {
        String url = HOST + "/new/brand/clear";
        Boolean result = restTemplate.getForObject(url, Boolean.class);
        System.out.println(result);
    }

    @Test
    public void batchGet() {
        String url = HOST + "/new/brand/batchGet";
        BrandIdListBO param = new BrandIdListBO();
        param.addBrandId(1001);
        param.addBrandId(1002);
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchGetOrLoad() {
        String url = HOST + "/new/brand/batchGetOrLoad";
        BrandIdListBO param = new BrandIdListBO();
        param.addBrandId(1001);
        param.addBrandId(1002);
        param.addBrandId(1003);
        param.addBrandId(1004);
        param.addBrandId(1005);
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchReload() {
        String url = HOST + "/new/brand/batchReload";
        BrandIdListBO param = new BrandIdListBO();
        param.addBrandId(1001);
        param.addBrandId(1002);
        param.addBrandId(1003);
        ResponseEntity result = restTemplate.postForEntity(url, param, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchEvict() {
        String url = HOST + "/new/brand/batchEvict";

        BrandIdListBO param = new BrandIdListBO();
        param.addBrandId(1001);
        param.addBrandId(1002);
        param.addBrandId(1003);
        ResponseEntity result = restTemplate.postForEntity(url, param, Boolean.class);
        System.out.println(JSON.toJSONString(result));
    }

}
