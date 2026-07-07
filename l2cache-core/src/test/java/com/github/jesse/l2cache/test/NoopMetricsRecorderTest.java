package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.metrics.KeyStat;
import com.github.jesse.l2cache.metrics.NoopMetricsRecorder;
import org.junit.Test;

import java.util.List;

import static com.github.jesse.l2cache.metrics.CacheMetrics.LEVEL_L1;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * NoopMetricsRecorder 测试
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
public class NoopMetricsRecorderTest {

    @Test
    public void testNoopReturnsEmptyList() {
        NoopMetricsRecorder recorder = new NoopMetricsRecorder();
        recorder.recordHit("cache1", "k1", LEVEL_L1);
        recorder.recordMiss("cache1", "k2");
        recorder.recordPut("cache1", "k3", "value");
        recorder.recordEvict("cache1", "k1");

        List<KeyStat> hotKeys = recorder.getHotKeyRanking("cache1", 10);
        List<KeyStat> bigKeys = recorder.getBigKeyRanking("cache1", 10);

        assertNotNull(hotKeys);
        assertTrue(hotKeys.isEmpty());
        assertNotNull(bigKeys);
        assertTrue(bigKeys.isEmpty());
    }
}
