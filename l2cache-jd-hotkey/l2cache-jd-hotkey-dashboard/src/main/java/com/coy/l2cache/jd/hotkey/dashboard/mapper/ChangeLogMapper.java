package com.coy.l2cache.jd.hotkey.dashboard.mapper;

import com.coy.l2cache.jd.hotkey.dashboard.common.domain.req.SearchReq;
import com.coy.l2cache.jd.hotkey.dashboard.model.ChangeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChangeLogMapper {

    int insertSelective(ChangeLog record);

    ChangeLog selectByPrimaryKey(Integer id);

    List<ChangeLog> listChangeLog(SearchReq param);
}