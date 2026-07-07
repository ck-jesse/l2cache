package com.github.jesse.l2cache.spring.consistency;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 单个实例的一致性检测快照
 *
 * @author chenck
 * @date 2026/7/6 11:00
 */
@Data
public class InstanceSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 实例 id
     */
    private String instanceId;

    /**
     * key 数量（count 模式下使用）
     */
    private long keyCount;

    /**
     * key -> valueHash（value 模式下使用）
     */
    private Map<String, String> valueHashes = new HashMap<>();

    /**
     * 是否超时未上报
     */
    private boolean timeout;
}
