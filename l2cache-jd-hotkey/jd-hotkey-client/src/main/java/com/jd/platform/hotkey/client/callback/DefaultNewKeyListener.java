package com.jd.platform.hotkey.client.callback;

import com.jd.platform.hotkey.client.cache.CacheFactory;
import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.common.model.HotKeyModel;

/**
 * 收到来自于worker的新增key，或者etcd的新增和删除key事件
 *
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class DefaultNewKeyListener implements ReceiveNewKeyListener {

    @Override
    public void newKey(HotKeyModel hotKeyModel) {
        long now = System.currentTimeMillis();
        //如果key到达时已经过去1秒了，记录一下。手工删除key时，没有CreateTime
        if (hotKeyModel.getCreateTime() != 0 && Math.abs(now - hotKeyModel.getCreateTime()) > 1000) {
            JdLogger.warn(getClass(), "the key comes too late : " + hotKeyModel.getKey() + " now " +
                    +now + " keyCreateAt " + hotKeyModel.getCreateTime());
        }
        if (hotKeyModel.isRemove()) {
            //如果是删除事件，就直接删除
            deleteKey(hotKeyModel.getKey());
            return;
        }
        //已经是热key了，又推过来同样的热key，做个日志记录，并刷新一下
        if (JdHotKeyStore.isHot(hotKeyModel.getKey())) {
            JdLogger.warn(getClass(), "receive repeat hot key ：" + hotKeyModel.getKey() + " at " + now);
        }
        addKey(hotKeyModel.getKey());
    }

    private void addKey(String key) {
        ValueModel valueModel = ValueModel.defaultValue(key);
        if (valueModel == null) {
            //不符合任何规则
            deleteKey(key);
            return;
        }
        //如果原来该key已经存在了，那么value就被重置，过期时间也会被重置。如果原来不存在，就新增的热key
        JdHotKeyStore.setValueDirectly(key, valueModel);
    }


    private void deleteKey(String key) {
        CacheFactory.getNonNullCache(key).delete(key);
    }
}
