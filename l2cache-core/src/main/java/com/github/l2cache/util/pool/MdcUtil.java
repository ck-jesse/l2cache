package com.github.l2cache.util.pool;

import org.slf4j.MDC;

import java.util.Map;

/**
 * @author chenck
 * @date 2021/5/11 17:00
 */
public class MdcUtil {

    /**
     * Invoked before running a task.
     *
     * @param newMdcContext the new MDC context
     * @return the old MDC context
     */
    public static Map<String, String> beforeExecution(Map<String, String> newMdcContext) {
        Map<String, String> oldMdcContext = MDC.getCopyOfContextMap();
        if (newMdcContext == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(newMdcContext);
        }
        return oldMdcContext;
    }

    /**
     * Invoked after running a task.
     *
     * @param oldMdcContext the old MDC context
     */
    public static void afterExecution(Map<String, String> oldMdcContext) {
        if (oldMdcContext == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(oldMdcContext);
        }
    }
}
