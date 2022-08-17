package com.jd.platform.hotkey.worker.starters;

import com.jd.platform.hotkey.worker.tool.InitConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-22
 */
@Component
public class InitStarter {
    @Value("${netty.timeOut}")
    private int timeOut;
    @Value("${disruptor.bufferSize}")
    private int bufferSize;
    @Value("${caffeine.minutes}")
    private int caffeineMinutes;

    @Bean("initParam")
    public Object init() {
        InitConstant.timeOut = timeOut;
        InitConstant.bufferSize = bufferSize;
        InitConstant.caffeineMaxMinutes = caffeineMinutes;

        return null;
    }

}
