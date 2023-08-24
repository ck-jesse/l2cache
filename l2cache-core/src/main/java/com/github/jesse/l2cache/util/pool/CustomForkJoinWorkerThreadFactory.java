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

    public CustomForkJoinWorkerThreadFactory() {
        this.threadNamePrefix = PoolConsts.DEFAULT_THREAD_NAME_PREFIX;
    }

    public CustomForkJoinWorkerThreadFactory(String threadNamePrefix) {
        if (null == threadNamePrefix || "".equals(threadNamePrefix.trim())) {
            this.threadNamePrefix = PoolConsts.DEFAULT_THREAD_NAME_PREFIX;
        } else {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        int threadNum = threadNumber.incrementAndGet();
        String newThreadName = getNewThreadName(threadNum);
        if (logger.isDebugEnabled()) {
            logger.debug("create thread, threadNum={}, newThreadName={}, pool={}", threadNumber.get(), newThreadName, pool.toString());
        }

        // 当线程编号大于等于最大线程编号时，将线程编号重置
        if (threadNum >= PoolConsts.MAX_THREAD_NUMBER) {
            threadNumber.compareAndSet(threadNum, 1);
        }

        // 使用自定义线程名称
        return new CustomForkJoinWorkerThread(pool, newThreadName);
    }

    /**
     * 获取新线程名称
     */
    String getNewThreadName(int threadNum) {
        return threadNamePrefix + "-worker-" + threadNum;
    }
}
