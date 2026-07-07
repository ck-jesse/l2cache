package com.github.jesse.l2cache.spring.consistency;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 一致性检测任务
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Data
public class ConsistencyTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 id
     */
    private String taskId;

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 检测模式：count / value
     */
    private String mode;

    /**
     * 指定对比的 key 列表（value 模式下使用）
     */
    private List<String> keys;

    /**
     * 采样大小（value 模式下未指定 key 时使用）
     */
    private int sampleSize;

    public ConsistencyTask() {
    }

    public ConsistencyTask(String taskId, String cacheName, String mode, List<String> keys, int sampleSize) {
        this.taskId = taskId;
        this.cacheName = cacheName;
        this.mode = mode;
        this.keys = keys;
        this.sampleSize = sampleSize;
    }
}
