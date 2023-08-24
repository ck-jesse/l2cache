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
     * 问：为什么自定义线程的onTermination()方法，不会在线程回收时被调用？
     * 答：onTermination() 方法仅在线程执行任务时抛出未捕获异常的情况下被调用，它并不是在线程被回收时执行的方法。
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
