package com.github.jesse.l2cache.spring.consistency;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一致性检测结果
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Data
public class ConsistencyResult implements Serializable {

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
     * 检测模式
     */
    private String mode;

    /**
     * 各实例快照：<instanceId, InstanceSnapshot>
     */
    private Map<String, InstanceSnapshot> instanceSnapshots = new HashMap<>();

    /**
     * L2 基准快照
     */
    private InstanceSnapshot l2Snapshot = new InstanceSnapshot();

    /**
     * 差异报告
     */
    private List<String> diffReport = new ArrayList<>();

    /**
     * 是否完成（所有实例均上报）
     */
    private boolean completed;
}
