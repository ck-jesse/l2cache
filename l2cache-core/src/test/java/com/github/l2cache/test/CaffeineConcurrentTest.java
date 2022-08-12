package com.github.l2cache.test;

import com.github.l2cache.util.DateUtils;
import com.github.l2cache.util.pool.MdcForkJoinPool;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author chenck
 * @date 2021/5/8 18:36
 */
public class CaffeineConcurrentTest {


    /**
     * refreshAfterWrite模式
     * 缓存已过期，且多线程并发获取同一个缓存key时，显现是怎样的？
     * 结论：所有线程均不阻塞且直接返回已过期数据，会触发一个ForkJoinPool的异步线程进行加载数据
     *
     * @param
     * @author chenck
     * @date 2021/5/8 18:38
     */
    @Test
    public void refreshAfterWriteConcurrent() throws InterruptedException {
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
                .refreshAfterWrite(3, TimeUnit.SECONDS)
                .maximumSize(5)
                .removalListener((key, value, cause) -> {
                    println("remove removalCause={}, key=" + key + ", value=" + value);
                })
                .executor(MdcForkJoinPool.mdcCommonPool())// 自定义ForkJoinPool
                .build(key -> {
                    // 走到此处，若异步，则表示进入到了ForkJoinPool的线程里面
                    Integer value = 100;
                    println("start load value = " + value);
                    Thread.sleep(2000);
                    println("end load value = " + value);
                    return value;
                });

        valid(cache);
    }

    /**
     * expireAfterWrite模式
     * 缓存已过期，且多线程并发获取同一个缓存key时，显现是怎样的？
     * 结论：只有一个线程加载数据，其他线程被阻塞，直到加载数据线程返回数据。
     *
     * @param
     * @author chenck
     * @date 2021/5/8 18:38
     */
    @Test
    public void expireAfterWriteConcurrent() throws InterruptedException {
        LoadingCache<Integer, Integer> cache = Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .maximumSize(5)
                .removalListener((key, value, cause) -> {
                    println("remove removalCause={}, key=" + key + ", value=" + value);
                })
                .build(key -> {
                    Integer value = 100;
                    println("start load value = " + value);
                    Thread.sleep(2000);
                    println("end load value = " + value);
                    return value;
                });

        valid(cache);
    }

    private void valid(LoadingCache<Integer, Integer> cache) throws InterruptedException {

        Integer key1 = 1;
        cache.put(key1, 1);

        println("get key = " + key1 + ", value =" + cache.get(key1));
        Thread.sleep(3000);// 模拟缓存过期

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                println("get key = " + key1 + ", value =" + cache.get(key1));
            }
        };

        // 模拟多线程
        new Thread(runnable, "thread1").start();
        new Thread(runnable, "thread2").start();
        new Thread(runnable, "thread3").start();

        while (true) {
        }
    }

    private static void println(String message) {
        System.out.println(DateUtils.format(new Date(), DateUtils.DATE_FORMAT_23) + " " + Thread.currentThread().getName() + " " + message);
    }
}
