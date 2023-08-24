package com.github.jesse.l2cache.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于 l2cache-spring-boot-starter 的demo
 */
@SpringBootApplication
public class Application81 {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "81");// 基于端口维度，指定启用的配置文件
        SpringApplication.run(Application81.class, args);
    }

}
