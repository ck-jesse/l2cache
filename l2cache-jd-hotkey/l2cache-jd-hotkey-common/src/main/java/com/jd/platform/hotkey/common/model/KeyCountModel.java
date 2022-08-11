package com.jd.platform.hotkey.common.model;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-24
 */
public class KeyCountModel {
    /**
     * 对应的规则名，如 pin_2020-08-09 11:32:43
     */
    private String ruleKey;
    /**
     * 总访问次数
     */
    private int totalHitCount;
    /**
     * 热后访问次数
     */
    private int hotHitCount;
    /**
     * 发送时的时间
     */
    private long createTime;

    @Override
    public String toString() {
        return "KeyCountModel{" +
                "ruleKey='" + ruleKey + '\'' +
                ", totalHitCount=" + totalHitCount +
                ", hotHitCount=" + hotHitCount +
                ", createTime=" + createTime +
                '}';
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public int getTotalHitCount() {
        return totalHitCount;
    }

    public void setTotalHitCount(int totalHitCount) {
        this.totalHitCount = totalHitCount;
    }

    public int getHotHitCount() {
        return hotHitCount;
    }

    public void setHotHitCount(int hotHitCount) {
        this.hotHitCount = hotHitCount;
    }
}
