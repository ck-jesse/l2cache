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
     *
     * @param
     * @author chenck
     * @date 2021/5/26 11:22
     */
    public static void log(Logger logger, String batchGetLogLevel, String logFormat, Object... logParams) {
        if (CacheConsts.LOG_DEBUG.equalsIgnoreCase(batchGetLogLevel)) {
            logger.debug(logFormat, logParams);
        } else if (CacheConsts.LOG_INFO.equalsIgnoreCase(batchGetLogLevel)) {
            logger.info(logFormat, logParams);
        } else if (CacheConsts.LOG_WARN.equalsIgnoreCase(batchGetLogLevel)) {
            logger.warn(logFormat, logParams);
        }
    }
}
