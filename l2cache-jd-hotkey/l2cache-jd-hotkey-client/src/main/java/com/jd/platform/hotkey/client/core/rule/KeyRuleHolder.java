package com.jd.platform.hotkey.client.core.rule;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.eventbus.Subscribe;
import com.jd.platform.hotkey.client.cache.CacheFactory;
import com.jd.platform.hotkey.client.cache.LocalCache;
import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.common.rule.KeyRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 保存key的规则
 *
 * @author wuweifeng wrote on 2020-02-26
 * @version 1.0
 */
public class KeyRuleHolder {

    /**
     * 保存超时时间和caffeine的映射，key是超时时间，value是caffeine
     */
    private static final ConcurrentHashMap<Integer, LocalCache> RULE_CACHE_MAP = new ConcurrentHashMap<>();

    private static final List<KeyRule> KEY_RULES = new ArrayList<>();

    /**
     * 所有的规则，如果规则的超时时间变化了，会重建caffeine
     */
    public static void putRules(List<KeyRule> keyRules) {
        synchronized (KEY_RULES) {
            //如果规则为空，清空规则表
            if (CollectionUtil.isEmpty(keyRules)) {
                KEY_RULES.clear();
                RULE_CACHE_MAP.clear();
                return;
            }

            KEY_RULES.clear();
            KEY_RULES.addAll(keyRules);

            Set<Integer> durationSet = keyRules.stream().map(KeyRule::getDuration).collect(Collectors.toSet());
            for (Integer duration : RULE_CACHE_MAP.keySet()) {
                //先清除掉那些在RULE_CACHE_MAP里存的，但是rule里已没有的
                if (!durationSet.contains(duration)) {
                    RULE_CACHE_MAP.remove(duration);
                }
            }

            //遍历所有的规则
            for (KeyRule keyRule : keyRules) {
                int duration = keyRule.getDuration();
                if (RULE_CACHE_MAP.get(duration) == null) {
                    LocalCache cache = CacheFactory.build(duration);
                    RULE_CACHE_MAP.put(duration, cache);
                }
            }
        }
    }

    /**
     * 根据key返回对应的LocalCache。
     * 譬如Rules里有多个
     * ｛key1 , 500｝
     * ｛key2 , 600｝
     * ｛*    , 700｝
     * 如果命中了key1，就直接返回。如果key1、key2都没命中，再去判断rule里是否有 * ，如果有 * 代表通配
     */
    public static LocalCache findByKey(String key) {
        if (StrUtil.isEmpty(key)) {
            return null;
        }
        KeyRule keyRule = findRule(key);
        if (keyRule == null) {
            return null;
        }
        return RULE_CACHE_MAP.get(keyRule.getDuration());

    }

    /**
     * 判断该key命中了哪个rule
     */
    public static String rule(String key) {
        KeyRule keyRule = findRule(key);
        if (keyRule != null) {
            return keyRule.getKey();
        }
        return "";
    }

    /**
     * 获取该key应该缓存多久
     */
    public static int duration(String key) {
        KeyRule keyRule = findRule(key);
        if (keyRule != null) {
            return keyRule.getDuration();
        }
        return 0;
    }

    //遍历该app的所有rule，找到与key匹配的rule。优先全匹配->prefix匹配-> * 通配
    //这一段虽然看起来比较奇怪，但是没毛病，不要乱改
    private static KeyRule findRule(String key) {
        KeyRule prefix = null;
        KeyRule common = null;
        for (KeyRule keyRule : KEY_RULES) {
            if (key.equals(keyRule.getKey())) {
                return keyRule;
            }
            if ((keyRule.isPrefix() && key.startsWith(keyRule.getKey()))) {
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

    /**
     * 判断key是否在配置的要探测的规则内
     */
    public static boolean isKeyInRule(String key) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        //遍历该app的所有rule，找到与key匹配的rule。
        for (KeyRule keyRule : KEY_RULES) {
            if ("*".equals(keyRule.getKey()) || key.equals(keyRule.getKey()) ||
                    (keyRule.isPrefix() && key.startsWith(keyRule.getKey()))) {
                return true;
            }
        }
        return false;
    }


    @Subscribe
    public void ruleChange(KeyRuleInfoChangeEvent event) {
        JdLogger.info(getClass(), "new rules info is :" + event.getKeyRules());
        List<KeyRule> ruleList = event.getKeyRules();
        if (ruleList == null) {
            return;
        }

        putRules(ruleList);
    }
}
