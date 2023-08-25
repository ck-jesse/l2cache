package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.pool.CustomForkJoinWorkerThreadFactory;
import com.github.jesse.l2cache.util.pool.LimitedThreadForkJoinWorkerThreadFactory;
import org.junit.Test;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * ForkJoinPool 自定义线程名称Test
 *
 * @author chenck
 * @date 2023/8/23 23:55
 */
public class CustomForkJoinWorkerThreadFactoryTest {
    // 线程池并行度（线程数）
    public static final int parallelism = 5;
    public static final int maxThreads = 10;
    public static final String threadNamePrefix = "test";

    /**
     * 验证 CustomForkJoinWorkerThreadFactory
     * 结果：
     * 1、通过Custom自定义线程工程中创建线程，线程池中最大线程数为5，等同于使用默认的DefaultForkJoinWorkerThreadFactory）
     * 2、在线程执行完任务，且没有窃取到其他任务时，会执行 onTermination()
     */
    @Test
    public void testCustomForkJoinWorkerThreadFactory() throws InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(parallelism, new CustomForkJoinWorkerThreadFactory(threadNamePrefix), null, false);
        loopTest(pool);
    }

    /**
     * 验证 LimitedThreadForkJoinWorkerThreadFactory
     * 结果：
     * 1、通过Limited自定义线程工程中创建线程，线程池中最大线程数为5，线程数不会达到maxThreads（等同于使用默认的DefaultForkJoinWorkerThreadFactory）
     * 2、在线程执行完任务，且没有窃取到其他任务时，会执行 onTermination()
     */
    @Test
    public void testLimitedThreadForkJoinWorkerThreadFactory() throws InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(parallelism, new LimitedThreadForkJoinWorkerThreadFactory(maxThreads, threadNamePrefix), null, false);
        loopTest(pool);
    }

    public void loopTest(ForkJoinPool pool) throws InterruptedException {
        // 模拟执行业务逻辑
        submitTaskToPool(pool, 0);

        int execNum = 0;
        while (true) {
            execNum++;
            Thread.sleep(1000);
            System.out.println("execNum=" + execNum + " , " + threadDateTimeInfo() + ", pool=" + pool);

            if (execNum % 30 == 0) {
                System.out.println("execNum=" + execNum);
                // 模拟执行业务逻辑
                submitTaskToPool(pool, execNum);
                Thread.sleep(30000);
            }
        }
    }

    /**
     * 模拟提交任务到ForkJoinPool
     */
    public void submitTaskToPool(ForkJoinPool pool, int execNum) {
        String key = "key_" + execNum + "_";
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            // 构建任务并提交到线程池
            pool.execute(new RecursiveTask<Object>() {
                @Override
                protected Object compute() {
                    try {
                        String result = key + "" + finalI;
                        //System.out.println(threadDateTimeInfo() + ", 休眠1s, result=" + result);
                        Thread.sleep(1000);// 模拟IO阻塞任务

                        System.out.println(threadDateTimeInfo() + ", 休眠1s, result=" + result + ", pool=" + pool);
                        setRawResult(result);
                        return getRawResult();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public String threadDateTimeInfo() {
        return DateTimeFormatter.ISO_TIME.format(LocalTime.now()) + " " + Thread.currentThread().getName() + " ";
    }
}
