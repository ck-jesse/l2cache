package com.github.jesse.l2cache.example;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.BrandIdListBO;
import com.github.jesse.l2cache.example.dto.BrandRespBO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsPutReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionIdsReqDTO;
import com.github.jesse.l2cache.example.dto.GoodsPriceRevisionRespBO;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewGoodsPriceRevisionCacheControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void get() {
        String url = HOST + "/new/revision/get";
        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void getOrLoad() {
        String url = HOST + "/new/revision/getOrLoad";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void put() {
        String url = HOST + "/new/revision/put";

        GoodsPriceRevisionIdsReqDTO reqDTO = new GoodsPriceRevisionIdsReqDTO();
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
        String url = HOST + "/new/revision/reload";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, GoodsPriceRevisionRespBO.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void evict() {
        String url = HOST + "/new/revision/evict";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        ResponseEntity result = restTemplate.postForEntity(url, param, Boolean.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchGet() {
        String url = HOST + "/new/revision/batchGet";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        GoodsPriceRevisionIdsReqDTO param2 = new GoodsPriceRevisionIdsReqDTO();
        param2.setGoodsGroupId(1);
        param2.setGoodsId(1002);

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        keyList.add(param);
        keyList.add(param2);

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchGetOrLoad() {
        String url = HOST + "/new/revision/batchGetOrLoad";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        GoodsPriceRevisionIdsReqDTO param2 = new GoodsPriceRevisionIdsReqDTO();
        param2.setGoodsGroupId(1);
        param2.setGoodsId(1002);

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        keyList.add(param);
        keyList.add(param2);

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }

    @Test
    public void batchReload() {
        String url = HOST + "/new/revision/batchReload";

        GoodsPriceRevisionIdsReqDTO param = new GoodsPriceRevisionIdsReqDTO();
        param.setGoodsGroupId(1);
        param.setGoodsId(1001);

        GoodsPriceRevisionIdsReqDTO param2 = new GoodsPriceRevisionIdsReqDTO();
        param2.setGoodsGroupId(1);
        param2.setGoodsId(1002);

        List<GoodsPriceRevisionIdsReqDTO> keyList = new ArrayList<>();
        keyList.add(param);
        keyList.add(param2);

        ResponseEntity result = restTemplate.postForEntity(url, keyList, Map.class);
        System.out.println(JSON.toJSONString(result));
    }


}
