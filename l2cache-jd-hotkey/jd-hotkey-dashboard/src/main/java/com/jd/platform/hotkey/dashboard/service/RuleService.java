package com.jd.platform.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.HitCountVo;
import com.jd.platform.hotkey.dashboard.model.Rules;

import java.util.List;

/**
 * @ProjectName: hotkey
 * @ClassName: RuleService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:29
 */
public interface RuleService {


    Rules selectRules(String app);

    int updateRule(Rules rules);

    Integer add(Rules rules);

    int delRule(String key, String updater);

    PageInfo<Rules> pageKeyRule(PageReq page, String appName);

    int save(Rules rules);

    List<String> listRules(String app);

    PageInfo<HitCountVo> pageRuleHitCount(PageReq pageReq, SearchReq req, String ownApp);
}
