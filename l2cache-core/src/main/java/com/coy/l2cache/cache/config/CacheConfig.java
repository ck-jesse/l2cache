package com.coy.l2cache.cache.config;

import com.coy.l2cache.cache.CacheType;
import com.coy.l2cache.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
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
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean allowNullValues = true;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;

    /**
     * 缓存类型，默认 COMPOSITE 组合缓存
     *
     * @see CacheType
     */
    private String cacheType = CacheType.COMPOSITE.name();

    private final Composite composite = new Composite();
    private final Caffeine caffeine = new Caffeine();
    private final Redis redis = new Redis();
    private final CacheSyncPolicy cacheSyncPolicy = new CacheSyncPolicy();

    public interface Config {
    }

    /**
     * 组合缓存配置
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Composite implements Config {
        /**
         * 一级缓存类型
         */
        private String l1CacheType = CacheType.CAFFEINE.name();
        /**
         * 二级缓存类型
         */
        private String l2CacheType = CacheType.REDIS.name();
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
        private Integer refreshPoolSize = 3;

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
     * Redis specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Redis implements Config {

        /**
         * Whether to use the key prefix when writing to Redis.
         */
        private boolean useKeyPrefix = true;

        /**
         * 缓存Key prefix.
         */
        private String keyPrefix;


        /**
         * 缓存过期时间(ms)
         */
        private long expireTime;

        /**
         * 缓存最大空闲时间(ms)
         * 注：在 Redisson 中 缓存过期被淘汰的时间 取符合条件的 expireTime 和 maxIdleTime 中间小的值。
         * 如：expireTime=10s, maxIdleTime=5s, 那么当缓存空闲5s时，会被 Redisson 淘汰掉。
         */
        private long maxIdleTime;

        /**
         * 最大缓存数，以便剔除多余元素
         * 注：注意如果与一级缓存（如：caffeine）中最大数量大小不一致，会出现一级缓存和二级缓存中缓存数量不一致，所以建议设置为一致减少不必要的歧义。
         */
        private int maxSize;

        /**
         * Redisson 的yaml配置文件
         */
        private String redissonYamlConfig;

        /**
         * Redisson Config
         */
        private org.redisson.config.Config redissonConfig;

        /**
         * 解析Redisson yaml文件
         */
        public org.redisson.config.Config getRedissonConfig() {
            if (null != redissonConfig) {
                return redissonConfig;
            }
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                URL url = classLoader.getResource(this.redissonYamlConfig);
                redissonConfig = org.redisson.config.Config.fromYAML(url);
                return redissonConfig;
            } catch (IOException e) {
                throw new IllegalStateException("parse redisson yaml config error", e);
            }
        }

    }

    /**
     * 缓存同步策略配置
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class CacheSyncPolicy implements Config {

        /**
         * 策略类型
         */
        private String type;

        /**
         * 缓存更新时通知其他节点的topic名称
         */
        private String topic = "l2cache:sync:topic";
    }
}
