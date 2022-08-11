package com.coy.l2cache.jd.hotkey.client.core.key;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyModel;
import com.coy.l2cache.jd.hotkey.common.model.KeyCountModel;

/**
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class KeyHandlerFactory {
    private static final DefaultKeyHandler iKeyHandler = new DefaultKeyHandler();

    private KeyHandlerFactory() {
    }

    public static IKeyPusher getPusher() {
        return iKeyHandler.keyPusher();
    }

    public static IKeyCollector<HotKeyModel, HotKeyModel> getCollector() {
        return iKeyHandler.keyCollector();
    }

    public static IKeyCollector<KeyHotModel, KeyCountModel> getCounter() {
        return iKeyHandler.keyCounter();
    }
}
