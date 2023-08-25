package com.github.jesse.l2cache.util.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * 自定义ForkJoinWorkerThread
 * 1、支持自定义线程名字
 * 2、线程终止时，执行threadCount-1，目的是为了保证可以创建线程
 * <p>
 * TODO 暂未找到方法对ForkJoinPool的线程回收进行精确控制，因此废弃该类
 *
 * @author chenck
 * @date 2023/8/24 13:59
 */
@Deprecated
public class LimitedThreadForkJoinWorkerThread extends ForkJoinWorkerThread {

    protected static Logger logger = LoggerFactory.getLogger(LimitedThreadForkJoinWorkerThread.class);

    private LimitedThreadForkJoinWorkerThreadFactory factory;

    protected LimitedThreadForkJoinWorkerThread(ForkJoinPool pool, String threadName, LimitedThreadForkJoinWorkerThreadFactory factory) {
        super(pool);
        setPriority(Thread.NORM_PRIORITY); // 设置线程优先级
        setDaemon(false); // 设置是否为守护线程
        setName(threadName);
        this.factory = factory;
    }

    /**
     * 线程终止时，执行的清理动作
     * 【场景1：不结合ManagedBlocker的情况下】
     * 结论：在线程执行完任务，且没有窃取到其他任务时，会执行 onTermination()
     * 案例：CustomForkJoinWorkerThreadFactoryTest
     *
     * 【场景2：结合ManagedBlocker的情况下】
     * 案例：ManagedBlockerTest
     * 分析：
     * 1、当创建的线程序号达到一定数量时（如：20个），任务执行完后，线程一直处于WAIT状态，这就导致无可用线程，且一直不会执行到onTermination()方法，也就不会执行threadCount-1
     * 2、同时由于threadCount=maxThreads，导致不会创建新线程，最终出现业务逻辑不被执行的情况。
     */
    @Override
    protected void onTermination(Throwable exception) {
        super.onTermination(exception);
        int threadCount = factory.threadTerminated();
        if (logger.isDebugEnabled()) {
            logger.debug("Performs cleanup associated with termination of this worker thread, threadCount={}, pool={}", threadCount, this.getPool().toString());
        }
        System.out.println("termination of this worker thread, threadCount=" + threadCount + ", pool=" + this.getPool().toString());
    }
}
