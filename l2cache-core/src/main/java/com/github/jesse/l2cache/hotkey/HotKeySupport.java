package com.github.jesse.l2cache.hotkey;

import cn.hutool.core.util.ObjectUtil;
import com.github.jesse.l2cache.HotkeyService;
import com.github.jesse.l2cache.consts.HotkeyType;
import com.github.jesse.l2cache.spi.ServiceLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 热key的工具类：门面
 *
 * @author chenck
 * @date 2023/12/6 16:02
 */
@Slf4j
public class HotKeySupport {

    /**
     * 统一的入口：判断是否为热key
     */
    public static boolean isHotkey(String hotkeyType, String cacheName, String key) {
        // 热key类型为空，则直接返回
        if (ObjectUtil.isEmpty(hotkeyType)) {
            return false;
        }
        // 没有配置热key识别，则直接返回
        if (HotkeyType.NONE.name().equalsIgnoreCase(hotkeyType)) {
            return false;
        }
        if (ObjectUtil.isEmpty(cacheName) || ObjectUtil.isEmpty(key)) {
            return false;
        }
        HotkeyService hotkeyService = ServiceLoader.load(HotkeyService.class, hotkeyType);
        if (ObjectUtil.isNull(hotkeyService)) {
            log.error("非法的 hotkeyType,无匹配的HotKey实现类, hotkeyType={}", hotkeyType);
            return false;
        }
        return hotkeyService.isHotkey(cacheName, key);
    }
}
