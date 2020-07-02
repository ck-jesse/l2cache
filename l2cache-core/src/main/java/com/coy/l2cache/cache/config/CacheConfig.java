package com.coy.l2cache.cache.config;

import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chenck
 * @date 2020/6/30 17:19
 */
@Getter
@Setter
@Accessors(chain = true)
public class CacheConfig {

    /**
     * 缓存实例id（默认为UUID）
     */
    private String instanceId = RandomUtil.getUUID();

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;

    /**
     * 缓存类型，默认 COMPOSITE 组合缓存
     *
     * @see CacheType
     */
    private String cacheType = CacheType.CAFFEINE.name();

    private final Composite composite = new Composite();
    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();

    public static interface Config {
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Composite implements Config {
        /**
         * 一级缓存类型
         */
        private String l1CacheType;
        /**
         * 二级缓存类型
         */
        private String l2CacheType;
    }

    /**
     * Caffeine specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Caffeine implements Config {

        private final Logger logger = LoggerFactory.getLogger(CacheConfig.Caffeine.class);

        /**
         * 是否异步缓存，true 表示是，false 表示否(默认)
         */
        private boolean asyncCache = false;

        /**
         * 是否自动刷新过期缓存 true 表示是(默认)，false 表示否
         */
        private boolean autoRefreshExpireCache = true;

        /**
         * 缓存刷新调度线程池的大小
         */
        private Integer refreshPoolSize = 1;

        /**
         * 缓存刷新的频率(秒)
         */
        private Long refreshPeriod = 5L;

        /**
         * The spec to use to create caches. See CaffeineSpec for more details on the spec format.
         */
        private String defaultSpec;

        /**
         * The spec to use to create caches. See CaffeineSpec for more details on the spec format.
         * <key,value>=<cacheName, spec>
         */
        private Map<String, String> specs = new HashMap<>();

    }

    /**
     * Redis-specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Redis implements Config {

        /**
         * 是否存储空值，默认true，防止缓存穿透
         */
        private boolean allowNullValues = true;

        /**
         * 过期时间(ms)
         */
        private long expireTime;

        /**
         * 缓存Key prefix.
         */
        private String keyPrefix;

        /**
         * Whether to use the key prefix when writing to Redis.
         */
        private boolean useKeyPrefix = true;

        /**
         * 缓存更新时通知其他节点的topic名称
         */
        private String topic = "l2cache:sync:topic";

    }
}
