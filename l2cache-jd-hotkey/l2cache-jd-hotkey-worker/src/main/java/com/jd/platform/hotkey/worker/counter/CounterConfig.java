package com.jd.platform.hotkey.worker.counter;

import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-28
 */
@Configuration
public class CounterConfig {
    /**
     * 队列
     */
    public static LinkedBlockingQueue<KeyCountItem> COUNTER_QUEUE = new LinkedBlockingQueue<>();

    @Resource
    private IConfigCenter configCenter;

    @Bean
    public CounterConsumer counterConsumer() {
        CounterConsumer counterConsumer = new CounterConsumer();
        counterConsumer.beginConsume(configCenter);
        return counterConsumer;
    }

}
