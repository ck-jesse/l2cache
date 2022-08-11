package com.jd.platform.hotkey.worker.netty.pusher.store;

import com.jd.platform.hotkey.common.model.HotKeyModel;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 已热待上报入库的热key集中营
 * @author wuweifeng
 * @version 1.0
 * @date 2020-08-31
 */
public class HotkeyTempStore {
    /**
     * 热key集中营
     */
    private static LinkedBlockingQueue<HotKeyModel> hotKeyStoreQueue = new LinkedBlockingQueue<>();

    public static void push(HotKeyModel model) {
        hotKeyStoreQueue.offer(model);
    }

    public static LinkedBlockingQueue<HotKeyModel> getQueue() {
        return hotKeyStoreQueue;
    }

}
