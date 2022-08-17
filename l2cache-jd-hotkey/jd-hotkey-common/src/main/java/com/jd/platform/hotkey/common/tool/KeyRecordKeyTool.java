package com.jd.platform.hotkey.common.tool;

import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-26
 */
public class KeyRecordKeyTool {
    /**
     * 热key记录，在etcd里的value存放的值
     */
    public static String key(HotKeyModel hotKeyModel) {
        return hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
    }
}
