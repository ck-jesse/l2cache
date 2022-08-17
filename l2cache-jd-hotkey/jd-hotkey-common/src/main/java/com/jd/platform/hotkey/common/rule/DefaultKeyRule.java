package com.jd.platform.hotkey.common.rule;

/**
 * 对key的字符串是否命中的规则，以及该key判断是否热key的频率，如2秒500次
 *
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
public class DefaultKeyRule implements IKeyRule {

    @Override
    public KeyRule getKeyRule() {
        return new KeyRule.Builder().key("*").duration(60).prefix(false).interval(5).threshold(1000).build();
    }

}
