package com.github.jesse.l2cache.consts;

/**
 * 热key类型
 *
 * @author zengjucai
 * @date 2020/7/2 12:06
 */
public enum HotkeyType {
    // 没有引入热key功能
    NONE,
    // 京东
    JD,
    ;

    public static HotkeyType getHotkeyType(String type) {
        HotkeyType[] types = HotkeyType.values();
        for (HotkeyType cacheType : types) {
            if (cacheType.name().equalsIgnoreCase(type)) {
                return cacheType;
            }
        }
        return null;
    }
}
