package com.coy.l2cache.example;

import com.alibaba.fastjson.JSON;
import com.coy.l2cache.util.ServiceResult;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class CacheManagerControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void getCacheNames() {
        String url = HOST + "/l2cache/manager/getCacheNames";

        ServiceResult user = restTemplate.getForObject(url, ServiceResult.class);

        System.out.println(JSON.toJSONString(user));
    }

    @Test
    public void getCacheConfig() {
        String url = HOST + "/l2cache/manager/getCacheConfig?cacheName=userCache";

        ServiceResult user = restTemplate.getForObject(url, ServiceResult.class);
        System.out.println(JSON.toJSONString(user));
    }

    @Test
    public void get() {
        String url = HOST + "/l2cache/manager/get?cacheName=userCache&key=user01";

        ServiceResult user = restTemplate.getForObject(url, ServiceResult.class);
        System.out.println(JSON.toJSONString(user));
    }

    @Test
    public void evict() {
        String url = HOST + "/l2cache/manager/evit?cacheName=userCache&key=user01";

        ServiceResult user = restTemplate.getForObject(url, ServiceResult.class);
        System.out.println(JSON.toJSONString(user));
    }


}
