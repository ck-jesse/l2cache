package com.github.jesse.l2cache.spring.config;

import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.metrics.MetricsRecorder;
import com.github.jesse.l2cache.spring.L2CacheProperties;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import com.github.jesse.l2cache.spring.cache.L2CacheSpringCache;
import com.github.jesse.l2cache.spring.cache.MicrometerMetricsRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 缓存监控指标自动配置。
 * <p>
 * 当 classpath 存在 {@link MeterRegistry} 且开启 {@code l2cache.config.metrics.enabled=true} 时，
 * 注入 {@link MicrometerMetricsRecorder} 并启动缓存大小定时收集。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Slf4j
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(name = "l2cache.config.metrics.enabled", havingValue = "true")
public class MetricsAutoConfiguration {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private L2CacheProperties l2CacheProperties;

    @Autowired
    private L2CacheCacheManager l2CacheCacheManager;

    private MetricsRecorder metricsRecorder;

    private final ScheduledExecutorService sizeCollectorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "l2cache-cache-size-collector");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        L2CacheConfig.Metrics metricsConfig = l2CacheProperties.getConfig().getMetrics();
        MicrometerMetricsRecorder recorder = new MicrometerMetricsRecorder(meterRegistry, metricsConfig);
        this.metricsRecorder = recorder;
        l2CacheCacheManager.setMetricsRecorder(recorder);
        log.info("[MetricsAutoConfiguration] MicrometerMetricsRecorder initialized, instanceId={}", L2CacheConfig.INSTANCE_ID);

        // 启动缓存大小定时收集，15s 一次
        sizeCollectorExecutor.scheduleAtFixedRate(this::collectCacheSize, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        sizeCollectorExecutor.shutdown();
    }

    /**
     * 收集各缓存的大小并注册为 Gauge。
     * <p>
     * simple-by-design: 当前仅收集 CompositeCache 的 L1 缓存大小作为缓存总数估算。
     */
    private void collectCacheSize() {
        try {
            for (org.springframework.cache.Cache springCache : l2CacheCacheManager.getAllCaches()) {
                if (!(springCache instanceof L2CacheSpringCache)) {
                    continue;
                }
                com.github.jesse.l2cache.Cache nativeCache = ((L2CacheSpringCache) springCache).getNativeCache();
                String cacheName = nativeCache.getCacheName();
                long size = 0;
                if (nativeCache instanceof CompositeCache) {
                    Level1Cache l1 = ((CompositeCache) nativeCache).getLevel1Cache();
                    if (l1 != null) {
                        size = l1.size();
                    }
                }
                metricsRecorder.recordCacheSize(cacheName, size);
            }
        } catch (Exception e) {
            log.debug("[MetricsAutoConfiguration] collectCacheSize error", e);
        }
    }
}
