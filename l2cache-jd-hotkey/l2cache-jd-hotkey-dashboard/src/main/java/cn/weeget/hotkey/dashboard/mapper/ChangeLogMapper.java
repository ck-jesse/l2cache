package cn.weeget.hotkey.dashboard.mapper;

import cn.weeget.hotkey.dashboard.common.domain.req.SearchReq;
import cn.weeget.hotkey.dashboard.model.ChangeLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChangeLogMapper {

    int insertSelective(ChangeLog record);

    ChangeLog selectByPrimaryKey(Integer id);

    List<ChangeLog> listChangeLog(SearchReq param);
}