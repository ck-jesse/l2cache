package com.jd.platform.hotkey.common.tool;

import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class HotKeyPathTool {
    /**
     * app的热key存放地址，client会监听该地址，当有热key变化时会响应
     */
    public static String keyPath(HotKeyModel hotKeyModel) {
        return ConfigConstant.hotKeyPath + hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
    }

    /**
     * worker将热key推送到该地址，供dashboard监听入库做记录
     */
    public static String keyRecordPath(HotKeyModel hotKeyModel) {
        return ConfigConstant.hotKeyRecordPath + hotKeyModel.getAppName() + "/" + hotKeyModel.getKey();
    }
}
