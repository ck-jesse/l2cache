package com.coy.l2cache.example;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class ControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void queryUser() {
        String url = HOST + "/queryUser?userId=cck";

        User user = restTemplate.getForObject(url, User.class);

        System.out.println(JSON.toJSONString(user));
    }

    @Test
    public void queryUserSync() {
        String url = HOST + "/queryUserSync?userId=cck";

        List user = restTemplate.getForObject(url, List.class);

        System.out.println(JSON.toJSONString(user));
    }


}
