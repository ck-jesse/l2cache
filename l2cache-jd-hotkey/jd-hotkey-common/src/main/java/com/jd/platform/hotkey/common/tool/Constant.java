package com.jd.platform.hotkey.common.tool;

/**
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class Constant {
    public static String PING = "ping";
    public static String PONG = "pong";

    /**
     * 该值在ProducerFactory有修改
     */
    public static int Default_Threads = 4;

    public static int MAGIC_NUMBER = 0x12fcf76;
    /**
     * netty的分隔符
     */
    public static String DELIMITER = "$(* *)$";
    /**
     * 数量统计时，rule+时间 组成key用的分隔符
     */
    public static String COUNT_DELIMITER = "#**#";

    public static String BAK_DELIMITER = "#\\*\\*#";

    /**
     * 当客户端要删除某个key时，就往etcd里赋值这个value，设置1秒过期，就算删除了
     */
    public static String DEFAULT_DELETE_VALUE = "#[DELETE]#";

    //单次包最大2M
    public static int MAX_LENGTH = 2 * 1024 * 1024;

}
