package com.jd.platform.hotkey.client.callback;

import com.jd.platform.hotkey.client.core.rule.KeyRuleHolder;
import com.jd.platform.hotkey.common.tool.Constant;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-07
 */
public class ValueModel {
    /**
     * 该热key创建时间
     */
    private long createTime = System.currentTimeMillis();
    /**
     * 本地缓存时间，单位毫秒
     */
    private int duration;
    /**
     * 用户实际存放的value
     */
    private Object value;

    public static ValueModel defaultValue(String key) {
        ValueModel valueModel = new ValueModel();
        int duration = KeyRuleHolder.duration(key);
        if (duration == 0) {
            //不符合任何规则
            return null;
        }
        //转毫秒
        valueModel.setDuration(duration * 1000);
        valueModel.setValue(Constant.MAGIC_NUMBER);
        return valueModel;
    }


    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
