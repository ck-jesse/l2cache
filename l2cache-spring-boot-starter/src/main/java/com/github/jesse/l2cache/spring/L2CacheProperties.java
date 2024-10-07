package com.github.jesse.l2cache.spring;

import com.github.jesse.l2cache.CacheConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 一二级缓存属性配置
 * 注：Caffeine 一级缓存，Redis 二级缓存
 *
 * @author chenck
 * @date 2020/4/26 20:44
 */
@ConfigurationProperties(prefix = "l2cache")
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
