package com.github.jesse.l2cache.spring.consistency;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.Cache;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.cache.Level1Cache;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.spring.cache.L2CacheCacheManager;
import com.github.jesse.l2cache.spring.cache.L2CacheSpringCache;
import com.github.jesse.l2cache.util.CacheValueHashUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 一致性检测服务。
 * <p>
 * 基于 Redis Pub/Sub（Redisson RTopic）实时触发，各实例订阅 channel 后上报本机 L1 缓存快照。
 * <p>
 * 由 {@code L2CacheConfiguration} 在 {@code management-enabled=true} 且存在 RedissonClient 时以 @Bean 条件装配，
 * 不依赖组件扫描。
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Slf4j
public class ConsistencyCheckService {

    private static final String CHANNEL = "l2cache:consistency:channel";
    private static final String RESULT_KEY_PREFIX = "l2cache:consistency:result:";
    private static final long RESULT_TTL_SECONDS = 10;
    private static final long COLLECT_TIMEOUT_MILLIS = 3000;

    @Autowired
    @Qualifier("redissonClient")
    private RedissonClient redissonClient;

    @Autowired
    private L2CacheCacheManager l2CacheCacheManager;

    @PostConstruct
    public void subscribe() {
        try {
            RTopic topic = redissonClient.getTopic(CHANNEL);
            topic.addListener(ConsistencyTask.class, (channel, task) -> {
                try {
                    InstanceSnapshot snapshot = computeSnapshot(task);
                    String resultKey = RESULT_KEY_PREFIX + task.getTaskId() + ":" + L2CacheConfig.INSTANCE_ID;
                    redissonClient.getBucket(resultKey).set(snapshot, RESULT_TTL_SECONDS, TimeUnit.SECONDS);
                    log.debug("[ConsistencyCheckService] reported snapshot, taskId={}, instanceId={}", task.getTaskId(), L2CacheConfig.INSTANCE_ID);
                } catch (Exception e) {
                    log.warn("[ConsistencyCheckService] handle task error, taskId={}", task.getTaskId(), e);
                }
            });
            log.info("[ConsistencyCheckService] subscribed channel={}", CHANNEL);
        } catch (Exception e) {
            log.warn("[ConsistencyCheckService] subscribe error", e);
        }
    }

