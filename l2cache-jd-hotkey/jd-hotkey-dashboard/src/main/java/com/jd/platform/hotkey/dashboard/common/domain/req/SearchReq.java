package com.jd.platform.hotkey.dashboard.common.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jd.platform.hotkey.dashboard.util.DateUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @ProjectName: hotkey
 * @ClassName: SearchParam
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/16 21:03
 */
public class SearchReq implements Serializable {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date endTime;

    private Integer status;

    private Integer type;

    private String app;

    private String key;

    public SearchReq() {
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public SearchReq(LocalDateTime st) {
        this.startTime = DateUtil.ldtToDate(st);
        this.endTime = new Date();
    }
}
