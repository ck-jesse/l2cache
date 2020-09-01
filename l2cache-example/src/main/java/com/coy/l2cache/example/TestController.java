package com.coy.l2cache.example;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenck
 * @date 2020/9/1 9:54
 */
@Slf4j
@RestController
public class TestController {

    @Autowired
    RedissonClient redissonClient;


    @RequestMapping(value = "/evictUserSync")
    public String evictUserSync(String userId) {
        return null;
    }

}