    /**
     * 触发一致性检测
     *
     * @param cacheName  缓存名称
     * @param mode       检测模式：count / value
     * @param keys       value 模式下指定对比的 key（逗号分隔）
     * @param sampleSize value 模式下未指定 key 时的采样大小
     * @return 一致性检测结果
     */
    public ConsistencyResult check(String cacheName, String mode, String keys, int sampleSize) {
        String taskId = IdUtil.simpleUUID();
        List<String> keyList = parseKeys(keys);
        ConsistencyTask task = new ConsistencyTask(taskId, cacheName, mode, keyList, sampleSize);

        // 发布检测任务
        RTopic topic = redissonClient.getTopic(CHANNEL);
        topic.publish(task);
        log.debug("[ConsistencyCheckService] published task, taskId={}, cacheName={}, mode={}", taskId, cacheName, mode);

        // 等待各实例上报
        try {
            Thread.sleep(COLLECT_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ConsistencyResult result = new ConsistencyResult();
        result.setTaskId(taskId);
        result.setCacheName(cacheName);
        result.setMode(mode);

        // 收集各实例快照
        Map<String, InstanceSnapshot> snapshots = collectSnapshots(taskId);
        result.setInstanceSnapshots(snapshots);

        // 获取 L2 基准快照
        InstanceSnapshot l2Snapshot = computeL2Snapshot(task);
        result.setL2Snapshot(l2Snapshot);

        // 比对差异
        compare(result);

        return result;
    }

    /**
     * 计算本实例快照
     */
    private InstanceSnapshot computeSnapshot(ConsistencyTask task) {
        InstanceSnapshot snapshot = new InstanceSnapshot();
        snapshot.setInstanceId(L2CacheConfig.INSTANCE_ID);

        CompositeCache cache = getCompositeCache(task.getCacheName());
        if (cache == null) {
            return snapshot;
        }
        Level1Cache l1 = cache.getLevel1Cache();
        if (l1 == null) {
            return snapshot;
        }

        if ("count".equals(task.getMode())) {
            snapshot.setKeyCount(l1.size());
        } else if ("value".equals(task.getMode())) {
            List<String> targetKeys = getTargetKeys(task, l1);
            for (String key : targetKeys) {
                Object value = l1.get(key);
                snapshot.getValueHashes().put(key, CacheValueHashUtil.calcHash(value));
            }
        }
        return snapshot;
    }

    /**
     * 计算 L2 基准快照
     */
    private InstanceSnapshot computeL2Snapshot(ConsistencyTask task) {
        InstanceSnapshot snapshot = new InstanceSnapshot();
        snapshot.setInstanceId("L2");

        CompositeCache cache = getCompositeCache(task.getCacheName());
        if (cache == null) {
            return snapshot;
        }
        Level1Cache l1 = cache.getLevel1Cache();

        if ("count".equals(task.getMode())) {
            // simple-by-design: L2 key 数量统计成本较高，count 模式主要比对各实例 L1 数量差异
            snapshot.setKeyCount(-1);
        } else if ("value".equals(task.getMode())) {
            List<String> targetKeys = getTargetKeys(task, l1);
            for (String key : targetKeys) {
                String cacheKey = task.getCacheName() + CacheConsts.SPLIT + key;
                Object value = redissonClient.getBucket(cacheKey).get();
                snapshot.getValueHashes().put(key, CacheValueHashUtil.calcHash(value));
            }
        }
        return snapshot;
    }

    /**
     * 获取目标 key 列表：优先使用指定 key，否则从 L1 随机采样
     */
    private List<String> getTargetKeys(ConsistencyTask task, Level1Cache l1) {
        if (task.getKeys() != null && !task.getKeys().isEmpty()) {
            return task.getKeys();
        }
        if (l1 == null) {
            return Collections.emptyList();
        }
        Set<Object> allKeys = l1.keys();
        if (allKeys == null || allKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keyList = allKeys.stream().map(Object::toString).collect(Collectors.toList());
        Collections.shuffle(keyList);
        int sample = task.getSampleSize() > 0 ? task.getSampleSize() : 100;
        sample = Math.min(sample, keyList.size());
        return keyList.subList(0, sample);
    }

    /**
     * 收集各实例上报的快照
     */
    private Map<String, InstanceSnapshot> collectSnapshots(String taskId) {
        Map<String, InstanceSnapshot> map = new HashMap<>();
        try {
            RKeys rKeys = redissonClient.getKeys();
            Iterable<String> keys = rKeys.getKeysByPattern(RESULT_KEY_PREFIX + taskId + ":*");
            for (String key : keys) {
                RBucket<InstanceSnapshot> bucket = redissonClient.getBucket(key);
                InstanceSnapshot snapshot = bucket.get();
                if (snapshot != null) {
                    map.put(snapshot.getInstanceId(), snapshot);
                }
            }
        } catch (Exception e) {
            log.warn("[ConsistencyCheckService] collectSnapshots error, taskId={}", taskId, e);
        }
        return map;
    }

    /**
     * 获取 CompositeCache
     */
    private CompositeCache getCompositeCache(String cacheName) {
        org.springframework.cache.Cache springCache = l2CacheCacheManager.getCache(cacheName);
        if (!(springCache instanceof L2CacheSpringCache)) {
            return null;
        }
        Cache nativeCache = ((L2CacheSpringCache) springCache).getNativeCache();
        if (nativeCache instanceof CompositeCache) {
            return (CompositeCache) nativeCache;
        }
        return null;
    }

    /**
     * 解析逗号分隔的 key
     */
    private List<String> parseKeys(String keys) {
        if (StrUtil.isBlank(keys)) {
            return Collections.emptyList();
        }
        String[] arr = keys.split(",");
        List<String> list = new ArrayList<>(arr.length);
        for (String key : arr) {
            if (StrUtil.isNotBlank(key)) {
                list.add(key.trim());
            }
        }
        return list;
    }

    /**
     * 比对差异
     */
    private void compare(ConsistencyResult result) {
        List<String> diffReport = new ArrayList<>();
        InstanceSnapshot l2Snapshot = result.getL2Snapshot();

        if ("count".equals(result.getMode())) {
            long firstCount = -1;
            String firstInstance = null;
            for (Map.Entry<String, InstanceSnapshot> entry : result.getInstanceSnapshots().entrySet()) {
                long count = entry.getValue().getKeyCount();
                if (firstCount < 0) {
                    firstCount = count;
                    firstInstance = entry.getKey();
                    continue;
                }
                if (count != firstCount) {
                    diffReport.add("instance=" + entry.getKey() + " keyCount=" + count + " vs instance=" + firstInstance + " keyCount=" + firstCount);
                }
            }
        } else if ("value".equals(result.getMode())) {
            Map<String, String> l2Hashes = l2Snapshot.getValueHashes();
            for (Map.Entry<String, InstanceSnapshot> entry : result.getInstanceSnapshots().entrySet()) {
                Map<String, String> instanceHashes = entry.getValue().getValueHashes();
                for (Map.Entry<String, String> l2Entry : l2Hashes.entrySet()) {
                    String key = l2Entry.getKey();
                    String l2Hash = l2Entry.getValue();
                    String instanceHash = instanceHashes.get(key);
                    if (!Objects.equals(l2Hash, instanceHash)) {
                        diffReport.add("instance=" + entry.getKey() + " key=" + key + " l1Hash=" + instanceHash + " l2Hash=" + l2Hash);
                    }
                }
                // 实例间互相比对
                for (Map.Entry<String, InstanceSnapshot> otherEntry : result.getInstanceSnapshots().entrySet()) {
                    if (otherEntry.getKey().equals(entry.getKey())) {
                        continue;
                    }
                    Map<String, String> otherHashes = otherEntry.getValue().getValueHashes();
                    for (Map.Entry<String, String> entryHash : instanceHashes.entrySet()) {
                        String key = entryHash.getKey();
                        String hash = entryHash.getValue();
                        String otherHash = otherHashes.get(key);
                        if (!Objects.equals(hash, otherHash)) {
                            diffReport.add("instance=" + entry.getKey() + " key=" + key + " hash=" + hash + " vs instance=" + otherEntry.getKey() + " hash=" + otherHash);
                        }
                    }
                }
            }
        }

        result.setDiffReport(diffReport);
        result.setCompleted(!result.getInstanceSnapshots().isEmpty());
    }
}
