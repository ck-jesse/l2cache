package com.github.jesse.l2cache.example.util;

/**
 * @author chenck
 * @date 2023/5/5 15:39
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 基于slf4j的MDC的日志跟踪
 *
 */
public class MDCLogTracerContextUtil {
    private static final Logger logger = LoggerFactory.getLogger(MDCLogTracerContextUtil.class);

    static String localIp;

    public static void attachTraceId(String traceId) {
        if (traceId == null) {
            logger.error("traceId can not be null");
            return;
        }

        mdcPut(TracingVariable.TRACE_ID, traceId);
        mdcPut(TracingVariable.LOCAL_IP, localIp);
    }

    private static void mdcPut(String key, String val) {
        MDC.put(key, val);
    }

    private static void mdcRemove(String key) {
        MDC.remove(key);
    }

    private static String mdcGet(String key) {
        return MDC.get(key);
    }

    public static void removeTraceId() {
        mdcRemove(TracingVariable.TRACE_ID);
    }

    public static String getTraceId() {
        return mdcGet(TracingVariable.TRACE_ID);
    }

    public static void main(String[] args) {
        attachTraceId("xx");
    }

    static {
        localIp = "";
        try {
            localIp = NetUtils.getLocalHostIP();
        } catch (Exception e) {
            logger.error("fetch local ip error", e);
        }
    }

}

