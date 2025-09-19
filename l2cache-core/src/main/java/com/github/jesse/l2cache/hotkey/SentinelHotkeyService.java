package com.github.jesse.l2cache.hotkey;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.HotkeyService;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.github.jesse.l2cache.sync.CacheMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * sentinel热key探测
 * <p>
 * sentinel 默认是采用单机模式，因此，需要将本机识别到的hotkey，推送到服务的其他节点，以缓存到本地缓存
 * <p>
 * 热点参数限流：https://sentinelguard.io/zh-cn/docs/parameter-flow-control.html
 *
 * @author chenck
 * @date 2023/12/6 15:37
 */
@Slf4j
public class SentinelHotkeyService implements HotkeyService {

    private String instanceId;

    private CacheSyncPolicy cacheSyncPolicy;

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void setCacheSyncPolicy(CacheSyncPolicy cacheSyncPolicy) {
        this.cacheSyncPolicy = cacheSyncPolicy;
    }

    @Override
    public void init(L2CacheConfig.Hotkey hotKeyConfig, List<String> cacheNameList) {
        ParamFlowRule defaultRule = hotKeyConfig.getSentinel().getDefaultRule();
        List<ParamFlowRule> rules = hotKeyConfig.getSentinel().getRules();

        // 若配置了默认规则，则针对所有的cacheName，生成其默认的热点参数规则
        // 若未配置默认规则，则仅针对 rules 中的配置进行热点参数探测
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
            // 加载规则，静态限流
            ParamFlowRuleManager.loadRules(rules);
        }
    }

    @Override
    public boolean isHotkey(String level1CacheType, String cacheName, String key) {
        Entry entry = null;
        try {
            entry = SphU.entry(cacheName, EntryType.IN, 1, key);

            // 判断AutoDetectHotKeyCache中hotkey是否存在
            // 说明：放到 SphU.entry(...) 后面执行的目的，是为了保留sentinel探测hotkey的统计数据
            Boolean value = AutoDetectHotKeyCache.get(cacheName, key);

            // value=null 表示key不存在，也就是说，不是热key
            if (null == value) {
                return false;
            }

            if (value) {
                // sentinel 在第一次将key识别为hotkey，并缓存到AutoDetectHotKeyCache中，假设随后的1分钟内，sentinel识别到该key不是hotkey
                // 而AutoDetectHotKeyCache中缓存的hotkey，过期时间为10分钟，且还未过期，所以会走到该处
                // 另外，当 sentinel 再次将该key识别为hotkey时，从SphU.entry(...)这里会直接进入下面的异常处理逻辑中，也就是直接进入识别为hotkey的处理逻辑中
                if (log.isDebugEnabled()) {
                    log.debug("sentinel local hotkey, resource={}, key={}", cacheName, key);
                }

                // 简化处理：只要被识别为hotkey，则认为未来一段时间内(如：10分钟)，大概率还是热key，因此无需执行下面的逻辑，避免因复杂的分布式环境而导致出现各种奇怪问题
                /*
                // 将hotkey从本地缓存中移除
                Level1Cache level1Cache = CacheSupport.getLevel1Cache(level1CacheType, cacheName);
                if (null != level1Cache) {
                    level1Cache.clearLocalCache(key);
                }

                // 通知其他节点，该key由hotkey变为非hotkey
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage()
                            .setInstanceId(this.instanceId)
                            .setCacheType(level1CacheType)
                            .setCacheName(cacheName)
                            .setKey(key)
                            .setOptType(CacheConsts.CACHE_HOTKEY_EVIT)
                            .setDesc("Autodetect Hotkey Evit"));
                }
                */
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("sentinel remote hotkey, resource={}, key={}", cacheName, key);
                }
            }
            return true;// 返回 true 表示热key
        } catch (BlockException ex) {
            if (log.isDebugEnabled()) {
                log.debug("sentinel auto detect hotkey, resource={}, key={}, rule={}", cacheName, key, ex.getRule().toString());
            }

            Boolean value = AutoDetectHotKeyCache.get(cacheName, key);

            // value=null 表示key不存在，也就是说，不是热key
            // value=false 表示remote hotkey，直接更新为local hotkey
            if (null == value || !value) {
                // 将hotkey缓存到本地缓存中
                AutoDetectHotKeyCache.put(cacheName, key, Boolean.TRUE);

                // 通知其他节点，探测到hotkey
                if (null != cacheSyncPolicy) {
                    cacheSyncPolicy.publish(new CacheMessage()
                            .setInstanceId(this.instanceId)
                            .setCacheType(level1CacheType)
                            .setCacheName(cacheName)
                            .setKey(key)
                            .setOptType(CacheConsts.CACHE_HOTKEY)
                            .setDesc(null == value ? "Autodetect Hotkey" : "Autodetect Hotkey Remote upgrade"));
                }
            }
            return true;// 返回 true 表示热key
        } finally {
            if (entry != null) {
                entry.exit(1, key);
            }
        }
    }
}
