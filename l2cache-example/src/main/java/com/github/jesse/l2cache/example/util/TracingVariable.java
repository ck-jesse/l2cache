package com.github.jesse.l2cache.example.util;

/**
 * @author chenck
 * @date 2023/5/5 15:39
 */
public class TracingVariable {

    public static final String TRACE_ID = "trace_id";// 分布式应用链路追踪id
    /**
     * 调用层级id，可通过 opentracing 实现SPAN。
     * SPAN 表示分布式调用链条中的一个调用单元
     */
    public static final String SPAN_ID = "span_id";// 调用层级id
    public static final String LOCAL_IP = "local_ip";
}
