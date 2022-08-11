package cn.weeget.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import cn.weeget.hotkey.dashboard.common.domain.req.PageReq;
import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.model.ChangeLog;

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
