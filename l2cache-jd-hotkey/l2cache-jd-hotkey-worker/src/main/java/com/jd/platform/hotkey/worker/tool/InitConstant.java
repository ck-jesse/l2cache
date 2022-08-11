package com.jd.platform.hotkey.worker.tool;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-22
 */
public class InitConstant {
    public static int timeOut = 5000;

    //单位是百万
    public static int bufferSize = 2;

    public static final LongAdder expireTotalCount = new LongAdder();

    public static final LongAdder totalDealCount = new LongAdder();

    public static final LongAdder totalOfferCount = new LongAdder();

    /**
     * key在caffeine里多久过期，默认只存1分钟的
     */
    public static int caffeineMaxMinutes = 1;
}
