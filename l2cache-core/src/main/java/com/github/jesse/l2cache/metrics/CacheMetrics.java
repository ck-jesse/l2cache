package com.github.jesse.l2cache.metrics;

/**
 * 缓存指标名称与标签常量
 * <p>
 * 指标名采用 Micrometer 点号风格，由 PrometheusMeterRegistry 自动转换为下划线风格。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
public final class CacheMetrics {

    private CacheMetrics() {
    }

    /**
     * 缓存命中次数（Counter）
     * <p>Prometheus 展示：l2cache_cache_hits_total</p>
     */
    public static final String CACHE_HITS = "l2cache.cache.hits";

    /**
     * 缓存未命中次数（Counter）
     * <p>Prometheus 展示：l2cache_cache_misses_total</p>
     */
    public static final String CACHE_MISSES = "l2cache.cache.misses";

    /**
     * 缓存穿透次数（Counter）
     * <p>Prometheus 展示：l2cache_cache_penetrations_total</p>
     */
    public static final String CACHE_PENETRATIONS = "l2cache.cache.penetrations";

    /**
     * 缓存 get 请求总数（Counter）
     * <p>Prometheus 展示：l2cache_cache_gets_total</p>
     */
    public static final String CACHE_GETS = "l2cache.cache.gets";

    /**
     * 缓存 put 次数（Counter）
     * <p>Prometheus 展示：l2cache_cache_puts_total</p>
     */
    public static final String CACHE_PUTS = "l2cache.cache.puts";

    /**
     * 缓存 evict 次数（Counter）
     * <p>Prometheus 展示：l2cache_cache_evicts_total</p>
     */
    public static final String CACHE_EVICTS = "l2cache.cache.evicts";

    /**
     * 缓存条目数（Gauge）
     * <p>Prometheus 展示：l2cache_cache_size</p>
     */
    public static final String CACHE_SIZE = "l2cache.cache.size";

    /**
     * 缓存占用内存大小，单位字节（Gauge）
     * <p>Prometheus 展示：l2cache_cache_memory_size_bytes</p>
     */
    public static final String CACHE_MEMORY_SIZE = "l2cache.cache.memory.size";

    /**
     * 标签：缓存名称
     */
    public static final String TAG_CACHE_NAME = "cacheName";

    /**
     * 标签：缓存层级（l1 / l2），仅用于命中指标
     */
    public static final String TAG_LEVEL = "level";

    /**
     * 层级常量：一级缓存
     */
    public static final String LEVEL_L1 = "l1";

    /**
     * 层级常量：二级缓存
     */
    public static final String LEVEL_L2 = "l2";
}
