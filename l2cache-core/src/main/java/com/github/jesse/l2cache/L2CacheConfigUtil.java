package com.github.jesse.l2cache;

import cn.hutool.core.collection.CollectionUtil;
import com.github.jesse.l2cache.content.CacheSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class L2CacheConfigUtil {


    private static final Logger logger = LoggerFactory.getLogger(L2CacheConfigUtil.class);

    /**
     * 根据 cacheName 获取对应的 CacheConfig
     *
     * @param
     * @author chenck
     * @date 2024/10/14 12:50
     */
    public static L2CacheConfig.CacheConfig getCacheConfig(L2CacheConfig l2CacheConfig, String cacheName) {

        L2CacheConfig.CacheConfig defaultConfig = l2CacheConfig.getDefaultConfig();
        Map<String, L2CacheConfig.CacheConfig> configMap = l2CacheConfig.getConfigMap();

        if (CollectionUtil.isEmpty(configMap)) {
            if (logger.isDebugEnabled()) {
                logger.debug("use defaultConfig, L2CacheConfig.configMap is empty, cacheName=" + cacheName);
            }
            return defaultConfig;
        }

        if (!configMap.containsKey(cacheName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("use defaultConfig, L2CacheConfig.configMap not contain cacheName=" + cacheName);
            }
            return defaultConfig;
        }

        return configMap.getOrDefault(cacheName, defaultConfig);
    }
}
