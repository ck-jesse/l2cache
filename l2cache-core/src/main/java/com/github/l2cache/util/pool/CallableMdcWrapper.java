package com.github.l2cache.util.pool;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author chenck
 * @date 2021/5/11 17:09
 */
public class CallableMdcWrapper<T> implements Callable<T> {

    private static final long serialVersionUID = 1L;

    Callable<T> callable;
    Map<String, String> contextMap;

    public CallableMdcWrapper(Callable<T> callable) {
        this.callable = callable;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    @Override
    public T call() throws Exception {
        Map<String, String> oldContext = MdcUtil.beforeExecution(contextMap);
        try {
            return callable.call();
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }
}
