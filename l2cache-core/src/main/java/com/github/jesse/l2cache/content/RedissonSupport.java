package com.github.jesse.l2cache.content;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.L2CacheConfig;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此 RedissonClient 容器的作用：将 RedissonClient 共享，以便在 RedisCacheSyncPolicy 和 RedissonCache 中重复使用。
 *
 * @author chenck
 * @date 2020/7/7 16:06
 */
public class RedissonSupport {

    /**
     * Map<InstanceId,RedissonClient>
     */
    private static final Map<String, RedissonClient> REDISSON_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * 获取或创建缓存实例
     */
    public static RedissonClient getRedisson(L2CacheConfig l2CacheConfig) {
        RedissonClient redissonClient = REDISSON_MAP.get(L2CacheConfig.INSTANCE_ID);
        if (null != redissonClient) {
            return redissonClient;
        }
        synchronized (lock) {
            redissonClient = REDISSON_MAP.get(L2CacheConfig.INSTANCE_ID);
            if (null != redissonClient) {
                return redissonClient;
            }
            Config config = RedissonSupport.getRedissonConfig(l2CacheConfig.getRedissonYamlConfig());
            if (null == config) {
                // 默认走本地redis，方便测试
                redissonClient = Redisson.create();
            } else {
                redissonClient = Redisson.create(config);
            }
            REDISSON_MAP.put(L2CacheConfig.INSTANCE_ID, redissonClient);
            return redissonClient;
        }
    }

    /**
     * 解析Redisson yaml文件，获取Redisson Config
     */
    public static org.redisson.config.Config getRedissonConfig(String redissonYamlConfig) {
        if (StrUtil.isBlank(redissonYamlConfig)) {
            return null;
        }
        try {
            // 此方式可获取到springboot打包以后jar包内的资源文件
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(redissonYamlConfig);
            if (null == is) {
                throw new IllegalStateException("not found redisson yaml config file:" + redissonYamlConfig);
            }
            return org.redisson.config.Config.fromYAML(is);
        } catch (IOException e) {
            throw new IllegalStateException("parse redisson yaml config error", e);
        }
    }
}
