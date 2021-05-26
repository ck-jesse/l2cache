package com.coy.l2cache.util;

import com.coy.l2cache.consts.CacheConsts;
import org.slf4j.Logger;

/**
 * @author chenck
 * @date 2021/5/26 11:21
 */
public class LogUtil {

    /**
     * 自定义日志打印的级别，用于控制打印的内容的多少
     * <p>
     * 注：生产的日志级别一般为info，所以将 batchGetLogLevel 配置为 debug，则不会打印日志，在高性能要求下，可有效减少日志量，提升性能
     *
     * @param
     * @author chenck
     * @date 2021/5/26 11:22
     */
    public static void log(Logger logger, String batchGetLogLevel, String logFormat, Object... logParams) {
        if (CacheConsts.LOG_DEBUG.equalsIgnoreCase(batchGetLogLevel)) {
            if (logger.isDebugEnabled()) {
                logger.debug(logFormat, logParams);
            }
        } else if (CacheConsts.LOG_INFO.equalsIgnoreCase(batchGetLogLevel)) {
            if (logger.isInfoEnabled()) {
                logger.info(logFormat, logParams);
            }
        } else if (CacheConsts.LOG_WARN.equalsIgnoreCase(batchGetLogLevel)) {
            if (logger.isWarnEnabled()) {
                logger.warn(logFormat, logParams);
            }
        }
    }
}
