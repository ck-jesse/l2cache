package com.github.jesse.l2cache;

import com.github.jesse.l2cache.spi.SPI;

import java.io.Serializable;
import java.util.List;

/**
 * 热key自动识别
 *
 * @author zengjucai
 * @date 2021/6/10 13:45
 */
@SPI("sentinel")
public interface HotkeyService extends Serializable {

    /**
     * 初始化
     */
    void init(L2CacheConfig.Hotkey hotkey, List<String> cacheNameList);

    /**
     * 是否为热key
     *
     * @return 返回 true 表示热key
     */
    boolean isHotkey(String level1CacheType, String cacheName, String key);

    default void setInstanceId(String instanceId) {

    }

    default void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {

    }

}
