package com.coy.l2cache;

import com.coy.l2cache.context.CustomCaffeineSpec;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 一二级缓存属性配置
 * 注：Caffeine 一级缓存，Redis 二级缓存
 *
 * @author chenck
 * @date 2020/4/26 20:44
 */
@ConfigurationProperties(prefix = "spring.cache.multi")
@Getter
@Setter
public class CaffeineRedisCacheProperties {

    /**
     * 缓存实例id（默认为UUID）
     */
    private String instanceId = UUID.randomUUID().toString().replaceAll("-", "");

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean allowNullValues = true;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;

    private final Caffeine caffeine = new Caffeine();

    private final Redis redis = new Redis();

    /**
     * Caffeine specific cache properties.
     */
    @Getter
    @Setter
    public static class Caffeine {

        private final Logger logger = LoggerFactory.getLogger(Caffeine.class);

        /**
         * 是否异步缓存，true 表示是，false 表示否
         */
        private boolean asyncCache = false;

        /**
         * 是否自动刷新过期缓存 true 表示是，false 表示否
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

        /**
         * 获取 spec
         */
        public String getSpec(String cacheName) {
            if (!StringUtils.hasText(cacheName)) {
                return defaultSpec;
            }
            String spec = specs.get(cacheName);
            if (!StringUtils.hasText(spec)) {
                return defaultSpec;
            }
            return spec;
        }

        /**
         * 获取自定义的CaffeineSpec
         */
        public CustomCaffeineSpec getCaffeineSpec(String name) {
            String spec = this.getSpec(name);
            logger.info("create a native Caffeine Cache, name={}, spec={}", name, spec);
            if (!StringUtils.hasText(spec)) {
                return null;
            }
            return CustomCaffeineSpec.parse(spec);
        }
    }

    /**
     * Redis-specific cache properties.
     */
    @Getter
    @Setter
    public static class Redis {

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
        private String topic = "cache:caffeine:redis:topic";

        /**
         * 获取redis key
         */
        public Object getRedisKey(String name, Object key) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(":");
            if (this.isUseKeyPrefix() && !StringUtils.isEmpty(this.getKeyPrefix())) {
                sb.append(this.getKeyPrefix()).append(":");
            }
            sb.append(key.toString());
            return sb.toString();
        }

    }
}
