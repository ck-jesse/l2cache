package com.coy.l2cache.util.pool;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Runnable 包装 MDC
 *
 * @author chenck
 * @date 2020/9/23 19:37
 */
public class RunnableMdcWarpper implements Runnable {

    private static final long serialVersionUID = 1L;

    Runnable runnable;
    Map<String, String> contextMap;
    Object param;

    public RunnableMdcWarpper(Runnable runnable) {
        this.runnable = runnable;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    public RunnableMdcWarpper(Runnable runnable, Object param) {
        this.runnable = runnable;
        this.contextMap = MDC.getCopyOfContextMap();
        this.param = param;
    }

    @Override
    public void run() {
        Map<String, String> oldContext = MdcUtil.beforeExecution(contextMap);
        try {
            runnable.run();
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }

    public Object getParam() {
        return param;
    }
}
