package com.coy.l2cache.jd.hotkey.dashboard.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.PageReq;
import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.mapper.ChangeLogMapper;
import com.coy.l2cache.jd.hotkey.dashboard.model.ChangeLog;
import com.coy.l2cache.jd.hotkey.dashboard.service.ChangeLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ProjectName: hotkey
 * @ClassName: ChangeLogServiceImpl
 * @Description: TODO(一句话描述该类的功能)
 * @Author: liyunfeng31
 * @Date: 2020/4/17 17:53
 */
@Service
public class ChangeLogServiceImpl implements ChangeLogService {

    @Resource
    private ChangeLogMapper changeLogMapper;


    @Override
    public PageInfo<ChangeLog> pageChangeLog(PageReq page, SearchReq param) {
        PageHelper.startPage(page.getPageNum(),page.getPageSize());
        List<ChangeLog> changeLogs = changeLogMapper.listChangeLog(param);
        return new PageInfo<>(changeLogs);
    }

    @Override
    public ChangeLog selectByPrimaryKey(int id) {
        return changeLogMapper.selectByPrimaryKey(id);
    }

}
