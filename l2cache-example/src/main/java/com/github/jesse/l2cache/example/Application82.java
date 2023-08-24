package com.github.jesse.l2cache.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于 l2cache-spring-boot-starter 的demo
 */
@SpringBootApplication
public class Application82 {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "82");// 基于端口维度，指定启用的配置文件
        SpringApplication.run(Application82.class, args);
    }

}
