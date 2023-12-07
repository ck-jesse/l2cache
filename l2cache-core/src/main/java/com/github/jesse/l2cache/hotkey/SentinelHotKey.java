package com.github.jesse.l2cache.hotkey;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.github.jesse.l2cache.CacheConfig;
import com.github.jesse.l2cache.HotKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * sentinel热key探测
 *
 * @author chenck
 * @date 2023/12/6 15:37
 */
@Slf4j
public class SentinelHotKey implements HotKey {

    @Override
    public void init(CacheConfig.HotKey hotKeyConfig, List<String> cacheNameList) {
        ParamFlowRule defaultRule = hotKeyConfig.getSentinel().getDefaultRule();
        List<ParamFlowRule> rules = hotKeyConfig.getSentinel().getParamFlowRules();

        // 若配置了默认规则，则针对所有的cacheName，生成其默认的热点参数规则
        // 若未配置默认规则，则仅针对 paramFlowRules 中的配置进行热点参数探测
        if (null != defaultRule) {
            for (String cacheName : cacheNameList) {
                // 检查是否使用默认的热点参数规则
                boolean useDefaultRule = true;
                for (ParamFlowRule rule : rules) {
                    if (rule.getResource().equals(cacheName)) {
                        useDefaultRule = false;
                        break;
                    }
                }

                // 为 cacheName 构建默认的热点参数规则
                if (useDefaultRule) {
                    ParamFlowRule rule = new ParamFlowRule();
                    BeanUtil.copyProperties(defaultRule, rule);
                    rule.setResource(cacheName);
                    rules.add(rule);
                }
            }
        }

        // 设置热点参数规则
        if (rules.size() > 0) {
            ParamFlowRuleManager.loadRules(rules);
        }
    }

    @Override
    public boolean isHotKey(String cacheName, String key) {
        Entry entry = null;
        try {
            entry = SphU.entry(cacheName, EntryType.IN, 1, key);
            return false;// 返回 false 表示不是热key
        } catch (BlockException ex) {
            if (log.isDebugEnabled()) {
                log.debug("sentinel 识别到热key, resource={}, key={}, rule={}", cacheName, key, ex.getRule().toString());
            }
            return true;// 返回 true 表示热key
        } finally {
            if (entry != null) {
                entry.exit(1, key);
            }
        }
    }
}
