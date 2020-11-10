package com.coy.l2cache;

import com.coy.l2cache.consts.CacheSyncPolicyType;
import com.coy.l2cache.consts.CacheType;
import com.coy.l2cache.util.RandomUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
     * 是否存储空值，设置为true时，可防止缓存穿透
     */
    private boolean allowNullValues = true;

    /**
     * NullValue的过期时间，单位秒，默认30秒
     * 用于淘汰NullValue的值
     * 注：当缓存项的过期时间小于该值时，则NullValue不会淘汰
     */
    private long nullValueExpireTimeSeconds = 60;

    /**
     * NullValue 的最大数量，防止出现内存溢出
     * 注：当超出该值时，会在下一次刷新缓存时，淘汰掉NullValue的元素
     */
    private long nullValueMaxSize = 3000;

    /**
     * NullValue 的清理频率(秒)
     */
    private long nullValueClearPeriodSeconds = 10L;

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
    private final Guava guava = new Guava();
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
        /**
         * 是否开启一级缓存，默认true开启
         * 注：便于动态控制一二级缓存
         */
        private boolean startupL1Cache = true;
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
        private boolean autoRefreshExpireCache = false;

        /**
         * 缓存刷新调度线程池的大小
         * 默认为 CPU数 * 2
         */
        private Integer refreshPoolSize = Runtime.getRuntime().availableProcessors();

        /**
         * 缓存刷新的频率(秒)
         */
        private Long refreshPeriod = 30L;

        /**
         * 缓存刷新时，是否淘汰缓存NullValue对象，true 表示淘汰，false 表示不淘汰(默认)
         * 设置为true，则表示是提前淘汰NullValue对象，设置为false，则表示NullValue对象的过期时间与缓存过期时间一致
         */
        private boolean refreshInvalidateNullValue = false;

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
     * guava specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Guava implements Config {
        /**
         * 是否自动刷新过期缓存 true 表示是(默认)，false 表示否
         */
        private boolean autoRefreshExpireCache = true;

        /**
         * 缓存刷新调度线程池的大小
         */
        private Integer refreshPoolSize = Runtime.getRuntime().availableProcessors();

        /**
         * 缓存刷新的频率(秒)
         */
        private Long refreshPeriod = 10L;

        /**
         * 缓存刷新时，是否淘汰缓存NullValue对象，true 表示淘汰，false 表示不淘汰(默认)
         * 设置为true，则表示是提前淘汰NullValue对象，设置为false，则表示NullValue对象的过期时间与缓存过期时间一致
         */
        private boolean refreshInvalidateNullValue = false;

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
        @Deprecated
        private boolean useKeyPrefix = true;

        /**
         * 缓存Key prefix.
         */
        @Deprecated
        private String keyPrefix;

        /**
         * 加载数据时，是否加锁
         */
        private boolean lock = false;

        /**
         * 加载数据时是调用tryLock()，还是lock()
         * 注：
         * tryLock() 只有一个请求执行加载动作，其他并发请求，直接返回失败
         * lock() 只有一个请求执行加载动作，其他并发请求，会阻塞直到获得锁
         */
        private boolean tryLock = true;

        /**
         * 缓存过期时间(ms)
         * 注：作为默认的缓存过期时间，如果一级缓存设置了过期时间，则以一级缓存的过期时间为准。
         * 目的是为了支持cacheName维度的缓存过期时间设置
         */
        private long expireTime;

        /**
         * 缓存最大空闲时间(ms) - RMap 参数
         * 注：在 Redisson 中 缓存过期被淘汰的时间 取符合条件的 expireTime 和 maxIdleTime 中间小的值。
         * 如：expireTime=10s, maxIdleTime=5s, 那么当缓存空闲5s时，会被 Redisson 淘汰掉。
         * 注：从1.0.13版本开始废弃
         */
        @Deprecated
        private long maxIdleTime;

        /**
         * 最大缓存数，以便剔除多余元素 - RMap 参数
         * 注：作为默认的最大缓存数，如果一级缓存设置了最大缓存数，则以一级缓存的最大缓存数为准。
         * 注：注意如果与一级缓存（如：caffeine）中最大数量大小不一致，会出现一级缓存和二级缓存中缓存数量不一致，所以建议设置为一致减少不必要的歧义。
         * 注：从1.0.13版本开始废弃
         */
        @Deprecated
        private int maxSize;

        /**
         * 是否启动最大值，默认false - RMap 参数
         * 注：主要为了支持需要缓存的数据量比较大，导致本地缓存因为机器的限制值存放部分数据，而redis可以存放全部数据，所以本地缓存建议设置最大值，当超过最大值时，会从redis中获取
         * 注：从1.0.13版本开始废弃
         */
        @Deprecated
        private boolean startupMaxSize = false;

        /**
         * 是否启用副本，默认false
         * 主要解决单个redis分片上热点key的问题，相当于原来存一份数据，现在存多份相同的数据，将热key的压力分散到多个分片。
         * 以redis内存空间来降低单分片压力。
         */
        private boolean duplicate = false;

        /**
         * 针对所有key启用副本
         */
        private boolean duplicateALlKey = false;

        /**
         * 默认副本数量
         */
        private int defaultDuplicateSize = 10;

        /**
         * 副本缓存key集合，针对单个key维度
         * <key,副本数量>
         */
        private Map<String, Integer> duplicateKeyMap = new HashMap<>();

        /**
         * 副本缓存名字集合，针对cacheName维度
         * <cacheName,副本数量>
         */
        private Map<String, Integer> duplicateCacheNameMap = new HashMap<>();

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
            if (StringUtils.isEmpty(this.redissonYamlConfig)) {
                return null;
            }
            if (null != redissonConfig) {
                return redissonConfig;
            }
            try {
                // 此方式可获取到springboot打包以后jar包内的资源文件
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.redissonYamlConfig);
                if (null == is) {
                    throw new IllegalStateException("not found redisson yaml config file:" + redissonYamlConfig);
                }
                redissonConfig = org.redisson.config.Config.fromYAML(is);
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
         *
         * @see CacheSyncPolicyType
         */
        private String type;

        /**
         * 缓存更新时通知其他节点的topic名称
         */
        private String topic = "l2cache";

        /**
         * 是否支持异步发送消息
         */
        private boolean isAsync;

        /**
         * 具体的属性配置
         * 定义一个通用的属性字段，不同的MQ可配置各自的属性即可。
         * 如:kafka 的属性配置则完全与原生的配置保持一致
         */
        private Properties props = new Properties();
    }
}
