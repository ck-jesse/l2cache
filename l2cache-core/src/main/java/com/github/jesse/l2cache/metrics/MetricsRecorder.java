package com.github.jesse.l2cache.metrics;

import java.util.List;

/**
 * 缓存监控埋点抽象接口
 * <p>
 * core 默认注入 {@link NoopMetricsRecorder}，starter 可注入具体实现（如 Micrometer）。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
public interface MetricsRecorder {

    /**
     * 记录缓存命中
     *
     * @param level 命中层级，取值 {@link CacheMetrics#LEVEL_L1} 或 {@link CacheMetrics#LEVEL_L2}
     */
    void recordHit(String cacheName, String key, String level);

    /**
     * 记录缓存未命中
     */
    void recordMiss(String cacheName, String key);

    /**
     * 记录缓存穿透（缓存空值）
     */
    void recordPenetration(String cacheName, String key);

    /**
     * 记录缓存 get 请求
     */
    void recordGet(String cacheName, String key);

    /**
     * 记录缓存 put
     */
    void recordPut(String cacheName, String key, Object value);

    /**
     * 记录缓存 evict
     */
    void recordEvict(String cacheName, String key);

    /**
     * 记录缓存大小
     */
    void recordCacheSize(String cacheName, long size);

    /**
     * 记录 key 维度访问与 value 大小
     */
    void recordKeyAccess(String cacheName, String key, long valueSize);

    /**
     * 获取热 key 排行榜
     */
    List<KeyStat> getHotKeyRanking(String cacheName, int topN);

    /**
     * 获取大 key 排行榜
     */
    List<KeyStat> getBigKeyRanking(String cacheName, int topN);
}
