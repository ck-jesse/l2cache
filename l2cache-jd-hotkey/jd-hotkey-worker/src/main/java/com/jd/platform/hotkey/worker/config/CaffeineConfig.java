package com.jd.platform.hotkey.worker.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.jd.platform.hotkey.worker.cache.CaffeineBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author wuweifeng wrote on 2019-12-16
 * @version 1.0
 */
@Configuration
public class CaffeineConfig {

    @Bean("hotKeyCache")
    public Cache<String, Object> hotKeyCache() {
        return CaffeineBuilder.buildRecentHotKeyCache();
    }
}
