package com.jd.platform.hotkey.dashboard.mapper;

import com.jd.platform.hotkey.dashboard.model.Rules;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface RulesMapper {

    Rules select(String app);

    int insert(Rules record);

    int update(Rules record);

    int delete(String app);

}