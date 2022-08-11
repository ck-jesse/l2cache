package com.jd.platform.hotkey.dashboard.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author liyunfeng31
 */
public class Rules implements Serializable {

    private Integer id;

    private String rules;

    private String app;

    private String updateUser;

    private Date updateTime;

    private Integer version;

    public Rules() {
    }

    public Rules(String app, String rules) {
        this.rules = rules;
        this.app = app;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
