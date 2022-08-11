package com.jd.platform.hotkey.dashboard.service;

import com.github.pagehelper.PageInfo;
import com.jd.platform.hotkey.dashboard.common.domain.req.PageReq;
import com.jd.platform.hotkey.dashboard.common.domain.vo.ClearCfgVo;

/**
 * @ProjectName: hotkey
 * @ClassName: ClearService
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/8/3 9:51
 */
public interface ClearService {


    PageInfo<ClearCfgVo> pageClearCfg(PageReq page, String app);

    ClearCfgVo selectClearCfg(String app);

    int saveClearCfg(ClearCfgVo cfg);

}
