package cn.weeget.hotkey.worker.config;

import cn.weeget.hotkey.worker.netty.client.ClientChangeListener;
import cn.weeget.hotkey.worker.netty.client.IClientChangeListener;
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
