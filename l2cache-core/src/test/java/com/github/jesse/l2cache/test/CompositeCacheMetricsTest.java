package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.builder.CompositeCacheBuilder;
import com.github.jesse.l2cache.cache.CompositeCache;
import com.github.jesse.l2cache.consts.CacheType;
import com.github.jesse.l2cache.metrics.KeyStat;
import com.github.jesse.l2cache.metrics.MetricsRecorder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.jesse.l2cache.metrics.CacheMetrics.LEVEL_L1;
import static com.github.jesse.l2cache.metrics.CacheMetrics.LEVEL_L2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * CompositeCache 埋点指标测试
 *
 * @author chenck
 * @date 2026/7/6 11:30
 */
public class CompositeCacheMetricsTest {

    private L2CacheConfig l2CacheConfig;
    private L2CacheConfig.CacheConfig cacheConfig;

    @Before
    public void before() {
        cacheConfig = new L2CacheConfig.CacheConfig();
        cacheConfig.setCacheType(CacheType.COMPOSITE.name())
                .setAllowNullValues(true)
                .getComposite()
                .setL1CacheType(CacheType.CAFFEINE.name())
                .setL2CacheType(CacheType.NONE.name())
                .setL1AllOpen(true);
        cacheConfig.getCaffeine()
                .setDefaultSpec("initialCapacity=10,maximumSize=200,recordStats");

        l2CacheConfig = new L2CacheConfig();
        l2CacheConfig.setDefaultConfig(cacheConfig);
    }

    private CompositeCache buildCache() {
        CacheBuilder builder = new CompositeCacheBuilder()
                .setL2CacheConfig(l2CacheConfig);
        return (CompositeCache) builder.build("metricCache");
    }

    @Test
    public void testPutAndGetHitRecordsMetrics() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        cache.put("k1", "v1");
        assertEquals(1, recorder.putCount.get());
        assertEquals("k1", recorder.lastPutKey);

