package com.github.jesse.l2cache.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于 l2cache-spring-boot-starter 的demo
 */
@SpringBootApplication
public class Application80 {

    /**
     * -Xmx256M -Xms256M -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:F:/temp/jvm/gc_%t.log -XX:+UseConcMarkSweepGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=F:/temp/jvm/heap_%t.hprof
     */
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "80");// 基于端口维度，指定启用的配置文件
        SpringApplication.run(Application80.class, args);
    }

}
