package com.github.jesse.l2cache.util.pool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

/**
 * 自定义 {@link ForkJoinPool}，扩展MDC内容，以便链路追踪
 *
 * @author chenck
 * @date 2021/5/11 14:48
 */
public class MdcForkJoinPool extends ForkJoinPool {

    /**
     * Common (static) pool.
     */
    static final MdcForkJoinPool mdcCommon = new MdcForkJoinPool();

    public static MdcForkJoinPool mdcCommonPool() {
        return mdcCommon;
    }

    // constructor

    public MdcForkJoinPool() {
        super();
    }

    public MdcForkJoinPool(int parallelism) {
        super(parallelism);
    }

    /**
     * Creates a new MdcForkJoinPool.
     *
     * @param parallelism the parallelism level. For default value, use {@link java.lang.Runtime#availableProcessors}.
     * @param factory     the factory for creating new threads. For default value, use
     *                    {@link #defaultForkJoinWorkerThreadFactory}.
     * @param handler     the handler for internal worker threads that terminate due to unrecoverable errors encountered
     *                    while executing tasks. For default value, use {@code null}.
     * @param asyncMode   if true, establishes local first-in-first-out scheduling mode for forked tasks that are never
     *                    joined. This mode may be more appropriate than default locally stack-based mode in applications
     *                    in which worker threads only process event-style asynchronous tasks. For default value, use
     *                    {@code false}.
     */
    public MdcForkJoinPool(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode) {
        super(parallelism, factory, handler, asyncMode);
    }

    // Execution methods

    @Override
    public <T> T invoke(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return super.invoke(new ForkJoinTaskMdcWrapper<T>(task));
    }

    @Override
    public void execute(ForkJoinTask<?> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        super.execute(new ForkJoinTaskMdcWrapper<>(task));
    }

    // AbstractExecutorService methods

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        super.execute(new RunnableMdcWarpper(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return super.submit(new ForkJoinTaskMdcWrapper<T>(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return super.submit(new CallableMdcWrapper(task));
    }

    @Override
    public <T> ForkJoinTask<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        return super.submit(new RunnableMdcWarpper(task), result);
    }

    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        return super.submit(new RunnableMdcWarpper(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        if (tasks == null) {
            throw new NullPointerException();
        }
        Collection<Callable<T>> wrapperTasks = new ArrayList<>();
        for (Callable<T> task : tasks) {
            wrapperTasks.add(new CallableMdcWrapper(task));
        }

        return super.invokeAll(wrapperTasks);
    }

}
