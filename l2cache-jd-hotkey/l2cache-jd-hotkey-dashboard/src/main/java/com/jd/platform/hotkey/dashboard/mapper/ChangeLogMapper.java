package com.jd.platform.hotkey.dashboard.mapper;

import com.jd.platform.hotkey.dashboard.common.domain.req.SearchReq;
import com.jd.platform.hotkey.dashboard.model.ChangeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChangeLogMapper {

    int insertSelective(ChangeLog record);

    ChangeLog selectByPrimaryKey(Integer id);

    List<ChangeLog> listChangeLog(SearchReq param);
}