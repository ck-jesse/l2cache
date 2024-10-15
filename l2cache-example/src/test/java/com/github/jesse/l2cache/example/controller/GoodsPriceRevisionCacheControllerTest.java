package com.github.jesse.l2cache.example.controller;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsPutReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoodsPriceRevisionCacheControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void get() {
        String url = HOST + "/revision/get";
        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setTenantId(1);
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void get1() {
        String url = HOST + "/revision/get";
        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setTenantId(1);
        param.setGoodsGroupId(1);
        param.setGoodsId(5001);

        System.out.println(JSON.toJSONString(param));
        for (int i = 0; i < 10; i++) {
            ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
            System.out.println(JSON.toJSONString(result));
        }
    }

    @Test
    public void getOrLoad() {
        String url = HOST + "/revision/getOrLoad";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setTenantId(1);
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void put() {
        String url = HOST + "/revision/put";

        GoodsPriceRevisionIdsReqDTO reqDTO = new GoodsPriceRevisionIdsReqDTO();
        reqDTO.setTenantId(1);
        reqDTO.setGoodsGroupId(1);
        reqDTO.setGoodsId(1001);

        GoodsPriceRevisionRespBO respBO = new GoodsPriceRevisionRespBO();
        respBO.setGoodsPriceRevisionId(100);
        respBO.setGoodsGroupId(1);
        respBO.setOrganizationId(2);
        respBO.setGroupId(3);
        respBO.setGoodsId(1002);
        respBO.setAddTime(123456L);
        respBO.setUpdateTime(0L);
        respBO.setState(1);

        GoodsPriceRevisionIdsPutReqDTO param = new GoodsPriceRevisionIdsPutReqDTO();
        param.setGoodsPriceRevisionIdsReqDTO(reqDTO);
        param.setGoodsPriceRevisionRespBO(respBO);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void reload() {
        String url = HOST + "/revision/reload";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setTenantId(1);
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void evict() {
        String url = HOST + "/revision/evict";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setTenantId(1);
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, Boolean.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void clear() {
        String url = HOST + "/revision/clear";

        Boolean result = restTemplate.getForObject(url, Boolean.class);
        System.out.println(result);
    }

    @Test
    public void batchGet() {
        String url = HOST + "/revision/batchGet";

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        GoodsPriceRevisionIdsReqDTO param = null;
        for (int i = 1; i <= 5; i++) {
            param = new GoodsPriceRevisionIdsReqDTO();
            param.setTenantId(1);
            param.setGoodsGroupId(1);
            param.setGoodsId(1000 + i);
            keyList.add(param);
        }

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchGetOrLoad() {
        String url = HOST + "/revision/batchGetOrLoad";

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        GoodsPriceRevisionIdsReqDTO param = null;
        for (int i = 1; i <= 10; i++) {
            param = new GoodsPriceRevisionIdsReqDTO();
            param.setTenantId(1);
            param.setGoodsGroupId(1);
            param.setGoodsId(5000 + i);
            keyList.add(param);
        }

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchReload() {
        String url = HOST + "/revision/batchReload";

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        GoodsPriceRevisionIdsReqDTO param = null;
        for (int i = 1; i <= 5; i++) {
            param = new GoodsPriceRevisionIdsReqDTO();
            param.setTenantId(1);
            param.setGoodsGroupId(1);
            param.setGoodsId(1000 + i);
            keyList.add(param);
        }

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchEvict() {
        String url = HOST + "/revision/batchEvict";

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        GoodsPriceRevisionIdsReqDTO param = null;
        for (int i = 1; i <= 10; i++) {
            param = new GoodsPriceRevisionIdsReqDTO();
            param.setTenantId(1);
            param.setGoodsGroupId(1);
            param.setGoodsId(5000 + i);
            keyList.add(param);
        }

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Boolean.class);
        System.out.println(JSON.toJSONString(result));
    }

}
