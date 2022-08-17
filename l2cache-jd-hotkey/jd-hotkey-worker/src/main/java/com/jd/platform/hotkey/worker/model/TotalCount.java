package com.jd.platform.hotkey.worker.model;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-20
 */
public class TotalCount {
    private long totalReceiveCount;
    private long totalDealCount;

    public TotalCount(long totalReceiveCount, long totalDealCount) {
        this.totalReceiveCount = totalReceiveCount;
        this.totalDealCount = totalDealCount;
    }

    public long getTotalReceiveCount() {
        return totalReceiveCount;
    }

    public void setTotalReceiveCount(long totalReceiveCount) {
        this.totalReceiveCount = totalReceiveCount;
    }

    public long getTotalDealCount() {
        return totalDealCount;
    }

    public void setTotalDealCount(long totalDealCount) {
        this.totalDealCount = totalDealCount;
    }
}
