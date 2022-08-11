package com.jd.platform.hotkey.dashboard.model;

import java.util.Date;
import java.util.List;

public class User {
    private Integer id;

    private String nickName;

    private String userName;

    private String pwd;

    private String phone;

    private String role;

    private String appName;

    private Date createTime;

    private Integer state;

    private List<String> appNames;

    public User() {
    }


    public User(String role, String appName) {
        this.role = role;
        this.appName = appName;
    }

    public User(String role, List<String> appNames) {
        this.role = role;
        this.appNames = appNames;
    }

    public User(String role, List<String> appNames, String app) {
        this.role = role;
        this.appNames = appNames;
        this.appName = app;
    }

    public User(String userName, String role, String appName) {
        this.userName = userName;
        this.role = role;
        this.appName = appName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName == null ? null : nickName.trim();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName == null ? null : userName.trim();
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd == null ? null : pwd.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? null : phone.trim();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role == null ? null : role.trim();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public List<String> getAppNames() {
        return appNames;
    }

    public void setAppNames(List<String> appNames) {
        this.appNames = appNames;
    }
}