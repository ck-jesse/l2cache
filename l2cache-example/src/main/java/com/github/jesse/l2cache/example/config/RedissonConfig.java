//package com.github.jesse.l2cache.example.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Redisson 配置
// *
// * @author chenck
// * @date 2020/9/2 14:32
// */
//@RefreshScope
//@Slf4j
//@Configuration
//public class RedissonConfig {
//
//    /**
//     * 特别注意，不同RedissonClient的codec不同时，会导致反序列化失败，请保持一致
//     */
//    @Bean
//    public RedissonClient redissonClient2() {
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//        config.setCodec(new org.redisson.codec.JsonJacksonCodec());
//
//        RedissonClient redissonClient = Redisson.create(config);
//        return redissonClient;
//    }
//
//    @Bean
//    public RedissonClient redissonClient3() {
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
//        config.setCodec(new org.redisson.codec.JsonJacksonCodec());
//
//        RedissonClient redissonClient = Redisson.create();
//        return redissonClient;
//    }
//}
