package com.github.jesse.l2cache.spring;

import com.github.jesse.l2cache.L2CacheConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * 一二级缓存属性配置
 * 注：Caffeine 一级缓存，Redis 二级缓存
 *
 * @author chenck
 * @date 2020/4/26 20:44
 */
@ConfigurationProperties(prefix = "l2cache")
@RefreshScope
public class L2CacheProperties {
    /**
     * 缓存配置
     */
    private L2CacheConfig config;

    public L2CacheConfig getConfig() {
        return config;
    }

    public void setConfig(L2CacheConfig config) {
        this.config = config;
    }
}
