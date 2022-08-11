package com.jd.platform.hotkey.client.core.key;

import com.jd.platform.hotkey.client.Context;
import com.jd.platform.hotkey.client.core.rule.KeyRuleHolder;
import com.jd.platform.hotkey.client.etcd.EtcdConfigFactory;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.typeenum.KeyType;
import com.jd.platform.hotkey.common.tool.Constant;
import com.jd.platform.hotkey.common.tool.HotKeyPathTool;

/**
 * 客户端上传热key的入口调用
 *
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class HotKeyPusher {

    public static void push(String key, KeyType keyType, int count, boolean remove) {
        if (count <= 0) {
            count = 1;
        }
        if (keyType == null) {
            keyType = KeyType.REDIS_KEY;
        }
        if (key == null) {
            return;
        }
        HotKeyModel hotKeyModel = new HotKeyModel();
        hotKeyModel.setAppName(Context.APP_NAME);
        hotKeyModel.setKeyType(keyType);
        hotKeyModel.setCount(count);
        hotKeyModel.setRemove(remove);
        hotKeyModel.setKey(key);


        if (remove) {
            //如果是删除key，就直接发到etcd去，不用做聚合。但是有点问题现在，这个删除只能删手工添加的key，不能删worker探测出来的
            //因为各个client都在监听手工添加的那个path，没监听自动探测的path。所以如果手工的那个path下，没有该key，那么是删除不了的。
            //删不了，就达不到集群监听删除事件的效果，怎么办呢？可以通过新增的方式，新增一个热key，然后删除它
            EtcdConfigFactory.configCenter().putAndGrant(HotKeyPathTool.keyPath(hotKeyModel), Constant.DEFAULT_DELETE_VALUE, 1);
            EtcdConfigFactory.configCenter().delete(HotKeyPathTool.keyPath(hotKeyModel));
            //也删worker探测的目录
            EtcdConfigFactory.configCenter().delete(HotKeyPathTool.keyRecordPath(hotKeyModel));
        } else {
            //如果key是规则内的要被探测的key，就积累等待传送
            if (KeyRuleHolder.isKeyInRule(key)) {
                //积攒起来，等待每半秒发送一次
                KeyHandlerFactory.getCollector().collect(hotKeyModel);
            }
        }
    }

    public static void push(String key, KeyType keyType, int count) {
        push(key, keyType, count, false);
    }

    public static void push(String key, KeyType keyType) {
        push(key, keyType, 1, false);
    }

    public static void push(String key) {
        push(key, KeyType.REDIS_KEY, 1, false);
    }

    public static void remove(String key) {
        push(key, KeyType.REDIS_KEY, 1, true);
    }
}
