package com.jd.platform.hotkey.client.core.key;

import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.KeyCountModel;

import java.util.List;

/**
 * 客户端上传热key到worker接口
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public interface IKeyPusher {
    /**
     * 发送待测key
     */
    void send(String appName, List<HotKeyModel> list);

    /**
     * 发送热key访问量
     */
    void sendCount(String appName, List<KeyCountModel> list);
}
