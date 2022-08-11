package com.jd.platform.hotkey.dashboard.common.domain.req;

import com.jd.platform.hotkey.dashboard.util.DateUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author liyunfeng31
 */
public class ChartReq implements Serializable {


    private Date startTime;

    private Date endTime;

    private String appName;

    private Integer limit;

    private String key;

    private Integer threshold;

    public ChartReq() {
    }

    public ChartReq(LocalDateTime st, LocalDateTime et, Integer limit) {
        this.startTime = DateUtil.ldtToDate(st);
        this.endTime = DateUtil.ldtToDate(et);
        this.limit = limit;
    }

    public ChartReq(Date st, Date et, String appName, String key) {
        this.startTime = st;
        this.endTime = et;
        this.appName = appName;
        this.key = key;
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

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
}
