package com.jd.platform.hotkey.worker.netty.holder;

import java.util.HashSet;
import java.util.Set;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-28
 */
public class WhiteListHolder {
    public static Set<String> whiteList = new HashSet<>();

    public static void add(String s) {
        whiteList.add(s.trim());
    }

    public static boolean contains(String s) {
        return whiteList.contains(s);
    }
}
