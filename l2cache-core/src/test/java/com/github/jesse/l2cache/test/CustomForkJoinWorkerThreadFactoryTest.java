package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.pool.CustomForkJoinWorkerThreadFactory;

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

    public static void main(String[] args) throws InterruptedException {
        // 线程池并行度（线程数）
        int parallelism = 5;

        // 验证结果：ForkJoinPool自定义线程名称后，最大线程数为5（等同于使用默认的DefaultForkJoinWorkerThreadFactory）
        ForkJoinPool pool = new ForkJoinPool(parallelism, new CustomForkJoinWorkerThreadFactory("test"), null, false);
        test1(pool, 0);

        int execNum = 0;
        while (true) {
            execNum++;
            Thread.sleep(1000);
            System.out.println("execNum=" + execNum + " , " + threadDateTimeInfo() + ", pool=" + pool.toString());

            if (execNum % 30 == 0) {
                System.out.println("execNum=" + execNum);
                test1(pool, execNum);
                Thread.sleep(30000);
            }
        }
    }

    static void test1(ForkJoinPool pool, int execNum) {

        String key = "key_" + execNum + "_";
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            // 构建任务并提交到线程池
            pool.execute(new RecursiveTask<Object>() {
                @Override
                protected Object compute() {
                    try {
                        String result = key + "" + finalI;
                        System.out.println(threadDateTimeInfo() + ", 休眠2s, result=" + result);
                        Thread.sleep(2000);// 模拟IO阻塞任务

                        System.out.println(threadDateTimeInfo() + ", 休眠2s, result=" + result + ", RunningThreadCount=" + pool.getRunningThreadCount() + ", ActiveThreadCount=" + pool.getActiveThreadCount() + ", PoolSize=" + pool.getPoolSize());
                        setRawResult(result);
                        return getRawResult();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    static String threadDateTimeInfo() {
        return DateTimeFormatter.ISO_TIME.format(LocalTime.now()) + " " + Thread.currentThread().getName() + " ";
    }
}
