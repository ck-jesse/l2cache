package cn.weeget.hotkey.client.core.key;

import cn.weeget.hotkey.common.model.HotKeyModel;
import cn.weeget.hotkey.common.model.KeyCountModel;

/**
 * @author wuweifeng wrote on 2020-02-25
 * @version 1.0
 */
public class DefaultKeyHandler {
    private IKeyPusher iKeyPusher = new NettyKeyPusher();

    private IKeyCollector<HotKeyModel, HotKeyModel> iKeyCollector = new TurnKeyCollector();

    private IKeyCollector<KeyHotModel, KeyCountModel> iKeyCounter = new TurnCountCollector();


    public IKeyPusher keyPusher() {
        return iKeyPusher;
    }

    public IKeyCollector<HotKeyModel, HotKeyModel> keyCollector() {
        return iKeyCollector;
    }

    public IKeyCollector<KeyHotModel, KeyCountModel> keyCounter() {
        return iKeyCounter;
    }
}
