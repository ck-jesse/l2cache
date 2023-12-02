package com.github.jesse.l2cache;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.consts.CacheSyncPolicyType;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.consts.HotkeyType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author chenck
 * @date 2020/6/30 17:19
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class CacheConfig {

    /**
     * 缓存实例id
     */
    private String instanceId = "C" + IdUtil.getSnowflakeNextIdStr();

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
     * 简单处理：日志级别配置，所以此参数可用于控制是否打印日志
     * 原因：一方面日志量太多影响性能，一方面日志量太多存储成本高
     */
    private String logLevel = CacheConsts.LOG_DEBUG;

    /**
     * 是否使用一级缓存的过期时间来替换二级缓存的过期时间，默认为true
     * 注：目的是为了简化缓存配置，且保证一级缓存和二级缓存的配置一致。因此在使用混合缓存时，只需要配置一级缓存即可。
     */
    private boolean useL1ReplaceL2ExpireTime = true;

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
    private final HotKey hotKey = new HotKey();

    public interface Config {
    }

    /**
     * 组合缓存配置
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
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
         * CompositeCache.batchPut()中往l2中是通过batchPut()还是循环put()来处理缓存数据，默认false
         * true 表示调用l2的batchPut()来缓存数据
         * false 表示循环调用l2的put()来缓存数据
         */
        private boolean l2BatchPut = false;
        /**
         * CompositeCache.batchEvict()中往l2中是通过batchEvict()还是循环调用evict()来处理缓存数据，默认false
         * true 表示调用l2的batchEvict()
         * false 表示循环调用l2的evict()
         */
        private boolean l2BatchEvict = false;
        /**
         * 是否全部启用一级缓存，默认false
         */
        private boolean l1AllOpen = false;

        /**
         * 是否手动启用一级缓存，默认false
         */
        private boolean l1Manual = false;

        /**
         * 手动配置走一级缓存的缓存key集合，针对单个key维度
         */
        private Set<String> l1ManualKeySet = new HashSet<>();

        /**
         * 手动配置走一级缓存的缓存名字集合，针对cacheName维度
         */
        private Set<String> l1ManualCacheNameSet = new HashSet<>();
    }

    /**
     * Caffeine specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
    public static class Caffeine implements Config {
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
         * 同一个key的发布消息频率(毫秒)
         */
        private Long publishMsgPeriodMilliSeconds = 500L;

        /**
         * 批量获取操作日志级别，因批量获取时，日志量非常大，而且每一条日志都是打印全量的数据，会对性能有影响，所以此参数可用于控制是否打印日志
         */
        private String batchGetLogLevel = CacheConsts.LOG_DEBUG;

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
         * 是否启用自定义 MdcForkJoinPool，用于链路追踪
         */
        private boolean enableMdcForkJoinPool = true;
    }

    /**
     * guava specific cache properties.
     */
    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
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
        private Long refreshPeriod = 30L;

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
    @ToString
    public static class Redis implements Config {

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
         * 针对cacheName维度过期时间集合
         * <cacheName,过期时间(ms)>
         */
        private Map<String, Long> expireTimeCacheNameMap = new HashMap<>();

        /**
         * 批量操作的大小，可以理解为是分页
         */
        private int batchPageSize = 50;

        /**
         * 批量获取操作日志级别，因批量获取时，日志量非常大，而且每一条日志都是打印全量的数据，会对性能有影响，所以此参数可用于控制是否打印日志
         */
        private String batchGetLogLevel = CacheConsts.LOG_INFO;

        /**
         * 是否打印详细日志，方便问题排查
         */
        private String printDetailLogSwitch = CacheConsts.NOT_PRINT_DETAIL_LOG;

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
            if (StrUtil.isBlank(this.redissonYamlConfig) && redissonConfig == null) {
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
    @ToString
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
        private boolean async;

        /**
         * 具体的属性配置
         * 定义一个通用的属性字段，不同的MQ可配置各自的属性即可。
         * 如:kafka 的属性配置则完全与原生的配置保持一致
         */
        private Properties props = new Properties();
    }


    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
    public static class HotKey implements Config {
        /**
         * 热key类型，默认 NONE 没有集成自动发现功能
         *
         * @see HotkeyType
         */
        private String hotkeyType = HotkeyType.NONE.name();

        private final JdHotKey jdHotKey = new JdHotKey();

        /**
         * 京东热key发现配置
         */
        @Getter
        @Setter
        @Accessors(chain = true)
        @ToString
        public static class JdHotKey implements Config {

            /**
             * 服务名称
             * 需要在dashboard进行注册
             */
            private String serviceName = "default";
            /**
             * ETCD 服务器地址
             */
            private String etcdUrl;
        }
    }


}
