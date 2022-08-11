package com.jd.platform.hotkey.dashboard.util;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.jd.platform.hotkey.common.rule.KeyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-05-29
 */
public class RuleUtil {
    private static final ConcurrentHashMap<String, List<KeyRule>> RULE_MAP = new ConcurrentHashMap<>();

    private static Logger logger = LoggerFactory.getLogger("RuleUtil");

    public static void init() {
        synchronized (RULE_MAP) {
            RULE_MAP.clear();
        }
    }

    public static void put(String appName, List<KeyRule> list) {
        synchronized (RULE_MAP) {
            logger.info("更新了appName:{}  rule:{}",appName, JSON.toJSONString(list));
            RULE_MAP.put(appName, list);
        }
    }

    /**
     * 根据APP的key，获取该key对应的rule.如 cartpc-pu__
     */
    public static String rule(String key) {
        try {
            KeyRule keyRule = findByKey(key);
            if (keyRule != null) {
                String[] appKey = key.split("/");
                String appName = appKey[0];
                return appName + "-" + keyRule.getKey();
            } else {
                logger.info("rule is null，key is " + key);
            }
        }catch (Exception e){
            logger.error("findByKey error",e);
        }
        return "";
    }

    /**
     * 根据APP的key，获取该key对应的rule的desc
     */
    public static String ruleDesc(String key) {
        KeyRule keyRule = findByKey(key);
        if (keyRule != null) {
            return keyRule.getDesc();
        }
        return "";
    }

    public static KeyRule findByKey(String appNameKey) {
        synchronized (RULE_MAP) {
            if (StrUtil.isEmpty(appNameKey)) {
                return null;
            }
            String[] appKey = appNameKey.split("/");
            String appName = appKey[0];
            String realKey = appKey[1];
            KeyRule prefix = null;
            KeyRule common = null;

            if (RULE_MAP.get(appName) == null) {
                return null;
            }
            //遍历该app的所有rule，找到与key匹配的rule。优先全匹配->prefix匹配-> * 通配
            //这一段虽然看起来比较奇怪，但是没毛病，不要乱改
            for (KeyRule keyRule : RULE_MAP.get(appName)) {
                if (realKey.equals(keyRule.getKey())) {
                    return keyRule;
                }
                if ((keyRule.isPrefix() && realKey.startsWith(keyRule.getKey()))) {
                    prefix = keyRule;
                }
                if ("*".equals(keyRule.getKey())) {
                    common = keyRule;
                }
            }

            if (prefix != null) {
                return prefix;
            }
            return common;
        }

    }
}
