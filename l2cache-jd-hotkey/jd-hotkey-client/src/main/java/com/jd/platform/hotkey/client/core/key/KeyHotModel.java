package com.jd.platform.hotkey.client.core.key;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-24
 */
public class KeyHotModel {
    private String key;
    private boolean isHot;

    public KeyHotModel(String key, boolean isHot) {
        this.key = key;
        this.isHot = isHot;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isHot() {
        return isHot;
    }

    public void setHot(boolean hot) {
        isHot = hot;
    }
}
