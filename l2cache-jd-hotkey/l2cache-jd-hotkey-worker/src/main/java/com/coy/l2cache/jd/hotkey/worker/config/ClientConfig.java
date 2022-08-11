package com.coy.l2cache.jd.hotkey.worker.config;

import com.coy.l2cache.jd.hotkey.worker.netty.client.ClientChangeListener;
import com.coy.l2cache.jd.hotkey.worker.netty.client.IClientChangeListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuweifeng wrote on 2019-12-11
 * @version 1.0
 */
@Configuration
public class ClientConfig {
    @Bean
    public IClientChangeListener clientChangeListener() {
        return new ClientChangeListener();
    }
}
