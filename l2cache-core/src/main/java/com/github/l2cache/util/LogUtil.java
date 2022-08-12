package com.github.l2cache.util;

import com.github.l2cache.consts.CacheConsts;
import org.slf4j.Logger;

/**
 * @author chenck
 * @date 2021/5/26 11:21
 */
public class LogUtil {

    /**
     * 自定义日志打印的级别，用于控制打印的内容的多少
     * <p>
     * 注：生产的日志级别一般为info，所以将 logLevel 配置为 debug，则不会打印日志，在高性能要求下，可有效减少日志量，提升性能
     *
     * @param
     * @author chenck
     * @date 2021/5/26 11:22
     */
    public static void log(Logger logger, String logLevel, String logFormat, Object... logParams) {
        if (CacheConsts.LOG_DEBUG.equalsIgnoreCase(logLevel)) {
            if (logger.isDebugEnabled()) {
                logger.debug(logFormat, logParams);
            }
        } else if (CacheConsts.LOG_INFO.equalsIgnoreCase(logLevel)) {
            if (logger.isInfoEnabled()) {
                logger.info(logFormat, logParams);
            }
        } else if (CacheConsts.LOG_WARN.equalsIgnoreCase(logLevel)) {
            if (logger.isWarnEnabled()) {
                logger.warn(logFormat, logParams);
            }
        }
    }

    /**
     * 打印详情日志信息
     * @param logger 日志对象
     * @param printDetailLogSwitch 日志详情开关
     * @param logFormat 日志信息
     * @param logParams 日志值
     */
    public static void logDetailPrint(Logger logger, String printDetailLogSwitch, String logFormat, Object... logParams) {
        if (CacheConsts.PRINT_DETAIL_LOG.equalsIgnoreCase(printDetailLogSwitch)) {
            logger.info(logFormat, logParams);
        }
    }

    /**
     * 打印简单日志信息
     * @param logger 日志对象
     * @param printDetailLogSwitch 日志详情开关
     * @param logFormat 日志信息
     * @param logParams 日志值
     */
    public static void logSimplePrint(Logger logger, String printDetailLogSwitch, String logFormat, Object... logParams) {
        if (CacheConsts.NOT_PRINT_DETAIL_LOG.equalsIgnoreCase(printDetailLogSwitch)) {
            logger.info(logFormat, logParams);
        }
    }
}
