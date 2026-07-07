package com.github.jesse.l2cache.spring.cache;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.metrics.CacheMetrics;
import com.github.jesse.l2cache.metrics.KeyAccessStats;
import com.github.jesse.l2cache.metrics.KeyStat;
import com.github.jesse.l2cache.metrics.MetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Micrometer 的缓存监控埋点实现。
 * <p>
 * 指标分两层：
 * <ul>
 *   <li>cacheName 维度（hits/misses/gets/puts/evicts/size）：走 Micrometer Counter/Gauge，
 *       标签为 cacheName（+ level 仅 hits），暴露给 Prometheus 供 Grafana 面板与告警使用。</li>
 *   <li>key 维度（热key/大key）：走本地 {@link KeyAccessStats} 滑动窗口 TopN 聚合，
 *       不上 Micrometer，避免 Prometheus 标签基数爆炸。通过管控 API 暴露排行。</li>
 * </ul>
 * 两条路径在 recordHit/recordMiss/recordPenetration/recordPut 中并行触发：
 * 先更新 Micrometer Counter（cacheName 维度），再调用 recordKeyAccess（key 维度）。
 * <p>
 * 实例维度不通过自定义标签暴露，复用 Prometheus 抓取时自带的 instance/pod 标签，
 * 避免容器化下标签基数爆炸。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Slf4j
public class MicrometerMetricsRecorder implements MetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final KeyAccessStats keyAccessStats;

    private final Map<String, Counter> counterMap = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheSizeMap = new ConcurrentHashMap<>();
    private final Set<String> registeredGaugeSet = ConcurrentHashMap.newKeySet();

    public MicrometerMetricsRecorder(MeterRegistry meterRegistry, L2CacheConfig.Metrics metricsConfig) {
        this.meterRegistry = meterRegistry;
        this.keyAccessStats = new KeyAccessStats(metricsConfig.getTopNSize(), metricsConfig.getCleanPeriodSeconds(), metricsConfig.getBigKeyThresholdBytes());
    }

    @Override
    public void recordHit(String cacheName, String key, String level) {
        try {
            getCounter(CacheMetrics.CACHE_HITS, cacheName, level).increment();
            recordKeyAccess(cacheName, key, 0);
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordHit error, cacheName={}", cacheName, e);
        }
    }

    @Override
    public void recordMiss(String cacheName, String key) {
        try {
            getCounter(CacheMetrics.CACHE_MISSES, cacheName).increment();
            recordKeyAccess(cacheName, key, 0);
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordMiss error, cacheName={}", cacheName, e);
        }
    }

    @Override
    public void recordPenetration(String cacheName, String key) {
        try {
            getCounter(CacheMetrics.CACHE_PENETRATIONS, cacheName).increment();
            recordKeyAccess(cacheName, key, 0);
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordPenetration error, cacheName={}", cacheName, e);
        }
    }

    /**
     * 记录缓存 get 请求。
     * <p>
     * 仅 cacheName 维度：gets Counter → Prometheus。不记录 key 维度，
     * 因为 get 后必然跟随 hit 或 miss，key 维度会在那里记录，避免重复计数。
     */
    @Override
    public void recordGet(String cacheName, String key) {
        try {
            getCounter(CacheMetrics.CACHE_GETS, cacheName).increment();
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordGet error, cacheName={}", cacheName, e);
        }
    }

    /**
     * 记录缓存 put。
     * <p>
     * cacheName 维度：puts Counter → Prometheus
     * <p>
     * key 维度：recordKeyAccess 带 valueSize → 本地 KeyAccessStats，
     * 同时更新访问计数和 value 大小（大key排行数据来源）
     */
    @Override
    public void recordPut(String cacheName, String key, Object value) {
        try {
            getCounter(CacheMetrics.CACHE_PUTS, cacheName).increment();
            long valueSize = estimateValueSize(value);
            recordKeyAccess(cacheName, key, valueSize);
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordPut error, cacheName={}", cacheName, e);
        }
    }

    /**
     * 记录缓存 evict。
     * <p>
     * 仅 cacheName 维度：evicts Counter → Prometheus。不记录 key 维度，
     * evict 是淘汰操作不是访问行为，不计入热key排行。
     */
    @Override
    public void recordEvict(String cacheName, String key) {
        try {
            getCounter(CacheMetrics.CACHE_EVICTS, cacheName).increment();
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordEvict error, cacheName={}", cacheName, e);
        }
    }

    @Override
    public void recordCacheSize(String cacheName, long size) {
        try {
            cacheSizeMap.put(cacheName, size);
            String gaugeKey = CacheMetrics.CACHE_SIZE + ":" + cacheName;
            if (registeredGaugeSet.add(gaugeKey)) {
                Tags tags = Tags.of(CacheMetrics.TAG_CACHE_NAME, cacheName);
                Gauge.builder(CacheMetrics.CACHE_SIZE, cacheSizeMap, map -> map.getOrDefault(cacheName, 0L).doubleValue())
                        .tags(tags)
                        .register(meterRegistry);
            }
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordCacheSize error, cacheName={}", cacheName, e);
        }
    }

    @Override
    public void recordKeyAccess(String cacheName, String key, long valueSize) {
        try {
            keyAccessStats.recordAccess(cacheName, key, valueSize);
        } catch (Exception e) {
            log.debug("[MicrometerMetricsRecorder] recordKeyAccess error, cacheName={}", cacheName, e);
        }
    }

    @Override
    public List<KeyStat> getHotKeyRanking(String cacheName, int topN) {
        return keyAccessStats.getHotKeyRanking(cacheName, topN);
    }

    @Override
    public List<KeyStat> getBigKeyRanking(String cacheName, int topN) {
        return keyAccessStats.getBigKeyRanking(cacheName, topN);
    }

    private Counter getCounter(String metricName, String cacheName) {
        return getCounter(metricName, cacheName, null);
    }

    private Counter getCounter(String metricName, String cacheName, String level) {
        String key = metricName + ":" + cacheName + ":" + (level == null ? "" : level);
        return counterMap.computeIfAbsent(key, k -> {
            Tags tags = Tags.of(CacheMetrics.TAG_CACHE_NAME, cacheName);
            if (level != null) {
                tags = tags.and(CacheMetrics.TAG_LEVEL, level);
            }
            return Counter.builder(metricName).tags(tags).register(meterRegistry);
        });
    }

    /**
     * 粗略估算 value 大小（字节）。
     * <p>
     * simple-by-design: 使用 toString().getBytes() 做简单估算，适用于字符串或简单对象。
     * 复杂对象（如大集合、二进制数据）建议后续接入序列化大小计算以获得更准确值。
     */
    private long estimateValueSize(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            String str = value.toString();
            return str == null ? 0 : str.getBytes().length;
        } catch (Exception e) {
            return 0;
        }
    }
}
