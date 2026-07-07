package com.github.jesse.l2cache.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * key 维度 TopN 滑动窗口聚合。
 * <p>
 * simple-by-design: 本地内存维护访问计数与 value 大小，按访问频次输出热 key，按 value 大小输出大 key。
 * 默认仅保留 TopN * 2 条高频 key，超过时按访问频次淘汰低频 key。若单个 cacheName 下 distinct key 超过 10w，
 * 建议评估内存或改为采样模式。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Slf4j
public class KeyAccessStats {

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>> accessCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicLong>> valueSizeMap = new ConcurrentHashMap<>();

    private final int topNSize;
    private final long cleanPeriodSeconds;
    private final long bigKeyThresholdBytes;

    private final ScheduledExecutorService cleanExecutor;

    public KeyAccessStats(int topNSize, long cleanPeriodSeconds, long bigKeyThresholdBytes) {
        this.topNSize = topNSize;
        this.cleanPeriodSeconds = cleanPeriodSeconds;
        this.bigKeyThresholdBytes = bigKeyThresholdBytes;
        this.cleanExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "l2cache-key-access-stats-cleaner");
            t.setDaemon(true);
            return t;
        });
        this.cleanExecutor.scheduleAtFixedRate(this::clean, cleanPeriodSeconds, cleanPeriodSeconds, TimeUnit.SECONDS);
    }

    /**
     * 记录 key 访问与 value 大小
     *
     * @param cacheName  缓存名称
     * @param key        缓存 key
     * @param valueSize  value 大小（字节），小于等于 0 时不更新 valueSize
     */
    public void recordAccess(String cacheName, String key, long valueSize) {
        if (cacheName == null || key == null) {
            return;
        }
        accessCountMap.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> new AtomicLong(0))
                .incrementAndGet();

        if (valueSize > 0) {
            valueSizeMap.computeIfAbsent(cacheName, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(key, k -> new AtomicLong(0))
                    .set(valueSize);
        }
    }

    /**
     * 获取热 key 排行榜
     *
     * @param cacheName 缓存名称
     * @param topN      排行榜条数
     * @return 按访问频次降序排列的 key 列表
     */
    public List<KeyStat> getHotKeyRanking(String cacheName, int topN) {
        ConcurrentHashMap<String, AtomicLong> keyMap = accessCountMap.get(cacheName);
        if (keyMap == null || keyMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<KeyStat> list = new ArrayList<>(keyMap.size());
        keyMap.forEach((key, count) -> {
            KeyStat stat = new KeyStat();
            stat.setCacheName(cacheName);
            stat.setKey(key);
            stat.setAccessCount(count.get());
            stat.setValueSize(getValueSize(cacheName, key));
            list.add(stat);
        });
        list.sort(Comparator.comparingLong(KeyStat::getAccessCount).reversed());
        return list.subList(0, Math.min(topN, list.size()));
    }

    /**
     * 获取大 key 排行榜
     *
     * @param cacheName 缓存名称
     * @param topN      排行榜条数
     * @return 按 value 大小降序排列的 key 列表
     */
    public List<KeyStat> getBigKeyRanking(String cacheName, int topN) {
        ConcurrentHashMap<String, AtomicLong> keyMap = valueSizeMap.get(cacheName);
        if (keyMap == null || keyMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<KeyStat> list = new ArrayList<>(keyMap.size());
        keyMap.forEach((key, size) -> {
            long valueSize = size.get();
            if (valueSize < bigKeyThresholdBytes) {
                return;
            }
            KeyStat stat = new KeyStat();
            stat.setCacheName(cacheName);
            stat.setKey(key);
            stat.setAccessCount(getAccessCount(cacheName, key));
            stat.setValueSize(valueSize);
            list.add(stat);
        });
        list.sort(Comparator.comparingLong(KeyStat::getValueSize).reversed());
        return list.subList(0, Math.min(topN, list.size()));
    }

    /**
     * 清理低频 key
     */
    public void clean() {
        try {
            accessCountMap.forEach((cacheName, keyMap) -> {
                if (keyMap.size() <= topNSize * 2) {
                    return;
                }
                // 按访问频次排序，取阈值后淘汰
                List<java.util.Map.Entry<String, AtomicLong>> entries = new ArrayList<>(keyMap.entrySet());
                entries.sort((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()));
                long threshold = entries.get(Math.min(topNSize, entries.size() - 1)).getValue().get();
                keyMap.entrySet().removeIf(entry -> entry.getValue().get() < threshold);
                // 同步清理 valueSizeMap
                ConcurrentHashMap<String, AtomicLong> sizeMap = valueSizeMap.get(cacheName);
                if (sizeMap != null) {
                    sizeMap.keySet().retainAll(keyMap.keySet());
                }
            });
        } catch (Exception e) {
            log.warn("[KeyAccessStats] clean error", e);
        }
    }

    /**
     * 停止清理任务
     */
    public void shutdown() {
        cleanExecutor.shutdown();
    }

    private long getAccessCount(String cacheName, String key) {
        ConcurrentHashMap<String, AtomicLong> keyMap = accessCountMap.get(cacheName);
        if (keyMap == null) {
            return 0;
        }
        AtomicLong count = keyMap.get(key);
        return count == null ? 0 : count.get();
    }

    private long getValueSize(String cacheName, String key) {
        ConcurrentHashMap<String, AtomicLong> keyMap = valueSizeMap.get(cacheName);
        if (keyMap == null) {
            return 0;
        }
        AtomicLong size = keyMap.get(key);
        return size == null ? 0 : size.get();
    }
}
