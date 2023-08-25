package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.util.pool.CustomForkJoinWorkerThreadFactory;
import com.github.jesse.l2cache.util.pool.LimitedThreadForkJoinWorkerThreadFactory;
import com.github.jesse.l2cache.util.pool.MyManagedBlocker;
import com.sun.xml.internal.bind.v2.TODO;

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
    // 线程池并行度（线程数）
    public static final int parallelism = 5;
    public static final int maxThreads = 10;
    public static final String threadNamePrefix = "test";

    public static void main(String[] args) throws InterruptedException {
        // 创建自定义的ForkJoinPool
        // 在管理阻塞时，通过自定义ForkJoinWorkerThreadFactory来限制最大可创建的线程数，避免无限制的创建线程
        // 适用于面对IO阻塞型任务时，通过扩展线程池中的线程数，来提高执行效率的场景
        // TODO 验证未通过：使用ManagedBlocker后，可以扩展的备用线程，但运行一段时间后，线程会出现阻塞的情况，导致无线程可用，所以未验证通过
        ForkJoinPool pool = new ForkJoinPool(parallelism, new LimitedThreadForkJoinWorkerThreadFactory(maxThreads, threadNamePrefix), null, false);

        // 不限制最大可创建的线程数，可能会OOM
        // ForkJoinPool pool = new ForkJoinPool(parallelism, new CustomForkJoinWorkerThreadFactory(threadNamePrefix), null, false);

        loopTest(pool);
    }


    static void loopTest(ForkJoinPool pool) throws InterruptedException {
        // 模拟执行业务逻辑
        submitTaskToPool(pool, 0);

        int execNum = 0;
        while (true) {
            execNum++;
            Thread.sleep(1000);
            System.out.println("execNum=" + execNum + " , " + threadDateTimeInfo() + ", pool=" + pool.toString());

            if (execNum % 30 == 0) {
                System.out.println("execNum=" + execNum);
                submitTaskToPool(pool, execNum);
                Thread.sleep(10000);
            }
        }
    }

    /**
     * 模拟提交任务到ForkJoinPool
     */
    static void submitTaskToPool(ForkJoinPool pool, int execNum) {

        String key = "key_" + execNum + "_";
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            // 构建任务并提交到线程池
            pool.execute(new RecursiveTask<Object>() {
                @Override
                protected Object compute() {
                    try {
                        MyManagedBlocker blocker = new MyManagedBlocker(key + "" + finalI, key -> {
                            System.out.println(threadDateTimeInfo() + ", 休眠1s, result=" + key);
                            try {
                                Thread.sleep(1000);// 模拟IO阻塞任务
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            return key;
                        });

                        // MyManagedBlocker 让线程池知道当前任务即将阻塞，因此需要创建新的补偿工作线程来执行新的提交任务
                        // 运行指定的阻塞任务。当ForkJoinTask 在 ForkJoinPool 中运行时，此方法可能会在必要时创建备用线程，以确保当前线程在 ManagedBlockerblock.block() 中阻塞时有足够的并行性。
                        ForkJoinPool.managedBlock(blocker);

                        System.out.println(threadDateTimeInfo() + ", 休眠1s, result=" + blocker.getResult() + ", pool=" + pool);
                        setRawResult(blocker.getResult());
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
