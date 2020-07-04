package com.coy.l2cache.test;

import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.cache.RedissonCache;
import com.coy.l2cache.cache.builder.CacheBuilder;
import com.coy.l2cache.cache.builder.RedisCacheBuilder;
import com.coy.l2cache.cache.config.CacheConfig;
import org.junit.Before;

public class RedisCacheTest {

    CacheConfig cacheConfig = new CacheConfig();
    RedissonCache cache;

    @Before
    public void before() {
        cacheConfig.setCacheType(CacheType.REDIS.name())
                .getRedis()
                .setExpireTime(1000)
                .setMaxIdleTime(5000)
                .setMaxSize(2)
                .setRedissonYamlConfig("redisson.yaml");

        CacheBuilder builder = new RedisCacheBuilder().setCacheConfig(cacheConfig);
        cache = (RedissonCache) builder.build("test3");
    }

}
