package com.coy.l2cache.test;

import com.coy.l2cache.EnableCaffeineRedisCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通过 Spring Enable 注解模式来启用二级缓存组件
 */
@EnableCaffeineRedisCache
@SpringBootApplication
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

}
