package com.github.jesse.l2cache.metrics;

import java.util.Collections;
import java.util.List;

/**
 * 空对象模式（Null Object Pattern）实现。
 * <p>
 * 所有方法方法体为空，未启用监控时 core 默认注入此实现，业务代码无感知且几乎无性能损耗。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
public class NoopMetricsRecorder implements MetricsRecorder {

    @Override
    public void recordHit(String cacheName, String key, String level) {
        // 空操作
    }

    @Override
    public void recordMiss(String cacheName, String key) {
        // 空操作
    }

    @Override
    public void recordPenetration(String cacheName, String key) {
        // 空操作
    }

    @Override
    public void recordGet(String cacheName, String key) {
        // 空操作
    }

    @Override
    public void recordPut(String cacheName, String key, Object value) {
        // 空操作
    }

    @Override
    public void recordEvict(String cacheName, String key) {
        // 空操作
    }

    @Override
    public void recordCacheSize(String cacheName, long size) {
        // 空操作
    }

    @Override
    public void recordKeyAccess(String cacheName, String key, long valueSize) {
        // 空操作
    }

    @Override
    public List<KeyStat> getHotKeyRanking(String cacheName, int topN) {
        return Collections.emptyList();
    }

    @Override
    public List<KeyStat> getBigKeyRanking(String cacheName, int topN) {
        return Collections.emptyList();
    }
}
