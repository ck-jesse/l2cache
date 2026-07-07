package com.github.jesse.l2cache.test;

import com.github.jesse.l2cache.metrics.KeyAccessStats;
import com.github.jesse.l2cache.metrics.KeyStat;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * KeyAccessStats 测试
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
public class KeyAccessStatsTest {

    @Test
    public void testHotKeyRanking() {
        KeyAccessStats stats = new KeyAccessStats(100, 60, 1024);
        stats.recordAccess("cache1", "k1", 100);
        stats.recordAccess("cache1", "k2", 200);
        stats.recordAccess("cache1", "k2", 200);
        stats.recordAccess("cache1", "k3", 300);
        stats.recordAccess("cache1", "k3", 300);
        stats.recordAccess("cache1", "k3", 300);

        List<KeyStat> ranking = stats.getHotKeyRanking("cache1", 2);
        assertEquals(2, ranking.size());
        assertEquals("k3", ranking.get(0).getKey());
        assertEquals(3, ranking.get(0).getAccessCount());
        assertEquals("k2", ranking.get(1).getKey());
        assertEquals(2, ranking.get(1).getAccessCount());
        stats.shutdown();
    }

    @Test
    public void testBigKeyRanking() {
        KeyAccessStats stats = new KeyAccessStats(100, 60, 1024);
        stats.recordAccess("cache1", "k1", 512);
        stats.recordAccess("cache1", "k2", 2048);
        stats.recordAccess("cache1", "k3", 4096);

        List<KeyStat> ranking = stats.getBigKeyRanking("cache1", 10);
        assertEquals(2, ranking.size());
        assertEquals("k3", ranking.get(0).getKey());
        assertEquals(4096, ranking.get(0).getValueSize());
        assertEquals("k2", ranking.get(1).getKey());
        assertEquals(2048, ranking.get(1).getValueSize());
        stats.shutdown();
    }

    @Test
    public void testEmptyRanking() {
        KeyAccessStats stats = new KeyAccessStats(100, 60, 1024);
        List<KeyStat> ranking = stats.getHotKeyRanking("cache1", 10);
        assertTrue(ranking.isEmpty());
        stats.shutdown();
    }
}
