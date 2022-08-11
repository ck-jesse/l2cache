package com.coy.l2cache.jd.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.PageReq;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.model.ChangeLog;

/**
 * @ProjectName: hotkey
 * @ClassName: ChangeLogService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 16:29
 */
public interface ChangeLogService {

    PageInfo<ChangeLog> pageChangeLog(PageReq page, SearchReq param);

    ChangeLog selectByPrimaryKey(int id);

}
