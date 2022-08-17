package com.jd.platform.hotkey.client.log;

/**
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class JdLogger {
    private static HotKeyLogger logger = new DefaultLogger();

    public static void setLogger(HotKeyLogger log) {
        if (log != null) {
            logger = log;
        }
    }

    public static void debug(Class className, String info) {
        logger.debug(className, info);
    }

    public static void info(Class className, String info) {
        logger.info(className, info);
    }

    public static void warn(Class className, String info) {
        logger.warn(className, info);
    }

    public static void error(Class className, String info) {
        logger.error(className, info);
    }

}
