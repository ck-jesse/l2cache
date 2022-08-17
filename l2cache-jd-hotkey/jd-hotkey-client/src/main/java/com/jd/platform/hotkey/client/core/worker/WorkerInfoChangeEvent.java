package com.jd.platform.hotkey.client.core.worker;

import java.util.List;

/**
 * worker信息有变动
 * @author wuweifeng wrote on 2020-01-07
 * @version 1.0
 */
public class WorkerInfoChangeEvent {
    private List<String> addresses;

    public WorkerInfoChangeEvent(List<String> addresses) {
        this.addresses = addresses;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }
}
