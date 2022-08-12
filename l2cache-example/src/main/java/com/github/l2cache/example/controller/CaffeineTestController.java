package com.github.l2cache.example.controller;

import com.github.l2cache.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2020/10/10 11:40
 */
@Slf4j
@RestController
public class CaffeineTestController {

    @RequestMapping(value = "/refreshAfterWriteTest")
    public String refreshAfterWriteTest() throws InterruptedException {
        LoadingCache<String, String> valueLoaderCache = Caffeine.newBuilder()
                .initialCapacity(32)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(10000)
//                .softValues()
                .build(key -> {
                    Thread.sleep(3000);
                    String value = RandomUtil.getUUID();
                    log.info("key={},loadvalue={}", key, value);
                    return value;
                });
        printForkJoinPool();

        log.info("size={}", valueLoaderCache.estimatedSize());
        for (int i = 0; i < 100000; i++) {
            valueLoaderCache.put("key" + i, "valuevvvvv" + i);
        }
        log.info("size={}", valueLoaderCache.estimatedSize());

        printForkJoinPool();

        int index = 0;
        while (valueLoaderCache.asMap().size() > 0) {
            log.info("size={}, index={}", valueLoaderCache.estimatedSize(), index);
            for (String key : valueLoaderCache.asMap().keySet()) {
                valueLoaderCache.get(key);
            }
            log.info("size={}, index={}", valueLoaderCache.estimatedSize(), index);
            if (index % 10 == 0) {
                printForkJoinPool();
            }
            Thread.sleep(1000);
            index++;
        }
        return "处理中";
    }

    private void printForkJoinPool() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        System.out.println();
        log.info("pool={}", pool.toString());
        log.info("Parallelism={}", pool.getParallelism());
        log.info("PoolSize={}", pool.getPoolSize());
        log.info("RunningThreadCount={}", pool.getRunningThreadCount());
        log.info("ActiveThreadCount={}", pool.getActiveThreadCount());
        log.info("QueuedTaskCount={}", pool.getQueuedTaskCount());
        System.out.println();
    }
}
