package com.coy.l2cache.consts;

/**
 * @author chenck
 * @date 2020/5/18 10:04
 */
public class CacheConsts {

    /**
     * 缓存操作类型 refresh 刷新缓存，clear 清理缓存
     */
    public static final String CACHE_REFRESH = "refresh";
    public static final String CACHE_CLEAR = "clear";

    /**
     * 分隔符
     */
    public static final String SPLIT = ":";

    /**
     * 日志级别
     */
    public static final String LOG_DEBUG = "debug";
    public static final String LOG_INFO = "info";
    public static final String LOG_WARN = "warn";

    // 链路跟踪的字段名
    public static final String SID = "sid";
    public static final String TRACE_ID = "trace_id";
    public static final String PREFIX = "CACHE_MSG";


    /**
     * 是否打印详细日志
     */
    public static final String PRINT_DETAIL_LOG ="on";
    public static final String NOT_PRINT_DETAIL_LOG ="off";
}
