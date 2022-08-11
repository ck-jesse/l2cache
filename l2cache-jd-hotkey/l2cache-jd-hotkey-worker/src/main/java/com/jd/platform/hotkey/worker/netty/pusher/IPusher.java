package com.jd.platform.hotkey.worker.netty.pusher;

import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public interface IPusher {
    void push(HotKeyModel model);

    /**
     * worker是监听不到删除事件了，client不往worker发删除事件了
     */
    @Deprecated
    void remove(HotKeyModel model);
}
