package com.coy.l2cache.jd.hotkey.client.core.key;

import com.coy.l2cache.jd.hotkey.client.etcd.EtcdConfigFactory;
import com.coy.l2cache.jd.hotkey.client.log.JdLogger;

/**
 * 删除某个key
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-16
 */
public class KeyRemover {

    public static void remove(String key) {
        try {
            EtcdConfigFactory.configCenter().delete(key);
        } catch (Exception e) {
            JdLogger.error(KeyRemover.class, "remove key error");
        }
    }
}
