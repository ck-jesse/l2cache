package com.jd.platform.hotkey.common.model;

import com.jd.platform.hotkey.common.model.typeenum.KeyType;

/**
 * 热key的定义
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class HotKeyModel extends BaseModel {
    /**
     * 来自于哪个应用
     */
    private String appName;
    /**
     * key的类型（譬如是接口、热用户、redis的key等）
     */
    private KeyType keyType;
    /**
     * 是否是删除事件
     */
    private boolean remove;

    @Override
    public String toString() {
        return "appName:" + appName + "-key=" + getKey();
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

}
