package com.coy.l2cache.schedule;

import com.coy.l2cache.util.DaemonThreadFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * clear NullValue cache support
 *
 * @author chenck
 * @date 2020/10/9 16:24
 */
public class NullValueClearSupport {

    private volatile static ScheduledExecutorService scheduler = null;

    /**
     * 私有构造函数
     */
    private NullValueClearSupport() {
        // 防止通过反射的方式来获取该类的实例
        if (null != scheduler) {
            throw new RuntimeException("非法获取scheduler");
        }
    }

    /**
     * 获取Scheduled实例
     */
    public static ScheduledExecutorService getInstance() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return NullValueClearSupport.getInstance(corePoolSize);
    }

    /**
     * 获取Scheduled实例
     */
    public static ScheduledExecutorService getInstance(int corePoolSize) {
        if (null != scheduler) {
            return scheduler;
        }
        synchronized (NullValueClearSupport.class) {
            if (null == scheduler) {
                scheduler = new ScheduledThreadPoolExecutor(corePoolSize,
                        new DaemonThreadFactory("Clear-NullValue-"));
            }
        }
        return scheduler;
    }
}
