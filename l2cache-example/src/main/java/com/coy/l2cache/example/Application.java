package com.coy.l2cache.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基于 l2cache-spring-boot-starter 的demo
 */
@SpringBootApplication
public class Application {

    /**
     * -Xmx256M -Xms256M -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:F:/temp/jvm/gc_%t.log -XX:+UseConcMarkSweepGC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=F:/temp/jvm/heap_%t.hprof
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
