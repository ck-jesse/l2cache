package com.jd.platform.hotkey.client.callback;

import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
public class ReceiveNewKeyEvent {
    private HotKeyModel model;

    public ReceiveNewKeyEvent(HotKeyModel model) {
        this.model = model;
    }

    public HotKeyModel getModel() {
        return model;
    }

    public void setModel(HotKeyModel model) {
        this.model = model;
    }
}
