package com.jd.platform.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.domain.Page;
import com.jd.platform.hotkey.dashboard.common.domain.req.ChartReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.HotKeyLineChartVo;
import com.jd.platform.hotkey.dashboard.model.KeyRecord;
import com.jd.platform.hotkey.dashboard.model.KeyTimely;
import com.jd.platform.hotkey.dashboard.model.Statistics;

import java.util.List;

/**
 * @ProjectName: hotkey
 * @ClassName: KeyService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:28
 */
public interface KeyService {


    PageInfo<KeyRecord> pageKeyRecord(PageReq page, SearchReq param);

    int insertKeyByUser(KeyTimely keyTimely);

    int updateKeyByUser(KeyTimely keyTimely);

    int delKeyByUser(KeyTimely keyTimely);

    Page<KeyTimely> pageKeyTimely(PageReq page, SearchReq param);

    PageInfo<Statistics> pageMaxHot(PageReq page, SearchReq param);

    List<Statistics> listMaxHot(SearchReq searchReq);

    HotKeyLineChartVo getLineChart(ChartReq chartReq);

    HotKeyLineChartVo ruleLineChart(SearchReq req, String app);
}
