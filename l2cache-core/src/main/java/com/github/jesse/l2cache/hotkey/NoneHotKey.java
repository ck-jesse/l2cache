package com.github.jesse.l2cache.hotkey;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.HotKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NoneHotKey implements HotKey {

    @Override
    public void init(CacheConfig.HotKey hotKeyConfig, List<String> cacheNameList) {

    }

    @Override
    public boolean isHotKey(String cacheName, String key) {
        return false;
    }
}
