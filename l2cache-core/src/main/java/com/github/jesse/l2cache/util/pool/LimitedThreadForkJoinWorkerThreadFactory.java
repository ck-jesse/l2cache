package com.github.jesse.l2cache.util.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义ForkJoinWorkerThreadFactory，用于限制ForkJoinPool中创建的最大线程数，并复用当前的ForkJoinPool的线程
 *
 * @author chenck
 * @date 2023/5/6 13:48
 */
public class LimitedThreadForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    protected static Logger logger = LoggerFactory.getLogger(LimitedThreadForkJoinWorkerThreadFactory.class);

    /**
     * 最大线程数
     */
    private final int maxThreads;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix;

    /**
     * 当前线程数
     */
    private final AtomicInteger threadCount = new AtomicInteger(0);

    public LimitedThreadForkJoinWorkerThreadFactory(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public LimitedThreadForkJoinWorkerThreadFactory(int maxThreads, String threadNamePrefix) {
        this.maxThreads = maxThreads;
        this.threadNamePrefix = threadNamePrefix;
    }

    /**
     * 限制了线程数量并复用当前的ForkJoinPool的线程
     */
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        int count = threadCount.incrementAndGet();

        // 如果当前线程数量小于等于最大线程数，则创建新线程，并将threadCount+1
        if (count <= maxThreads) {
            if (null == threadNamePrefix || "".equals(threadNamePrefix.trim())) {
                return new LimitedThreadForkJoinWorkerThread(pool);
            } else {
                // 使用自定义线程名称
                return new LimitedThreadForkJoinWorkerThread(pool, threadNamePrefix + "-worker-" + count);
            }
        }

        // 如果当前线程数量超过最大线程数，则不创建新线程，并将threadCount-1
        threadCount.decrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.debug("Exceeded maximum number of threads");
        }
        return null;// 不创建新线程
    }

}
