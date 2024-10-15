package com.github.jesse.l2cache.example.controller;

import com.alibaba.fastjson.JSON;
import com.github.jesse.l2cache.example.dto.User;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

public class UserCacheControllerTest {

    private static final String HOST = "http://127.0.0.1:8080";

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void queryUser() {
        String url = HOST + "/queryUser?userId=user01";

        User user = restTemplate.getForObject(url, User.class);

        System.out.println(JSON.toJSONString(user));
    }

    @Test
    public void queryUserSync() {
        String url = HOST + "/queryUserSync?userId=user01222";

        User user = restTemplate.getForObject(url, User.class);
        System.out.println(JSON.toJSONString(user));
    }

    /**
     * 模拟某个key对应的valueLoader被gc回收后的场景
     */
    @Test
    public void delValueLoader() {
        String url = HOST + "/delValueLoader?userId=user01";

        String user = restTemplate.getForObject(url, String.class);
        System.out.println(JSON.toJSONString(user));
    }

    /**
     * 仅仅put一个值到l2cache，这时valueLoader=null，当缓存过期后，自动刷新时，应该直接淘汰该缓存，而不是缓存一个NullValue
     */
    @Test
    public void justput() {
        String url = HOST + "/justput?userId=user01222";

        String user = restTemplate.getForObject(url, String.class);
        System.out.println(JSON.toJSONString(user));
    }


}
