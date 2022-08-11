package cn.weeget.hotkey.client.core.rule;

import cn.weeget.hotkey.common.rule.KeyRule;

import java.util.List;

/**
 * @author wuweifeng wrote on 2020-02-26
 * @version 1.0
 */
public class KeyRuleInfoChangeEvent {
    private List<KeyRule> keyRules;

    public KeyRuleInfoChangeEvent(List<KeyRule> keyRules) {
        this.keyRules = keyRules;
    }

    public List<KeyRule> getKeyRules() {
        return keyRules;
    }

    public void setKeyRules(List<KeyRule> keyRules) {
        this.keyRules = keyRules;
    }
}
