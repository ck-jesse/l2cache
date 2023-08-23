package com.github.jesse.l2cache.util.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义ForkJoinWorkerThreadFactory
 * <p>
 * 1、支持自定义线程名字
 * 2、适用于面对IO阻塞型任务时，通过扩展线程池中的线程数，来提高执行效率的场景，配合ManagedBlocker使用
 * 注意：需通过LimitedThreadForkJoinWorkerThreadFactory，限制ForkJoinPool中创建的最大线程数，避免无限制的创建线程，导致OOM
 *
 * @author chenck
 * @date 2023/5/6 13:48
 */
@Deprecated
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
     * 线程编号
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);

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

        // TODO 该方法暂未验证通过，不可用于生产，需要深入研究
        // 不准确，poolSize会减少，这又是为啥？导致创建的线程数，超过maxThread。
        // 这是一个并发问题，并发执行到这里，获取getRunningThreadCount()的值一样，所以创建了多个线程。
        // int threadCount = pool.getRunningThreadCount();

        // 不准确，poolSize一直不减少，这是为啥？导致不会创建线程，不会继续执行业务代码。
        int threadCount = pool.getPoolSize();

        // 如果当前线程数量小于等于最大线程数，则创建新线程，并将threadCount+1
        // 创建新线程时，将threadCount+1，而ForkJoinPool中的线程在空闲后会被回收掉，但在回收掉时，没有执行threadCount-1，所以当threadCount=maxThreads时，一直不会去创建新的线程，也就导致不会去执行刷新缓存操作
        if (threadCount <= maxThreads) {
            int threadNum = threadNumber.incrementAndGet();
            if (logger.isDebugEnabled()) {
                logger.debug("create thread, parallelism={}, poolSize={}, runningThreadCount={}, activeThreadCount={}, threadNum={}", pool.getParallelism(), pool.getPoolSize(), pool.getRunningThreadCount(), pool.getActiveThreadCount(), threadNum);
            }
            System.out.println("create thread, pool=" + pool.toString());
            if (null == threadNamePrefix || "".equals(threadNamePrefix.trim())) {
                return new CustomForkJoinWorkerThread(pool);
            } else {
                // 使用自定义线程名称
                return new CustomForkJoinWorkerThread(pool, threadNamePrefix + "-worker-" + threadNum);
            }
        }

        // 如果当前线程数量超过最大线程数，则不创建新线程
        if (logger.isDebugEnabled()) {
            logger.debug("Exceeded maximum number of threads, parallelism={}, poolSize={}, runningThreadCount={}, activeThreadCount={}, threadNum={}", pool.getParallelism(), pool.getPoolSize(), pool.getRunningThreadCount(), pool.getActiveThreadCount(), threadNumber.get());
        }
        System.out.println("Exceeded maximum number of threads, pool=" + pool.toString());
        return null;// 不创建新线程
    }

}
