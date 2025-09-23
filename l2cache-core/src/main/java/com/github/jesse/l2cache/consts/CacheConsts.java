package com.github.jesse.l2cache.consts;

/**
 * @author chenck
 * @date 2020/5/18 10:04
 */
public class CacheConsts {

    /**
     * 缓存操作类型 refresh 刷新缓存，clear 清理缓存
     */
    public static final String CACHE_REFRESH = "refresh"; // 建议：改为清除缓存，下次访问时会重新加载，以简化缓存同步操作
    public static final String CACHE_REFRESH_CLEAR = "refresh_clear"; // 用于缓存同步时，采用删除操作，以简化缓存同步操作
    public static final String CACHE_CLEAR = "clear";
    public static final String CACHE_HOTKEY = "hotkey";
    public static final String CACHE_HOTKEY_EVIT = "hotkey_evit";

    /**
     * 分隔符
     */
    public static final String SPLIT = ":";
    public static final String SPLIT_UNDERLINE = "_";
    /**
     * 通配符 *
     */
    public static final String ASTERISK = "*";

    /**
     * 日志级别
     */
    public static final String LOG_DEBUG = "debug";
    public static final String LOG_INFO = "info";
    public static final String LOG_WARN = "warn";

    // 链路跟踪的字段名
    public static final String SID = "sid";
    public static final String TRACE_ID = "trace_id";
    public static final String PREFIX_CACHE_MSG = "MSG";
    /**
     * 清理 NullValue 的Task的trace_id的前缀
     */
    public static final String PREFIX_CLEAR_NULL_VALUE = "CLEAN_NULL";


    /**
     * 是否打印详细日志
     */
    public static final String PRINT_DETAIL_LOG = "on";
    public static final String NOT_PRINT_DETAIL_LOG = "off";
}
