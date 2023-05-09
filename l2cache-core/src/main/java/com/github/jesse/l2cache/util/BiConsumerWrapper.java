package com.github.jesse.l2cache.util;

import com.github.jesse.l2cache.util.pool.MdcUtil;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author chenck
 * @date 2023/5/9 14:38
 */
public class BiConsumerWrapper implements BiConsumer<Object, Throwable> {

    BiConsumer<Object, Throwable> action;
    Map<String, String> contextMap;

    public BiConsumerWrapper(BiConsumer<Object, Throwable> action) {
        this.action = action;
        this.contextMap = MDC.getCopyOfContextMap();
    }

    @Override
    public void accept(Object object, Throwable throwable) {
        Map<String, String> oldContext = MdcUtil.beforeExecution(contextMap);
        try {
            action.accept(object, throwable);
        } finally {
            MdcUtil.afterExecution(oldContext);
        }
    }
}
