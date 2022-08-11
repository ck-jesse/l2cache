package com.jd.platform.hotkey.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public class KeyRecord implements Serializable {

    private Long id;

    private String key;

    private String appName;

    private String val;

    /**
     * 缓存时间
     */
    private int duration;

    /**
     * 来源： SYSTEM 系统探测；USERNAME创建人
     */
    private String source;

    /**
     * 事件类型： 0 PUT； 1 删除
     */
    private Integer type;

    private Date createTime;

    private String uuid;
    /**
     * 该记录是哪个rule下的，如 /app1/uid__  将来会按rule对keyRecord进行分组做图表展示
     */
    private String rule;
    /**
     * 该rule的描述
     */
    private transient String ruleDesc;

    public KeyRecord() {
    }

    public KeyRecord(String key,String val, String appName, int duration,
                     String source, Integer type,String uuid, Date createTime) {
        this.key = key;
        this.val = val;
        this.appName = appName;
        this.duration = duration;
        this.source = source;
        this.type = type;
        this.uuid = uuid;
        this.createTime = createTime;
    }

    public String getRuleDesc() {
        return ruleDesc;
    }

    public void setRuleDesc(String ruleDesc) {
        this.ruleDesc = ruleDesc;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? null : key.trim();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}