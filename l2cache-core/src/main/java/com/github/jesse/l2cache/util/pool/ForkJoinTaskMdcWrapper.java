package com.github.jesse.l2cache.util.pool;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author chenck
 * @date 2021/5/11 16:56
 * @see https://stackoverflow.com/questions/36026402/how-to-use-mdc-with-forkjoinpool
 */
public class ForkJoinTaskMdcWrapper<T> extends ForkJoinTask<T> {

    private static final long serialVersionUID = 1L;

    /**
     * If non-null, overrides the value returned by the underlying task.
     */
    private final AtomicReference<T> override = new AtomicReference<>();

    private ForkJoinTask<T> task;
    private Map<String, String> newContext;

    public ForkJoinTaskMdcWrapper(ForkJoinTask<T> task) {
        this.task = task;
        this.newContext = MDC.getCopyOfContextMap();
    }

    @Override
    public T getRawResult() {
        T result = override.get();
        if (result != null) {
            return result;
        }
        return task.getRawResult();
    }

    @Override
    protected void setRawResult(T value) {
        override.set(value);
    }

    @Override
    protected boolean exec() {
        Map<String, String> oldContext = MdcUtil.beforeExecution(newContext);
        try {
            task.invoke();
            return true;
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }
}
