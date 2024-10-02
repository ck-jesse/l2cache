package com.github.jesse.l2cache.hotkey;

import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.HotkeyService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NoneHotkeyService implements HotkeyService {

    @Override
    public void init(CacheConfig.Hotkey hotKeyConfig, List<String> cacheNameList) {

    }

    @Override
    public boolean isHotkey(String level1CacheType, String cacheName, String key) {
        return false;
    }
}
