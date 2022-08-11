package com.jd.platform.hotkey.client.callback;

import com.google.common.eventbus.Subscribe;
import com.jd.platform.hotkey.common.model.HotKeyModel;


/**
 * 监听有新key推送事件
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
public class ReceiveNewKeySubscribe {

    private ReceiveNewKeyListener receiveNewKeyListener = new DefaultNewKeyListener();

    @Subscribe
    public void newKeyComing(ReceiveNewKeyEvent event) {
        HotKeyModel hotKeyModel = event.getModel();
        if (hotKeyModel == null) {
            return;
        }
        //收到新key推送
        if (receiveNewKeyListener != null) {
            receiveNewKeyListener.newKey(hotKeyModel);
        }
    }

}
