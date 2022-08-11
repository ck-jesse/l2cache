package com.jd.platform.hotkey.sample.controller;

import com.ibm.etcd.api.KeyValue;
import com.jd.platform.hotkey.client.etcd.EtcdConfigFactory;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.rule.DefaultKeyRule;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-15
 */
@RestController
public class RuleController {

    @RequestMapping("rulePath")
    public Object rulePath() {
        List<KeyValue> list = EtcdConfigFactory.configCenter().getPrefix(ConfigConstant.rulePath);
        Map<String, Object> map = new HashMap<>();
        for (KeyValue keyValue : list) {
            map.put(keyValue.getKey().toStringUtf8(), keyValue.getValue().toStringUtf8());
        }
        return map;
    }

    @RequestMapping("addRulePath")
    public Object adRulePath(String appName) {
        KeyRule keyRule = new DefaultKeyRule().getKeyRule();

        EtcdConfigFactory.configCenter().put(ConfigConstant.rulePath + appName, FastJsonUtils.convertObjectToJSON(Arrays.asList(keyRule)));
        return "success";
    }
}
