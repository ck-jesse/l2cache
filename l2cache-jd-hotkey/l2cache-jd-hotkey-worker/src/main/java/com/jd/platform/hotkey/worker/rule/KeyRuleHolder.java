package com.jd.platform.hotkey.worker.rule;

import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.DefaultKeyRule;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.worker.cache.CaffeineCacheHolder;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存各个app的rule信息
 *
 * @author wuweifeng wrote on 2019-12-12
 * @version 1.0
 */
public class KeyRuleHolder {
    /**
     * key就是appName，value是rule
     */
    private static final Map<String, List<KeyRule>> RULE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取key对应的rule规则
     */
    public static KeyRule getRuleByAppAndKey(HotKeyModel hotKeyModel) {
        List<KeyRule> keyRules = RULE_MAP.get(hotKeyModel.getAppName());
        //没有该key相关信息时，返回默认
        if (CollectionUtils.isEmpty(keyRules)) {
            return new DefaultKeyRule().getKeyRule();
        }

        KeyRule prefix = null;
        KeyRule common = null;

        //遍历该app的所有rule，找到与key匹配的rule。优先全匹配->prefix匹配-> * 通配
        for (KeyRule keyRule : keyRules) {
            if (hotKeyModel.getKey().equals(keyRule.getKey())) {
                return keyRule;
            }
            if (keyRule.isPrefix() && hotKeyModel.getKey().startsWith(keyRule.getKey())) {
                prefix = keyRule;
            }
            if ("*".equals(keyRule.getKey())) {
                common = keyRule;
            }
        }
        if (prefix != null) {
            return prefix;
        }
        if (common != null) {
            return common;
        }

        return new DefaultKeyRule().getKeyRule();
    }

    /**
     * 判断新取的rules和已有的是否一样
     */
    public static void put(String appName, List<KeyRule> keyRules) {
        if (RULE_MAP.get(appName) == null) {
            RULE_MAP.put(appName, keyRules);
            return;
        }
        if (keyRules.toString().equals(RULE_MAP.get(appName).toString())) {
            return;
        }
        //判断该APP的rule是否有变化，如果有变化了，则需要清空该app的caffeine缓存。
        RULE_MAP.put(appName, keyRules);

        CaffeineCacheHolder.clearCacheByAppName(appName);
    }

}
