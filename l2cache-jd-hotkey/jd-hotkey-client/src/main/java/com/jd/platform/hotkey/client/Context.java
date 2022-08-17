package com.jd.platform.hotkey.client;

/**
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class Context {
    public static String APP_NAME;

    /**
     * 与worker连接断开后，是否需要重连
     */
    public static boolean NEED_RECONNECT = true;

    public static int CAFFEINE_SIZE;
}
