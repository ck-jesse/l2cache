package com.jd.platform.hotkey.dashboard.common.domain.vo;

/**
 * @ProjectName: hotkey
 * @ClassName: ClearCfgVo
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/8/3 9:54
 */
public class ClearCfgVo {

    private String app;

    private String ttl;

    private Long version;

    public ClearCfgVo(String app, String ttl, Long version) {
        this.app = app;
        this.ttl = ttl;
        this.version = version;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
