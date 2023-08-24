package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.pool.LimitedThreadForkJoinWorkerThreadFactory;
import com.github.jesse.l2cache.util.pool.MyManagedBlocker;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * http://www.codexy.cn/manual/javaapi9/java/util/concurrent/ForkJoinPool.html
 * <p>
 * 静态公共池commonPool()可适用于大多数应用，任何未显式提交到指定ForkJoinPool的ForkJoinTask均使用公共池。
 * 对于部分应用程序，可以使用指定的并行数构建一个自定义ForkJoinPool，默认情况下，线程数等于可用处理器的数量。
 * 该自定义ForkJoinPool会尝试通过动态添加，挂起或恢复内部工作线程来维护足够的活动（或可用）线程。
 * 但是，当面对IO阻塞型任务时，若出现阻塞，则不能保证这样的调整会有足够的可用线程。
 * 此时，可通过ForkJoinPool.ManagedBlocker接口来扩展足够的可用线程，来保证并行性。
 *
 * @author chenck
 * @date 2023/5/5 18:44
 */
public class ManagedBlockerTest {

    public static void main(String[] args) throws InterruptedException {
        // 线程池并行度（线程数）
        int parallelism = 5;
        // 最大线程数
        int maxThreads = 10;
        // 创建自定义的ForkJoinPool
        // 在管理阻塞时，通过自定义ForkJoinWorkerThreadFactory来限制最大可创建的线程数，避免无限制的创建线程
        // 适用于面对IO阻塞型任务时，通过扩展线程池中的线程数，来提高执行效率的场景
        ForkJoinPool pool = new ForkJoinPool(parallelism, new LimitedThreadForkJoinWorkerThreadFactory(maxThreads), null, false);
        test(pool, 0);

        int execNum = 0;
        while (true) {
            execNum++;
            Thread.sleep(1000);
            System.out.println("execNum=" + execNum + " , " + threadDateTimeInfo() + ", pool=" + pool.toString());

            if (execNum % 30 == 0) {
                System.out.println("execNum=" + execNum);
                test(pool, execNum);
                Thread.sleep(30000);
            }
        }
    }

    static void test(ForkJoinPool pool, int execNum) {

        String key = "key_" + execNum + "_";
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            // 构建任务并提交到线程池
            pool.execute(new RecursiveTask<Object>() {
                @Override
                protected Object compute() {
                    try {
                        MyManagedBlocker myManagedBlocker = new MyManagedBlocker(key + "" + finalI, key -> {
                            System.out.println(threadDateTimeInfo() + ", 休眠2s, result=" + key);
                            try {
                                Thread.sleep(2000);// 模拟IO阻塞任务
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            return key;
                        });


                        // 运行指定的阻塞任务。当ForkJoinTask 在 ForkJoinPool 中运行时，此方法可能会在必要时创建备用线程，以确保当前线程在 ManagedBlockerblock.block() 中阻塞时有足够的并行性。
                        ForkJoinPool.managedBlock(myManagedBlocker);

                        System.out.println(threadDateTimeInfo() + ", 休眠2s, result=" + myManagedBlocker.getResult() + ", pool=" + pool);
                        setRawResult(myManagedBlocker.getResult());
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
