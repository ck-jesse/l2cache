package com.jd.platform.hotkey.dashboard.model;


import java.io.Serializable;

public class Rule implements Serializable {

    /**
     * key的前缀，也可以完全和key相同。为"*"时代表通配符
     */
    private String key;
    /**
     * 是否是前缀，true是前缀
     */
    private Boolean prefix;
    /**
     * 间隔时间（秒）
     */
    private Integer interval;
    /**
     * 累计数量
     */
    private Integer threshold;
    /**
     * 变热key后，本地、etcd缓存它多久。单位（秒），默认60
     */
    private Integer duration;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean getPrefix() {
        return prefix;
    }

    public void setPrefix(Boolean prefix) {
        this.prefix = prefix;
    }

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}