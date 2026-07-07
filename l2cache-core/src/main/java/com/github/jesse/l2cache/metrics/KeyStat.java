package com.github.jesse.l2cache.metrics;

import lombok.Data;

import java.io.Serializable;

/**
 * key 维度统计信息
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Data
public class KeyStat implements Serializable {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存 key
     */
    private String key;

    /**
     * 访问次数
     */
    private long accessCount;

    /**
     * value 大小（字节）
     */
    private long valueSize;
}
