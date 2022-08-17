package com.jd.platform.hotkey.client.log;

import org.slf4j.LoggerFactory;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-21
 */
public class DefaultLogger implements HotKeyLogger {
    @Override
    public void debug(Class<?> className, String info) {
        LoggerFactory.getLogger(className).debug(info);
    }

    @Override
    public void info(Class<?> className, String info) {
        LoggerFactory.getLogger(className).info(info);
    }

    @Override
    public void error(Class<?> className, String info) {
        LoggerFactory.getLogger(className).error(info);
    }

    @Override
    public void warn(Class<?> className, String info) {
        LoggerFactory.getLogger(className).warn(info);
    }
}
