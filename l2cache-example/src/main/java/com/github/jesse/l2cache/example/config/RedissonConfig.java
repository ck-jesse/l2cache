package com.github.jesse.l2cache.example.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 *
 * @author chenck
 * @date 2020/9/2 14:32
 */
@Slf4j
@Configuration
public class RedissonConfig {


//    @Bean
//    public RedissonClient redissonClient2()  {
//        RedissonClient redissonClient = Redisson.create();
//        return redissonClient;
//    }
//    @Bean
//    public RedissonClient redissonClient()  {
//        RedissonClient redissonClient = Redisson.create();
//        return redissonClient;
//    }
}
