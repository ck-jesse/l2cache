package com.jd.platform.hotkey.common.model;

import com.jd.platform.hotkey.common.tool.IdGenerater;

/**
 * 热key的定义
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class BaseModel {
    private String id = IdGenerater.generateId();
    /**
     * 创建的时间
     */
    private long createTime;
    /**
     * key的名字
     */
    private String key;
    /**
     * 该key出现的数量，如果一次一发那就是1，累积多次发那就是count
     */
    private int count;

    @Override
    public String toString() {
        return "BaseModel{" +
                "id='" + id + '\'' +
                ", createTime=" + createTime +
                ", key='" + key + '\'' +
                ", count=" + count +
                '}';
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
