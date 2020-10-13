package com.coy.l2cache.example;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class CaffeineTestControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void refreshAfterWriteTest() {
        String url = HOST + "/refreshAfterWriteTest";

        String user = restTemplate.getForObject(url, String.class);
        System.out.println(JSON.toJSONString(user));
    }

}
