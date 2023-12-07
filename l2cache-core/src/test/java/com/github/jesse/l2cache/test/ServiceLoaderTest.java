package com.github.jesse.l2cache.test;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.github.jesse.l2cache.CacheBuilder;
import com.github.jesse.l2cache.HotKey;
import com.github.jesse.l2cache.spi.ServiceLoader;

import java.util.Collections;

/**
 * @author chenck
 * @date 2020/7/2 18:17
 */
public class ServiceLoaderTest {

    public static void main(String[] args) {
        CacheBuilder cacheBuilder = ServiceLoader.load(CacheBuilder.class, "REDIS");
        System.out.println(cacheBuilder.getClass().getName());

        cacheBuilder = ServiceLoader.load(CacheBuilder.class, "caffeine");
        System.out.println(cacheBuilder.getClass().getName());

        // 设置热点参数规则
        String resourceName = "goodsCache";
        ParamFlowRule rule = new ParamFlowRule(resourceName)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setParamIdx(0)
                .setCount(2);
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));

        StringBuilder key = new StringBuilder(resourceName).append(":").append(123);

        HotKey hotKey = ServiceLoader.load(HotKey.class, "sentinel");

        for (int i = 0; i < 1000; i++) {
            boolean isHotKey = hotKey.isHotKey("goodsCache1", key.toString());
            System.out.println("i=" + i + " " + isHotKey);
        }

    }
}