        recorder.clear();
        Object value = cache.get("k1");
        assertEquals("v1", value);
        assertEquals(1, recorder.getCount.get());
        assertEquals(1, recorder.hitCount.get());
        assertEquals(1, recorder.l1HitCount.get());
        assertEquals(0, recorder.l2HitCount.get());
        assertEquals(0, recorder.missCount.get());
        assertEquals(1, recorder.keyAccessCount.get());
        assertTrue(recorder.accessKeys.contains("k1"));
    }

    @Test
    public void testGetMissRecordsMetrics() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        // L2 为 NoneCache，直接返回 null，触发 miss 埋点
        Object value = cache.get("notExistKey");
        assertEquals(null, value);
        assertEquals(1, recorder.getCount.get());
        assertEquals(0, recorder.hitCount.get());
        assertEquals(1, recorder.missCount.get());
    }

    @Test
    public void testEvictRecordsMetrics() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        cache.evict("k1");
        assertEquals(1, recorder.evictCount.get());
        assertEquals("k1", recorder.lastEvictKey);
    }

    @Test
    public void testBatchPutRecordsMetrics() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        Map<Object, String> dataMap = new HashMap<>();
        dataMap.put("bp1", "v1");
        dataMap.put("bp2", "v2");
        cache.batchPut(dataMap);

        assertEquals(2, recorder.putCount.get());
    }

    @Test
    public void testBatchEvictRecordsMetrics() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("be1", "be1");
        keyMap.put("be2", "be2");
        cache.batchEvict(keyMap);

        assertEquals(2, recorder.evictCount.get());
    }

    @Test
    public void testBatchGetRecordsHitAndMiss() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        // 预热两个key
        cache.put("bg1", "v1");
        cache.put("bg2", "v2");

        recorder.clear();
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("bg1", "bg1");
        keyMap.put("bg2", "bg2");
        keyMap.put("bg3", "bg3");
        cache.batchGet(keyMap, true);

        assertEquals(3, recorder.getCount.get());
        assertEquals(2, recorder.hitCount.get());
        assertEquals(2, recorder.l1HitCount.get());
        assertEquals(0, recorder.l2HitCount.get());
        assertEquals(1, recorder.missCount.get());
    }

    @Test
    public void testBatchGetOrLoadAllMissRecordsPenetration() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        // 注意：L1 Caffeine 由 CacheSupport 按 cacheName 共享，测试间需使用不同的 key 避免空值缓存串扰
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("amk1", "amk1");
        keyMap.put("amk2", "amk2");

        // valueLoader 全部查空，回源DB查空 -> 全部缓存穿透
        cache.batchGetOrLoad(keyMap, keys -> new HashMap<String, String>(), true);

        assertEquals(2, recorder.penetrationCount.get());
    }

    @Test
    public void testBatchGetOrLoadPartialMissRecordsPenetration() {
        CompositeCache cache = buildCache();
        CapturingMetricsRecorder recorder = new CapturingMetricsRecorder();
        cache.setMetricsRecorder(recorder);

        // 注意：L1 Caffeine 由 CacheSupport 按 cacheName 共享，测试间需使用不同的 key 避免空值缓存串扰
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("pmk1", "pmk1");
        keyMap.put("pmk2", "pmk2");

        // valueLoader 仅返回 pmk1，pmk2 查空 -> 仅 pmk2 穿透
        cache.batchGetOrLoad(keyMap, keys -> {
            Map<String, String> result = new HashMap<>();
            result.put("pmk1", "v1");
            return result;
        }, true);

        assertEquals(1, recorder.penetrationCount.get());
    }

    private static class CapturingMetricsRecorder implements MetricsRecorder {

        private final AtomicInteger hitCount = new AtomicInteger();
        private final AtomicInteger l1HitCount = new AtomicInteger();
        private final AtomicInteger l2HitCount = new AtomicInteger();
        private final AtomicInteger missCount = new AtomicInteger();
        private final AtomicInteger penetrationCount = new AtomicInteger();
        private final AtomicInteger getCount = new AtomicInteger();
        private final AtomicInteger putCount = new AtomicInteger();
        private final AtomicInteger evictCount = new AtomicInteger();
        private final AtomicInteger cacheSizeCount = new AtomicInteger();
        private final AtomicInteger keyAccessCount = new AtomicInteger();

        private final List<String> accessKeys = new ArrayList<>();

        private String lastPutKey;
        private String lastEvictKey;

        @Override
        public void recordHit(String cacheName, String key, String level) {
            hitCount.incrementAndGet();
            if (LEVEL_L1.equals(level)) {
                l1HitCount.incrementAndGet();
            } else if (LEVEL_L2.equals(level)) {
                l2HitCount.incrementAndGet();
            }
            recordKeyAccess(cacheName, key, 0);
        }

        @Override
        public void recordMiss(String cacheName, String key) {
            missCount.incrementAndGet();
            recordKeyAccess(cacheName, key, 0);
        }

        @Override
        public void recordPenetration(String cacheName, String key) {
            penetrationCount.incrementAndGet();
            recordKeyAccess(cacheName, key, 0);
        }

        @Override
        public void recordGet(String cacheName, String key) {
            getCount.incrementAndGet();
        }

        @Override
        public void recordPut(String cacheName, String key, Object value) {
            putCount.incrementAndGet();
            lastPutKey = key;
            long valueSize = estimateValueSize(value);
            recordKeyAccess(cacheName, key, valueSize);
        }

        @Override
        public void recordEvict(String cacheName, String key) {
            evictCount.incrementAndGet();
            lastEvictKey = key;
        }

        @Override
        public void recordCacheSize(String cacheName, long size) {
            cacheSizeCount.incrementAndGet();
        }

        @Override
        public void recordKeyAccess(String cacheName, String key, long valueSize) {
            keyAccessCount.incrementAndGet();
            accessKeys.add(key);
        }

        @Override
        public List<KeyStat> getHotKeyRanking(String cacheName, int topN) {
            return new ArrayList<>();
        }

        @Override
        public List<KeyStat> getBigKeyRanking(String cacheName, int topN) {
            return new ArrayList<>();
        }

        public void clear() {
            hitCount.set(0);
            l1HitCount.set(0);
            l2HitCount.set(0);
            missCount.set(0);
            penetrationCount.set(0);
            getCount.set(0);
            putCount.set(0);
            evictCount.set(0);
            cacheSizeCount.set(0);
            keyAccessCount.set(0);
            accessKeys.clear();
            lastPutKey = null;
            lastEvictKey = null;
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
}
