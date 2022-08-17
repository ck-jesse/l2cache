package com.jd.platform.hotkey.worker.keylistener;

import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * @author wuweifeng wrote on 2019-12-06
 * @version 1.0
 */
public interface IKeyListener {
    /**
     * 新来一个key
     */
    void newKey(HotKeyModel hotKeyModel, KeyEventOriginal orignal);

    /**
     * 删除一个key。（一种是客户端发消息删，二种是本地线程扫描过期的删，三种是etcd里删）
     */
    void removeKey(HotKeyModel hotKeyModel, KeyEventOriginal orignal);
}
