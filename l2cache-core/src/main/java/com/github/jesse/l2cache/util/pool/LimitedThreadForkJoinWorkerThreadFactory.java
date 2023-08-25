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
 * <p>
 * TODO 暂未找到方法对ForkJoinPool的线程回收进行精确控制，因此废弃该类
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
     * 线程编号，用于给线程命名
     */
    private final AtomicInteger threadNumber = new AtomicInteger(0);

    /**
     * 线程数量，用于控制创建线程的数量
     */
    private final AtomicInteger threadCount = new AtomicInteger(0);

    public LimitedThreadForkJoinWorkerThreadFactory(int maxThreads) {
        this.maxThreads = maxThreads;
        this.threadNamePrefix = PoolConsts.DEFAULT_THREAD_NAME_PREFIX;
    }

    public LimitedThreadForkJoinWorkerThreadFactory(int maxThreads, String threadNamePrefix) {
        this.maxThreads = maxThreads;
        if (null == threadNamePrefix || "".equals(threadNamePrefix.trim())) {
            this.threadNamePrefix = PoolConsts.DEFAULT_THREAD_NAME_PREFIX;
        } else {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    /**
     * 限制了线程数量并复用当前的ForkJoinPool的线程
     * <p>
     * 问题：创建新线程时，将threadCount+1，而ForkJoinPool中的线程在空闲后会被回收掉，但在被回收时，没有执行threadCount-1。所以当threadCount>=maxThreads时，一直不会去创建新的线程，也就导致不会去执行具体的业务操作
     * 方案：要解决该问题，有两个方向，如下
     * 方案1、从ForkJoinPool中获取当前线程数或者活跃线程数，但验证下来，发现无法从ForkJoinPool中获取准确的线程数，所以还是控制不住创建的线程数，要不创建多了，要么还是不去创建（不可行）
     * 方案2、在线程被回收时，执行threadCount-1，这样就可以控制住了。通过分析发现回收线程的扩展点：java.util.concurrent.ForkJoinWorkerThread.onTermination，因此扩展该方法即可。（不可行）
     * 建议：方案2，但方案2未验证通过，因为onTermination()在线程被回收时并不会被执行。
     * <p>
     * 注意：通过ForkJoinPool中 getPoolSize(), getRunningThreadCount(), getActiveThreadCount() 获取到的线程数都不准确，不能用于控制线程数
     * getPoolSize()，某些情况下，poolSize一直不减少，导致不会创建线程，不会继续执行业务代码。（结果是希望创建线程时，不去创建线程）
     * getRunningThreadCount()，某些情况下，poolSize会减少，导致创建的线程数，超过maxThread。（结果是希望不创建线程时，却创建了线程）
     * 这是一个并发问题，并发执行到这里，获取getRunningThreadCount()的值一样，所以创建了多个线程。
     */
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {

        // 如果当前线程数量小于等于最大线程数，则创建新线程，并将threadCount+1
        int count = threadCount.incrementAndGet();

        if (count <= maxThreads) {
            int threadNum = threadNumber.incrementAndGet();
            String newThreadName = getNewThreadName(threadNum);
            if (logger.isDebugEnabled()) {
                logger.debug("create thread, threadCount={}, maxThreads={}, newThreadName={}, pool={}", threadCount.get(), maxThreads, newThreadName, pool.toString());
            }
            System.out.println("create thread, threadCount=" + threadCount + ", maxThreads=" + maxThreads + ", newThreadName=" + newThreadName + " , pool=" + pool.toString());

            // 当线程编号大于等于最大线程编号时，将线程编号重置
            if (threadNum >= PoolConsts.MAX_THREAD_NUMBER) {
                threadNumber.compareAndSet(threadNum, 0);
            }

            // 使用自定义线程名称
            return new LimitedThreadForkJoinWorkerThread(pool, newThreadName, this);
        }

        // 如果当前线程数量超过最大线程数，则不创建新线程，并将threadCount-1
        threadCount.decrementAndGet();
        if (logger.isDebugEnabled()) {
            logger.debug("Exceeded maximum number of threads, threadCount={}, maxThreads={}, threadNum={}, pool={}", threadCount.get(), maxThreads, threadNumber.get(), pool.toString());
        }
        System.out.println("Exceeded maximum number of threads, threadName=" + Thread.currentThread().getName() + ", threadCount=" + threadCount.get() + ", maxThreads=" + maxThreads + ", threadNum=" + threadNumber.get() + " , pool=" + pool.toString());
        return null;
    }

    /**
     * 获取新线程名称
     */
    String getNewThreadName(int threadNum) {
        return threadNamePrefix + "-limit-worker-" + threadNum;
    }

    /**
     * 线程终止时，执行threadCount-1，目的是为了保证可以创建线程
     */
    int threadTerminated() {
        return threadCount.decrementAndGet();
    }
}
