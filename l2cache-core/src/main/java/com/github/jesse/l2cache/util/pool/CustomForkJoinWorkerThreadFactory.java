package com.github.jesse.l2cache.util.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义 CustomForkJoinWorkerThreadFactory
 * <p>
 * 1、支持自定义线程名字
 *
 * @author chenck
 * @date 2023/8/23 23:41
 */
public class CustomForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    protected static Logger logger = LoggerFactory.getLogger(CustomForkJoinWorkerThreadFactory.class);


    /**
     * 线程名称前缀
     */
    private String threadNamePrefix;


    /**
     * 线程编号
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public CustomForkJoinWorkerThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        int threadNum = threadNumber.incrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.debug("create thread, parallelism={}, poolSize={}, runningThreadCount={}, activeThreadCount={}, threadNum={}", pool.getParallelism(), pool.getPoolSize(), pool.getRunningThreadCount(), pool.getActiveThreadCount(), threadNum);
        }

        if (null == threadNamePrefix || "".equals(threadNamePrefix.trim())) {
            return new CustomForkJoinWorkerThread(pool);
        } else {
            // 使用自定义线程名称
            return new CustomForkJoinWorkerThread(pool, threadNamePrefix + "-worker-" + threadNum);
        }
    }

}
