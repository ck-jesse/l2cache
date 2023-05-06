package com.github.jesse.l2cache.util.pool;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * 自定义ForkJoinWorkerThread，用于限制ForkJoinPool中创建的最大线程数
 *
 * @author chenck
 * @date 2023/5/6 13:49
 */
public class LimitedThreadForkJoinWorkerThread extends ForkJoinWorkerThread {
    protected LimitedThreadForkJoinWorkerThread(ForkJoinPool pool) {
        super(pool);
        setPriority(Thread.NORM_PRIORITY); // 设置线程优先级
        setDaemon(false); // 设置是否为守护线程
    }

}
