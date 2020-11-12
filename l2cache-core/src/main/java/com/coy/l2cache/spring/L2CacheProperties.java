package com.coy.l2cache.spring;

import com.coy.l2cache.CacheConfig;
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
    private CacheConfig config;

    public CacheConfig getConfig() {
        return config;
    }

    public void setConfig(CacheConfig config) {
        this.config = config;
    }
}
