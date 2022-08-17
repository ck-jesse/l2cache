package com.jd.platform.hotkey.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public class ChangeLog implements Serializable {
    private Integer id;

    private String key;

    private Integer bizType;

    private String from;

    private String to;

    private String appName;

    private String updateUser;

    private Date createTime;

    private String uuid;

    public ChangeLog() {
    }

    public ChangeLog(String key, Integer bizType, String from, String to, String updateUser,String uuid) {
        this.key = key;
        this.bizType = bizType;
        this.from = from;
        this.to = to;
        this.updateUser = updateUser;
        this.createTime = new Date();
        this.uuid = uuid;
    }

    public ChangeLog(String key, Integer bizType, String from, String to, String updateUser, String appName,String uuid) {
        this.key = key;
        this.bizType = bizType;
        this.from = from;
        this.to = to;
        this.updateUser = updateUser;
        this.appName = appName;
        this.createTime = new Date();
        this.uuid = uuid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getBizType() {
        return bizType;
    }

    public void setBizType(Integer bizType) {
        this.bizType = bizType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from == null ? null : from.trim();
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to == null ? null : to.trim();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser == null ? null : updateUser.trim();
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