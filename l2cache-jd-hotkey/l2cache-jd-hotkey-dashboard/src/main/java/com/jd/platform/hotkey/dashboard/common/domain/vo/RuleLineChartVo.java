package com.jd.platform.hotkey.dashboard.common.domain.vo;

/**
 * @author liyunfeng31
 */
public class RuleLineChartVo {

    private String rule;

    private Integer count;

    public RuleLineChartVo() {
    }

    public RuleLineChartVo(String rule, Integer count) {
        this.rule = rule;
        this.count = count;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
