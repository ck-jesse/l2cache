package com.coy.l2cache.jd.hotkey.client.log;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-21
 */
public interface HotKeyLogger {
    void debug(Class<?> className, String info);

    void info(Class<?> className, String info);

    void error(Class<?> className, String info);

    void warn(Class<?> className, String info);
}
